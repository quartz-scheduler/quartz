/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.JdbcQuartzTestUtilities;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

@RunWith(Parameterized.class)
public class FlakyJdbcSchedulerTest extends AbstractSchedulerTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{0f, 0f, 0f}, {0.2f, 0f, 0f}, {0f, 0.2f, 0f}, {0f, 0f, 0.2f}, {0.2f, 0.2f, 0.2f}});
    }
    
    private final Random rndm;
    private final float createFailureProb;
    private final float preCommitFailureProb;
    private final float postCommitFailureProb;

    public FlakyJdbcSchedulerTest(float createFailureProb, float preCommitFailureProb, float postCommitFailureProb) {
        this.createFailureProb = createFailureProb;
        this.preCommitFailureProb = preCommitFailureProb;
        this.postCommitFailureProb = postCommitFailureProb;
        this.rndm = new Random();
    }

    @Override
    protected Scheduler createScheduler(String name, int threadPoolSize) throws SchedulerException {
        try {
            DBConnectionManager.getInstance().addConnectionProvider(name, new FlakyConnectionProvider(name));
        } catch (SQLException ex) {
            throw new AssertionError(ex);
        }
        JobStoreTX jobStore = new JobStoreTX();
        jobStore.setDataSource(name);
        jobStore.setTablePrefix("QRTZ_");
        jobStore.setInstanceId("AUTO");
        jobStore.setDbRetryInterval(50);
        DirectSchedulerFactory.getInstance().createScheduler(name + "Scheduler", "AUTO", new SimpleThreadPool(threadPoolSize, Thread.NORM_PRIORITY), jobStore, null, 0, -1, 50);
        return SchedulerRepository.getInstance().lookup(name + "Scheduler");
    }

    @Test
    public void testTriggerFiring() throws Exception {
        final int jobCount = 100;
        final int execCount = 5;

        Scheduler scheduler = createScheduler("testTriggerFiring", 2);
        try {
            for (int i = 0; i < jobCount; i++) {
                String jobName = "myJob" + i;
                JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity(jobName, "myJobGroup")
                        .usingJobData("data", 0).storeDurably().requestRecovery().build();

                Trigger trigger = TriggerBuilder
                        .newTrigger()
                        .withIdentity("triggerName" + i, "triggerGroup")
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1)
                        .withRepeatCount(execCount - 1)).build();

                if (!scheduler.checkExists(jobDetail.getKey())) {
                    scheduler.scheduleJob(jobDetail, trigger);
                }
            }

            scheduler.start();

            for (int i = 0; i < TimeUnit.MINUTES.toSeconds(5); i++) {
                int doneCount = 0;
                for (int j = 0; j < jobCount; j++) {
                    JobDetail jobDetail = scheduler.getJobDetail(new JobKey("myJob" + i, "myJobGroup"));
                    if (jobDetail.getJobDataMap().getInt("data") >= execCount) {
                        doneCount++;
                    }
                }
                if (doneCount == jobCount) {
                    return;
                }
                TimeUnit.SECONDS.sleep(1);
            }
            Assert.fail();
        } finally {
            scheduler.shutdown(true);
        }
    }

    @PersistJobDataAfterExecution
    @DisallowConcurrentExecution
    public static class TestJob implements Job {

        public void execute(JobExecutionContext context) {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            int val = dataMap.getInt("data") + 1;
            dataMap.put("data", val);
        }
    }

    private void createFailure() throws SQLException {
        if (rndm.nextFloat() < createFailureProb) {
            throw new SQLException("FlakyConnection failed on you on creation.");
        }
    }

    private void preCommitFailure() throws SQLException {
        if (rndm.nextFloat() < preCommitFailureProb) {
            throw new SQLException("FlakyConnection failed on you pre-commit.");
        }
    }

    private void postCommitFailure() throws SQLException {
        if (rndm.nextFloat() < postCommitFailureProb) {
            throw new SQLException("FlakyConnection failed on you post-commit.");
        }
    }
    
    private class FlakyConnectionProvider implements ConnectionProvider {

        private final Thread safeThread;
        private final String delegateName;

        private FlakyConnectionProvider(String name) throws SQLException {
            this.delegateName = "delegate_" + name;
            this.safeThread = Thread.currentThread();
            JdbcQuartzTestUtilities.createDatabase(delegateName);
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (Thread.currentThread() == safeThread) {
                return DBConnectionManager.getInstance().getConnection(delegateName);
            } else {
                createFailure();
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] {Connection.class},
                        new FlakyConnectionInvocationHandler(DBConnectionManager.getInstance().getConnection(delegateName)));
            }
        }

        @Override
        public void shutdown() throws SQLException {
            DBConnectionManager.getInstance().shutdown(delegateName);
            JdbcQuartzTestUtilities.destroyDatabase(delegateName);
            JdbcQuartzTestUtilities.shutdownDatabase();
        }

        @Override
        public void initialize() throws SQLException {
            //no-op
        }
    }

    private class FlakyConnectionInvocationHandler implements InvocationHandler {

        private final Connection delegate;

        public FlakyConnectionInvocationHandler(Connection delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("commit".equals(method.getName())) {
                preCommitFailure();
                method.invoke(delegate, args);
                postCommitFailure();
                return null;
            } else {
                return method.invoke(delegate, args);
            }
        }
    }
}
