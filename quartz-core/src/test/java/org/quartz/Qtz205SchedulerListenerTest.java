/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.quartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A unit test to reproduce QTZ-205 bug:
 * A TriggerListener vetoed job will affect SchedulerListener's triggerFinalized() notification. 
 * 
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
public class Qtz205SchedulerListenerTest {
	private static Logger logger = LoggerFactory.getLogger(Qtz205SchedulerListenerTest.class);
	
	public static class Qtz205Job implements Job {
		private static volatile int jobExecutionCount = 0;	
		public void execute(JobExecutionContext context) throws JobExecutionException {
			jobExecutionCount++;
			logger.info("Job executed. jobExecutionCount=" + jobExecutionCount);
		}
		
	}
	
	public static class Qtz205TriggerListener implements TriggerListener {
		private volatile int fireCount;
		public int getFireCount() {
			return fireCount;
		}
		public String getName() {
			return "Qtz205TriggerListener";
		}

		public void triggerFired(Trigger trigger, JobExecutionContext context) {
			fireCount++;
			logger.info("Trigger fired. count " + fireCount);
		}

		public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
			if (fireCount >= 3) {
				logger.info("Job execution vetoed.");
				return true;
			} else {
				return false;
			}
		}

		public void triggerMisfired(Trigger trigger) {
		}

		public void triggerComplete(Trigger trigger,
				JobExecutionContext context,
				CompletedExecutionInstruction triggerInstructionCode) {
		}
		
	}
	
	public static class Qtz205ScheListener implements SchedulerListener {
		private int triggerFinalizedCount;
		public int getTriggerFinalizedCount() {
			return triggerFinalizedCount;
		}
		public void jobScheduled(Trigger trigger) {
		}

		public void jobUnscheduled(TriggerKey triggerKey) {
		}

		public void triggerFinalized(Trigger trigger) {
			triggerFinalizedCount ++;
			logger.info("triggerFinalized " + trigger);
		}

		public void triggerPaused(TriggerKey triggerKey) {
		}

		public void triggersPaused(String triggerGroup) {	
		}

		public void triggerResumed(TriggerKey triggerKey) {
		}

		public void triggersResumed(String triggerGroup) {
		}

		public void jobAdded(JobDetail jobDetail) {
		}

		public void jobDeleted(JobKey jobKey) {
		}

		public void jobPaused(JobKey jobKey) {
		}

		public void jobsPaused(String jobGroup) {
		}

		public void jobResumed(JobKey jobKey) {
		}

		public void jobsResumed(String jobGroup) {
			
		}

		public void schedulerError(String msg, SchedulerException cause) {			
		}

		public void schedulerInStandbyMode() {
		}

		public void schedulerStarted() {
		}
		
		public void schedulerStarting() {
		}

		public void schedulerShutdown() {
		}

		public void schedulerShuttingdown() {
		}

		public void schedulingDataCleared() {
		}
	}
	
	/** QTZ-205 */

	@Test
	public void testTriggerFinalized() throws Exception {
		Qtz205TriggerListener triggerListener = new Qtz205TriggerListener();
		Qtz205ScheListener schedulerListener = new Qtz205ScheListener();
		Properties props = new Properties();
		props.setProperty("org.quartz.scheduler.idleWaitTime", "1500");
		props.setProperty("org.quartz.threadPool.threadCount", "2");
		Scheduler scheduler = new StdSchedulerFactory(props).getScheduler();
		scheduler.getListenerManager().addSchedulerListener(schedulerListener);
		scheduler.getListenerManager().addTriggerListener(triggerListener);
		scheduler.start();
		scheduler.standby();
		
		JobDetail job = newJob(Qtz205Job.class).withIdentity("test").build();
		Trigger trigger = newTrigger().withIdentity("test")
				.withSchedule(simpleSchedule().withIntervalInMilliseconds(250).withRepeatCount(2))
				.build();
		scheduler.scheduleJob(job, trigger);
		scheduler.start();
		Thread.sleep(5000);
		
		scheduler.shutdown(true);

		Assert.assertEquals(2, Qtz205Job.jobExecutionCount);
		Assert.assertEquals(3, triggerListener.getFireCount());
		Assert.assertEquals(1, schedulerListener.getTriggerFinalizedCount());
	}
}
