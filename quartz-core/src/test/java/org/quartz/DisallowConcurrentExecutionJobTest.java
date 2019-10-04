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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.JobListenerSupport;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * Integration test for using DisallowConcurrentExecution annot.
 * 
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
public class DisallowConcurrentExecutionJobTest {
	
	private static final long JOB_BLOCK_TIME = 300L;
	
	private static final String BARRIER = "BARRIER";
	private static final String DATE_STAMPS = "DATE_STAMPS";
	
	@DisallowConcurrentExecution
	public static class TestJob implements Job {
		public void execute(JobExecutionContext context) throws JobExecutionException {
			try {
				@SuppressWarnings("unchecked")
				List<Date> jobExecDates = (List<Date>)context.getScheduler().getContext().get(DATE_STAMPS);
                                long firedAt = System.currentTimeMillis();
				jobExecDates.add(new Date(firedAt));
                                long sleepTill = firedAt + JOB_BLOCK_TIME;
                                for (long sleepFor = sleepTill - System.currentTimeMillis(); sleepFor > 0; sleepFor = sleepTill - System.currentTimeMillis()) {
                                  Thread.sleep(sleepFor);
                                }
			} catch (InterruptedException e) {
				throw new JobExecutionException("Failed to pause job for testing.");
			} catch (SchedulerException e) {
				throw new JobExecutionException("Failed to lookup datestamp collection.");
			}
		}
	}
	
	public static class TestJobListener extends JobListenerSupport {

		private final AtomicInteger jobExCount = new AtomicInteger(0);
		private final int jobExecutionCountToSyncAfter;
		
		public TestJobListener(int jobExecutionCountToSyncAfter) {
			this.jobExecutionCountToSyncAfter = jobExecutionCountToSyncAfter;
		}
		
		public String getName() {
			return "TestJobListener";
		}

		@Override
		public void jobWasExecuted(JobExecutionContext context,
				JobExecutionException jobException) {
			if(jobExCount.incrementAndGet() == jobExecutionCountToSyncAfter) {
				try {
					CyclicBarrier barrier =  (CyclicBarrier)context.getScheduler().getContext().get(BARRIER);
					barrier.await(125, TimeUnit.SECONDS);
				} catch (Throwable e) {
					e.printStackTrace();
					throw new AssertionError("Await on barrier was interrupted: " + e.toString());
				} 
			}
		}
	}
	
        @Test
	public void testNoConcurrentExecOnSameJob() throws Exception {

		List<Date> jobExecDates = Collections.synchronizedList(new ArrayList<Date>());
		CyclicBarrier barrier = new CyclicBarrier(2);
		
		Date startTime = new Date(System.currentTimeMillis() + 100); // make the triggers fire at the same time.
		
		JobDetail job1 = JobBuilder.newJob(TestJob.class).withIdentity("job1").build();
		Trigger trigger1 = TriggerBuilder.newTrigger().withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.startAt(startTime).build();

		Trigger trigger2 = TriggerBuilder.newTrigger().withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.startAt(startTime).forJob(job1.getKey()).build();

		Properties props = new Properties();
		props.setProperty("org.quartz.scheduler.idleWaitTime", "1500");
		props.setProperty("org.quartz.threadPool.threadCount", "2");
		Scheduler scheduler = new StdSchedulerFactory(props).getScheduler();
		scheduler.getContext().put(BARRIER, barrier);
		scheduler.getContext().put(DATE_STAMPS, jobExecDates);
		scheduler.getListenerManager().addJobListener(new TestJobListener(2));
		scheduler.scheduleJob(job1, trigger1);
		scheduler.scheduleJob(trigger2);
		scheduler.start();
		
		barrier.await(125, TimeUnit.SECONDS);
		
		scheduler.shutdown(true);
		
                Assert.assertThat(jobExecDates, hasSize(2));
                long fireTimeTrigger1 = jobExecDates.get(0).getTime();
                long fireTimeTrigger2 = jobExecDates.get(1).getTime();
                Assert.assertThat(fireTimeTrigger2 - fireTimeTrigger1, greaterThanOrEqualTo(JOB_BLOCK_TIME));
	}
	
	/** QTZ-202 */
        @Test
	public void testNoConcurrentExecOnSameJobWithBatching() throws Exception {

		List<Date> jobExecDates = Collections.synchronizedList(new ArrayList<Date>());
		CyclicBarrier barrier = new CyclicBarrier(2);
		
		Date startTime = new Date(System.currentTimeMillis() + 100); // make the triggers fire at the same time.
		
		JobDetail job1 = JobBuilder.newJob(TestJob.class).withIdentity("job1").build();
		Trigger trigger1 = TriggerBuilder.newTrigger().withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.startAt(startTime).build();

		Trigger trigger2 = TriggerBuilder.newTrigger().withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.startAt(startTime).forJob(job1.getKey()).build();

		Properties props = new Properties();
		props.setProperty("org.quartz.scheduler.idleWaitTime", "1500");
		props.setProperty("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", "2");
		props.setProperty("org.quartz.threadPool.threadCount", "2");
		Scheduler scheduler = new StdSchedulerFactory(props).getScheduler();
		scheduler.getContext().put(BARRIER, barrier);
		scheduler.getContext().put(DATE_STAMPS, jobExecDates);
		scheduler.getListenerManager().addJobListener(new TestJobListener(2));
		scheduler.scheduleJob(job1, trigger1);
		scheduler.scheduleJob(trigger2);
		scheduler.start();
		
		barrier.await(125, TimeUnit.SECONDS);
		
		scheduler.shutdown(true);
		
                Assert.assertThat(jobExecDates, hasSize(2));
                long fireTimeTrigger1 = jobExecDates.get(0).getTime();
                long fireTimeTrigger2 = jobExecDates.get(1).getTime();
                Assert.assertThat(fireTimeTrigger2 - fireTimeTrigger1, greaterThanOrEqualTo(JOB_BLOCK_TIME));
	}
}
