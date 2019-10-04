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
package org.quartz.integrations.tests;

import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class QTZ179_TriggerLostAfterDbRestart_Test extends QuartzDatabaseTestSupport {

  private static final long DURATION_OF_FIRST_SCHEDULING = 9L;
	private static final long DURATION_OF_NETWORK_FAILURE = 10L;
  private static final long DURATION_OF_SECOND_SCHEDULING = 10L;
	private static final  Logger LOG = LoggerFactory.getLogger(QTZ179_TriggerLostAfterDbRestart_Test.class);
	private static final int INTERVAL_IN_SECONDS = 3;
	private static Trigger trigger1_1;
	private static Trigger trigger2_1;
	private static Trigger trigger1_2;
	private static Trigger trigger2_2;

	@Override
	protected Properties createSchedulerProperties() {
		Properties properties = new Properties();
		properties.put("org.quartz.scheduler.instanceName","TestScheduler");
		properties.put("org.quartz.scheduler.instanceId","AUTO");
		properties.put("org.quartz.scheduler.skipUpdateCheck","true");
		properties.put("org.quartz.threadPool.class","org.quartz.simpl.SimpleThreadPool");
		properties.put("org.quartz.threadPool.threadCount","12");
		properties.put("org.quartz.threadPool.threadPriority","5");
		properties.put("org.quartz.jobStore.misfireThreshold","10000");
		properties.put("org.quartz.jobStore.class","org.quartz.impl.jdbcjobstore.JobStoreTX");
		properties.put("org.quartz.jobStore.driverDelegateClass","org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
		properties.put("org.quartz.jobStore.useProperties","true");
		properties.put("org.quartz.jobStore.dataSource","myDS");
		properties.put("org.quartz.jobStore.tablePrefix","QRTZ_");
		properties.put("org.quartz.jobStore.isClustered","false");
		properties.put("org.quartz.dataSource.myDS.driver","org.apache.derby.jdbc.ClientDriver");
		properties.put("org.quartz.dataSource.myDS.URL",JdbcQuartzDerbyUtilities.DATABASE_CONNECTION_PREFIX);
		properties.put("org.quartz.dataSource.myDS.user","quartz");
		properties.put("org.quartz.dataSource.myDS.password","quartz");
		properties.put("org.quartz.dataSource.myDS.maxConnections","5");
		return properties;
	}

	@Override
	protected void afterSchedulerInit() throws Exception {
		LOG.info("------- Scheduling Job  -------------------");

		// define the jobs and tie them to our HelloJob class
		JobDetail job1_1 = newJob(HelloJob.class).withIdentity("job1", "group1").build();
		JobDetail job2_1 = newJob(HelloJob.class).withIdentity("job2", "group1").build();
		JobDetail job1_2 = newJob(HelloJob.class).withIdentity("job1", "group2").build();
		JobDetail job2_2 = newJob(HelloJob.class).withIdentity("job2", "group2").build();

		trigger1_1 = newTrigger()
				.withIdentity("job1", "group1")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();
		trigger2_1 = newTrigger()
				.withIdentity("job2", "group1")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();
		trigger1_2 = newTrigger()
				.withIdentity("job1", "group2")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();
		trigger2_2 = newTrigger()
				.withIdentity("job2", "group2")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();

		Trigger[] triggers = new Trigger[] { trigger1_1, trigger1_2, trigger2_1, trigger2_2 };
		JobDetail[] jobDetails = new JobDetail[] { job1_1, job1_2, job2_1, job2_2 };
		for (int i = 0; i < triggers.length; i++) {
			JobDetail job = jobDetails[i];
			Trigger trigger = triggers[i];
			if (scheduler.checkExists(job.getKey())) {
				// the job already exists in jdbcjobstore; let's reschedule it
				scheduler.rescheduleJob(trigger.getKey(), trigger);
			} else {
				scheduler.scheduleJob(job, trigger);
			}
		}

		// Start up the scheduler (nothing can actually run until the
		// scheduler has been started)
		scheduler.start();
	}

	@Test
	public void checkAll4TriggersStillRunningTest() throws Exception {

		LOG.info("------- Scheduler Started -----------------");

		// wait long enough so that the scheduler as an opportunity to
		// run the job!
		try {
			Thread.sleep(DURATION_OF_FIRST_SCHEDULING * 1000L);
		} catch (Exception e) {
		}

		//there should be maximum 1 trigger in acquired state
		if(JdbcQuartzDerbyUtilities.triggersInAcquiredState()>1){
			fail("There should not be more than 1 trigger in ACQUIRED state in the DB.");
		}

		// Shutting down and starting up again the database to simulate a
		// network error
		try {
			LOG.info("------- Shutting down database ! -----------------");
			derbyServer.shutdown();
			Thread.sleep(DURATION_OF_NETWORK_FAILURE * 1000L);
			derbyServer.start(null);
			LOG.info("------- Database back online ! -----------------");
			Thread.sleep(DURATION_OF_SECOND_SCHEDULING * 1000L);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int triggersInAcquiredState = JdbcQuartzDerbyUtilities.triggersInAcquiredState();
		assertFalse("There should not be more than 1 trigger in ACQUIRED state in the DB, but found "+triggersInAcquiredState,triggersInAcquiredState > 1);
	}

}
