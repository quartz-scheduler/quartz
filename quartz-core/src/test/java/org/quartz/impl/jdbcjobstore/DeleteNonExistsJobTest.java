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
package org.quartz.impl.jdbcjobstore;

import org.junit.*;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Tests for QTZ-326
 *
 * @author Zemian Deng
 */
public class DeleteNonExistsJobTest {
    private static Logger LOG = LoggerFactory.getLogger(DeleteNonExistsJobTest.class);
    private static String DB_NAME = "DeleteNonExistsJobTestDatasase";
    private static String SCHEDULER_NAME = "DeleteNonExistsJobTestScheduler";
    private static Scheduler scheduler;

    @BeforeClass
    public static void beforeClass() throws Exception {
        JdbcQuartzTestUtilities.createDatabase(DB_NAME);
    }

    @Before
    public void beforeTest() throws Exception {
        resetDatabaseData();
        JobStoreTX jobStore = new JobStoreTX();
        jobStore.setDataSource(DB_NAME);
        jobStore.setTablePrefix("QRTZ_");
        jobStore.setInstanceId("AUTO");
        DirectSchedulerFactory.getInstance().createScheduler(SCHEDULER_NAME, "AUTO", new SimpleThreadPool(4, Thread.NORM_PRIORITY), jobStore);
        scheduler = SchedulerRepository.getInstance().lookup(SCHEDULER_NAME);
        //scheduler.start(); // Do not start scheduler to produce the defect case.
    }

    private void resetDatabaseData() throws Exception {
        Connection conn = DBConnectionManager.getInstance().getConnection(DB_NAME);
        Statement statement = conn.createStatement();
        statement.addBatch("delete from qrtz_fired_triggers");
        statement.addBatch("delete from qrtz_paused_trigger_grps");
        statement.addBatch("delete from qrtz_scheduler_state");
        statement.addBatch("delete from qrtz_locks");
        statement.addBatch("delete from qrtz_simple_triggers");
        statement.addBatch("delete from qrtz_simprop_triggers");
        statement.addBatch("delete from qrtz_blob_triggers");
        statement.addBatch("delete from qrtz_cron_triggers");
        statement.addBatch("delete from qrtz_triggers");
        statement.addBatch("delete from qrtz_job_details");
        statement.addBatch("delete from qrtz_calendars");
        statement.executeBatch();
        statement.close();
        conn.close();
    }

    @After
    public void afterTest() throws Exception {
        scheduler.shutdown(true);
    }

    @Test
    public void deleteJobDetailOnly() throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity("testjob").storeDurably().build();
        scheduler.addJob(jobDetail, true);
        modifyStoredJobClassName();

        scheduler.deleteJob(jobDetail.getKey());
    }

    @Test
    public void deleteJobDetailWithTrigger() throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity("testjob2").storeDurably().build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("testjob2")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
        modifyStoredJobClassName();

        scheduler.deleteJob(jobDetail.getKey());
    }

    @Test
    public void deleteTrigger() throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity("testjob3").storeDurably().build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("testjob3")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
        modifyStoredJobClassName();

        scheduler.unscheduleJob(trigger.getKey());
    }

    @Test
    public void replaceJobDetail() throws Exception {
        JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity("testjob3").storeDurably().build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("testjob3")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
        modifyStoredJobClassName();

        jobDetail = JobBuilder.newJob(TestJob.class).withIdentity("testjob3").storeDurably().build();
        scheduler.addJob(jobDetail, true);
    }

    private void modifyStoredJobClassName() throws Exception {
        Connection conn = DBConnectionManager.getInstance().getConnection(DB_NAME);
        Statement statement = conn.createStatement();
        statement.executeUpdate("update qrtz_job_details set job_class_name='com.FakeNonExistsJob'");
        statement.close();
        conn.close();
    }

    public static class TestJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            LOG.info("Job is executing {}", context);
        }
    }
}
