/*
 * Copyright 2001-2013 Terracotta, Inc.
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
 *
 */
package org.quartz.integrations.tests;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.quartz.*;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.quartz.integrations.tests.TrackingJob.SCHEDULED_TIMES_KEY;

/**
 * A integration test for Quartz In-Memory Scheduler with Cron Trigger.
 * @author Zemian Deng
 */
public class QuartzMemoryCronTriggerTest extends QuartzMemoryTestSupport {
    @Test
    public void testCronRepeatCount() throws Exception {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("test")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        List<Long> scheduledTimes = Collections.synchronizedList(new LinkedList<Long>());
        scheduler.getContext().put(SCHEDULED_TIMES_KEY, scheduledTimes);
        JobDetail jobDetail = JobBuilder.newJob(TrackingJob.class).withIdentity("test").build();
        scheduler.scheduleJob(jobDetail, trigger);

        for (int i = 0; i < 20 && scheduledTimes.size() < 3; i++) {
          Thread.sleep(500);
        }
        assertThat(scheduledTimes, hasSize(greaterThanOrEqualTo(3)));

        Long[] times = scheduledTimes.toArray(new Long[scheduledTimes.size()]);
        
        long baseline = times[0];
        assertThat(baseline % 1000, is(0L));
        for (int i = 1; i < times.length; i++) {
          assertThat(times[i], is(baseline + TimeUnit.SECONDS.toMillis(i)));
        }
    }
    
    @Test
    public void testCronRepeatCountWithJobListener() throws Exception {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("test")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        List<Long> scheduledTimes = Collections.synchronizedList(new LinkedList<Long>());
        scheduler.getContext().put(SCHEDULED_TIMES_KEY, scheduledTimes);
        JobDetail jobDetail = JobBuilder.newJob(NonConcurrentTrackingJob.class).withIdentity("test").build();
        scheduler.scheduleJob(jobDetail, trigger);
        final AtomicInteger count = new AtomicInteger(0);
        scheduler.getListenerManager().addJobListener(new JobListener() {
            
            @Override
            public void jobWasExecuted(JobExecutionContext context,
                    JobExecutionException jobException) {
                count.set(count.get()+1);
                
            }
            
            @Override
            public void jobToBeExecuted(JobExecutionContext context) {
                count.set(count.get()+1);
                
            }
            
            @Override
            public void jobExecutionVetoed(JobExecutionContext context) {
                count.set(count.get()+1);
                
            }
            
            @Override
            public String getName() {
                return "testjoblistener";
            }
        });

        for (int i = 0; i < 20 && scheduledTimes.size() < 3; i++) {
          Thread.sleep(500);
        }
        assertThat(count.get(), is(3*2));
        
        assertThat(scheduledTimes, hasSize(greaterThanOrEqualTo(3)));

        Long[] times = scheduledTimes.toArray(new Long[scheduledTimes.size()]);
        
        long baseline = times[0];
        assertThat(baseline % 1000, is(0L));
        for (int i = 1; i < times.length; i++) {
          assertThat(times[i], is(baseline + TimeUnit.SECONDS.toMillis(i)));
        }

    }
    
    @Test
    public void testShouldNotStayBlockedOnJobListenerExceptionBeforeExecution() throws Exception {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("test")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        final List<Long> scheduledTimes = Collections.synchronizedList(new LinkedList<Long>());
        scheduler.getContext().put(SCHEDULED_TIMES_KEY, scheduledTimes);
        JobDetail jobDetail = JobBuilder.newJob(NonConcurrentTrackingJob.class).withIdentity("test").build();
        scheduler.scheduleJob(jobDetail, trigger);
        final AtomicInteger count = new AtomicInteger(0);
        scheduler.getListenerManager().addJobListener(new JobListener() {
            
            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
                count.set(count.get()+1);
            }
            
            @Override
            public void jobToBeExecuted(JobExecutionContext context) {
                count.set(count.get()+1);
                
                if (count.get() == 3){
                    throw new RuntimeException("failing job execution #2");
                }
            }
            
            @Override
            public void jobExecutionVetoed(JobExecutionContext context) {
                count.set(count.get()+1);
            }
            
            @Override
            public String getName() {
                return "testjoblistener";
            }
        });

        for (int i = 0; i < 20 && scheduledTimes.size() < 3; i++) {
          Thread.sleep(500);
        }
        assertThat(count.get(), is(3*2+1));
        
        assertThat(scheduledTimes, hasSize(greaterThanOrEqualTo(3)));

        Long[] times = scheduledTimes.toArray(new Long[scheduledTimes.size()]);
        
        long baseline = times[0];
        assertThat(baseline % 1000, is(0L));
        
        // first successful execution: #1 try
        assertThat(times[0], is(baseline + TimeUnit.SECONDS.toMillis(0)));
        // second successful execution: #2 try was a failure, then #3 try success
        assertThat(times[1], is(baseline + TimeUnit.SECONDS.toMillis(2)));
        // third successful execution: #4 try
        assertThat(times[2], is(baseline + TimeUnit.SECONDS.toMillis(3)));
    }
    
    @Test
    public void testShouldNotStayBlockedOnJobListenerExceptionAfterExecution() throws Exception {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("test")
                .withSchedule(CronScheduleBuilder.cronSchedule("* * * * * ?"))
                .build();
        final List<Long> scheduledTimes = Collections.synchronizedList(new LinkedList<Long>());
        scheduler.getContext().put(SCHEDULED_TIMES_KEY, scheduledTimes);
        JobDetail jobDetail = JobBuilder.newJob(NonConcurrentTrackingJob.class).withIdentity("test").build();
        scheduler.scheduleJob(jobDetail, trigger);
        final AtomicInteger count = new AtomicInteger(0);
        scheduler.getListenerManager().addJobListener(new JobListener() {
            
            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
                count.set(count.get()+1);
                if (count.get() == 4){
                    throw new RuntimeException("failing job execution #2");
                }
            }
            
            @Override
            public void jobToBeExecuted(JobExecutionContext context) {
                count.set(count.get()+1);
            }
            
            @Override
            public void jobExecutionVetoed(JobExecutionContext context) {
                count.set(count.get()+1);
            }
            
            @Override
            public String getName() {
                return "testjoblistener";
            }
        });

        for (int i = 0; i < 20 && scheduledTimes.size() < 3; i++) {
          Thread.sleep(500);
        }
        assertThat(count.get(), is(3*2));
        
        assertThat(scheduledTimes, hasSize(greaterThanOrEqualTo(3)));

        Long[] times = scheduledTimes.toArray(new Long[scheduledTimes.size()]);
        
        long baseline = times[0];
        assertThat(baseline % 1000, is(0L));
        
        // first successful execution: #1 try
        assertThat(times[0], is(baseline + TimeUnit.SECONDS.toMillis(0)));
        // second successful execution: #2 try was a success (only failed to notify of completion) 
        assertThat(times[1], is(baseline + TimeUnit.SECONDS.toMillis(1)));
        // third successful execution: #3 try
        assertThat(times[2], is(baseline + TimeUnit.SECONDS.toMillis(2)));
    }
}
