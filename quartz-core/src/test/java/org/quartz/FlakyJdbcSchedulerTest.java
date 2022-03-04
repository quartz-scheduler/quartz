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

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.JdbcQuartzTestUtilities;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

class FlakyJdbcSchedulerTest extends AbstractSchedulerTest {

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(0f, 0f, 0f), 
                Arguments.of(0.2f, 0f, 0f), 
                Arguments.of(0f, 0.2f, 0f), 
                Arguments.of(0f, 0f, 0.2f), 
                Arguments.of(0.2f, 0.2f, 0.2f));
    }
    
    private final Random rndm;

    public FlakyJdbcSchedulerTest() {
        this.rndm = new Random();
    }

    @Override
    protected Scheduler createScheduler(String name, int threadPoolSize, float createFailureProb,
            float preCommitFailureProb, float postCommitFailureProb) throws SchedulerException {
        try {
            DBConnectionManager.getInstance().addConnectionProvider(name,
                    new FlakyConnectionProvider(name, createFailureProb, preCommitFailureProb, postCommitFailureProb));
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

    @ParameterizedTest
    @MethodSource("data")
    void testTriggerFiring(float createFailureProb, float preCommitFailureProb, float postCommitFailureProb) throws Exception {
        final int jobCount = 100;
        final int execCount = 5;

        Scheduler scheduler = createScheduler("testTriggerFiring", 2, createFailureProb, preCommitFailureProb,
                postCommitFailureProb);
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
            fail();
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

    private void createFailure(float createFailureProb) throws SQLException {
        if (rndm.nextFloat() < createFailureProb) {
            throw new SQLException("FlakyConnection failed on you on creation.");
        }
    }

    private void preCommitFailure(float preCommitFailureProb) throws SQLException {
        if (rndm.nextFloat() < preCommitFailureProb) {
            throw new SQLException("FlakyConnection failed on you pre-commit.");
        }
    }

    private void postCommitFailure(float postCommitFailureProb) throws SQLException {
        if (rndm.nextFloat() < postCommitFailureProb) {
            throw new SQLException("FlakyConnection failed on you post-commit.");
        }
    }
    
    private class FlakyConnectionProvider implements ConnectionProvider {

        private final Thread safeThread;
        private final String delegateName;
        private final float createFailureProb;
        private final float preCommitFailureProb;
        private final float postCommitFailureProb;
        
        private FlakyConnectionProvider(String name, float createFailureProb, float preCommitFailureProb, float postCommitFailureProb) throws SQLException {
            this.delegateName = "delegate_" + name;
            this.safeThread = Thread.currentThread();
            this.createFailureProb = createFailureProb;
            this.preCommitFailureProb = preCommitFailureProb;
            this.postCommitFailureProb = postCommitFailureProb;
            JdbcQuartzTestUtilities.createDatabase(delegateName);
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (Thread.currentThread() == safeThread) {
                return DBConnectionManager.getInstance().getConnection(delegateName);
            } else {
                createFailure(createFailureProb);
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] {Connection.class},
                        new FlakyConnectionInvocationHandler(DBConnectionManager.getInstance().getConnection(delegateName), preCommitFailureProb, postCommitFailureProb));
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
        private final float preCommitFailureProb;
        private final float postCommitFailureProb;

        public FlakyConnectionInvocationHandler(Connection delegate, float preCommitFailureProb, float postCommitFailureProb) {
            this.delegate = delegate;
            this.preCommitFailureProb = preCommitFailureProb;
            this.postCommitFailureProb = postCommitFailureProb;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("commit".equals(method.getName())) {
                preCommitFailure(preCommitFailureProb);
                method.invoke(delegate, args);
                postCommitFailure(postCommitFailureProb);
                return null;
            } else {
                return method.invoke(delegate, args);
            }
        }
    }
}
