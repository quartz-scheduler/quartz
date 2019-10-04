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
package org.quartz.core;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JdbcQuartzTestUtilities;
import org.quartz.impl.jdbcjobstore.JobStoreTX;

import org.quartz.listeners.JobListenerSupport;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author https://github.com/eugene-goroschenya
 */
public class RecoverJobsTest {

    @Test
    public void testRecoveringRepeatJobWhichIsFiredAndMisfiredAtTheSameTime() throws SchedulerException, SQLException, InterruptedException {
        String dsName = "recoverJobsTest";
        JdbcQuartzTestUtilities.createDatabase(dsName);
        try {
            final JobStoreTX jobStore = new JobStoreTX();
            jobStore.setDataSource(dsName);
            jobStore.setInstanceId("SINGLE_NODE_TEST");
            jobStore.setInstanceName(dsName);
            jobStore.setMisfireThreshold(1000);

            DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();

            factory.createScheduler(new SimpleThreadPool(1, Thread.NORM_PRIORITY), jobStore);
            Scheduler scheduler = factory.getScheduler();

            // run forever up to the first fail over situation
            RecoverJobsTestJob.runForever = true;

            scheduler.scheduleJob(
                    JobBuilder.newJob(RecoverJobsTestJob.class)
                            .withIdentity("test")
                            .build(),
                    TriggerBuilder.newTrigger()
                            .withIdentity("test")
                            .withSchedule(
                                    SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInMilliseconds(1000)
                                            .repeatForever()
                            ).build()
            );

            scheduler.start();

            // wait to be sure job is executing
            Thread.sleep(2000);

            // emulate fail over situation
            scheduler.shutdown(false);

            Connection conn = DBConnectionManager.getInstance().getConnection(dsName);
            try {
                Statement st = conn.createStatement();
                ResultSet rs1 = st.executeQuery("SELECT TRIGGER_STATE from QRTZ_TRIGGERS");
                rs1.next();
                // check that trigger is blocked after fail over situation
                Assert.assertEquals("BLOCKED", rs1.getString(1));

                ResultSet rs2 = st.executeQuery("SELECT count(*) from QRTZ_FIRED_TRIGGERS");
                rs2.next();
                // check that fired trigger remains after fail over situation
                Assert.assertEquals(1, rs2.getLong(1));
                st.close();
            } finally {
                conn.close();
            }

            // stop job executing to not as part of emulation fail over situation
            RecoverJobsTestJob.runForever = false;

            // emulate down time >> trigger interval - misfireThreshold
            Thread.sleep(4000);

            final AtomicBoolean isJobRecovered = new AtomicBoolean(false);
            factory.createScheduler(new SimpleThreadPool(1, Thread.NORM_PRIORITY), jobStore);
            Scheduler recovery = factory.getScheduler();
            recovery.getListenerManager().addJobListener(new JobListenerSupport() {
                @Override
                public String getName() {
                    return RecoverJobsTest.class.getSimpleName();
                }

                @Override
                public void jobToBeExecuted(JobExecutionContext context) {
                    isJobRecovered.set(true);
                }
            });
            recovery.start();

            // wait to be sure recovered job was executed
            Thread.sleep(2000);

            // wait job
            recovery.shutdown(true);

            Assert.assertTrue(isJobRecovered.get());
        } finally {
            JdbcQuartzTestUtilities.destroyDatabase(dsName);
        }
    }

    /**
     * @author https://github.com/eugene-goroschenya
     */
    @DisallowConcurrentExecution
    public static class RecoverJobsTestJob implements Job {
        private static Logger _log = LoggerFactory.getLogger(RecoverJobsTestJob.class);
        static boolean runForever = true;

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            long now = System.currentTimeMillis();
            int tic = 0;
            _log.info("Started - " + now);
            try {
                while (runForever) {
                    Thread.sleep(1000);
                    _log.info("Tic " + (++tic) + "- " + now);
                }
                _log.info("Stopped - " + now);
            } catch (InterruptedException e) {
                _log.info("Interrupted - " + now);
            }
        }
    }
}
