/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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

import static org.junit.jupiter.api.Assertions.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;



/**
 * Test High Level Scheduler functionality (implicitly tests the underlying jobstore (RAMJobStore))
 */
public abstract class AbstractSchedulerTest {

    private static final String BARRIER = "BARRIER";
    private static final String DATE_STAMPS = "DATE_STAMPS";
    private static final String JOB_THREAD = "JOB_THREAD";

    @SuppressWarnings("deprecation")
    public static class TestStatefulJob implements StatefulJob {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }

    public static class TestJob implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }
    
	public static final long TEST_TIMEOUT_SECONDS = 50;
    
    public static class TestJobWithSync implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        	
			try {
				@SuppressWarnings("unchecked")
				List<Long> jobExecTimestamps = (List<Long>)context.getScheduler().getContext().get(DATE_STAMPS);
				CyclicBarrier barrier =  (CyclicBarrier)context.getScheduler().getContext().get(BARRIER);

	        	jobExecTimestamps.add(System.currentTimeMillis());
	        	
				barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (Throwable e) {
				e.printStackTrace();
				throw new AssertionError("Await on barrier was interrupted: " + e.toString());
			} 
        }
    }
    
    @DisallowConcurrentExecution
    @PersistJobDataAfterExecution
    public static class TestAnnotatedJob implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }
    
    protected abstract Scheduler createScheduler(String name, int threadPoolSize) throws SchedulerException;

    @Test
    void testBasicStorageFunctions() throws Exception {
        Scheduler sched = createScheduler("testBasicStorageFunctions", 2);

        // test basic storage functions of scheduler...
        
        JobDetail job = newJob()
            .ofType(TestJob.class)
            .withIdentity("j1")
            .storeDurably()
            .build();

        assertFalse(sched.checkExists(jobKey("j1")), "Unexpected existence of job named 'j1'.");

        sched.addJob(job, false); 

        assertTrue(sched.checkExists(jobKey("j1")), "Expected existence of job named 'j1' but checkExists return false.");

        job = sched.getJobDetail(jobKey("j1"));

        assertNotNull(job,"Stored job not found!");
        
        sched.deleteJob(jobKey("j1"));
        
        Trigger trigger = newTrigger()
            .withIdentity("t1")
            .forJob(job)
            .startNow()
            .withSchedule(simpleSchedule()
                    .repeatForever()
                    .withIntervalInSeconds(5))
             .build();

        assertFalse(sched.checkExists(triggerKey("t1")), "Unexpected existence of trigger named '11'.");

        sched.scheduleJob(job, trigger);
        
        assertTrue(sched.checkExists(triggerKey("t1")), "Expected existence of trigger named 't1' but checkExists return false.");

        job = sched.getJobDetail(jobKey("j1"));

        assertNotNull(job,"Stored job not found!");
        
        trigger = sched.getTrigger(triggerKey("t1"));

        assertNotNull(trigger,"Stored trigger not found!");

        job = newJob()
            .ofType(TestJob.class)
            .withIdentity("j2", "g1")
            .build();
    
        trigger = newTrigger()
            .withIdentity("t2", "g1")
            .forJob(job)
            .startNow()
            .withSchedule(simpleSchedule()
                    .repeatForever()
                    .withIntervalInSeconds(5))
             .build();

        sched.scheduleJob(job, trigger);
        
        job = newJob()
            .ofType(TestJob.class)
            .withIdentity("j3", "g1")
            .build();
    
        trigger = newTrigger()
            .withIdentity("t3", "g1")
            .forJob(job)
            .startNow()
            .withSchedule(simpleSchedule()
                    .repeatForever()
                    .withIntervalInSeconds(5))
             .build();
    
        sched.scheduleJob(job, trigger);
        
                
        List<String> jobGroups = sched.getJobGroupNames();
        List<String> triggerGroups = sched.getTriggerGroupNames();

        assertEquals(2, jobGroups.size(), "Job group list size expected to be = 2 ");
        assertEquals(2, triggerGroups.size(), "Trigger group list size expected to be = 2 ");
        
        Set<JobKey> jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP));
        Set<TriggerKey> triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(TriggerKey.DEFAULT_GROUP));

        assertEquals(1, jobKeys.size(), "Number of jobs expected in default group was 1 ");
        assertEquals(1, triggerKeys.size(), "Number of triggers expected in default group was 1 ");

        jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals("g1"));
        triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals("g1"));

        assertEquals(2, jobKeys.size(), "Number of jobs expected in 'g1' group was 2 ");
        assertEquals(2, triggerKeys.size(), "Number of triggers expected in 'g1' group was 2 ");

        
        TriggerState s = sched.getTriggerState(triggerKey("t2", "g1"));
        assertEquals(TriggerState.NORMAL, s, "State of trigger t2 expected to be NORMAL ");
        
        sched.pauseTrigger(triggerKey("t2", "g1"));
        s = sched.getTriggerState(triggerKey("t2", "g1"));
        assertEquals(TriggerState.PAUSED, s, "State of trigger t2 expected to be PAUSED ");

        sched.resumeTrigger(triggerKey("t2", "g1"));
        s = sched.getTriggerState(triggerKey("t2", "g1"));
        assertEquals(TriggerState.NORMAL, s, "State of trigger t2 expected to be NORMAL ");

        Set<String> pausedGroups = sched.getPausedTriggerGroups();
        assertTrue(pausedGroups.isEmpty(), "Size of paused trigger groups list expected to be 0 ");
        
        sched.pauseTriggers(GroupMatcher.triggerGroupEquals("g1"));
        
        // test that adding a trigger to a paused group causes the new trigger to be paused also... 
        job = newJob()
            .ofType(TestJob.class)
            .withIdentity("j4", "g1")
            .build();
    
        trigger = newTrigger()
            .withIdentity("t4", "g1")
            .forJob(job)
            .startNow()
            .withSchedule(simpleSchedule()
                    .repeatForever()
                    .withIntervalInSeconds(5))
             .build();
    
        sched.scheduleJob(job, trigger);

        pausedGroups = sched.getPausedTriggerGroups();
        assertEquals(1, pausedGroups.size(), "Size of paused trigger groups list expected to be 1 ");

        s = sched.getTriggerState(triggerKey("t2", "g1"));
        assertEquals(TriggerState.PAUSED, s, "State of trigger t2 expected to be PAUSED ");

        s = sched.getTriggerState(triggerKey("t4", "g1"));
        assertEquals(TriggerState.PAUSED, s, "State of trigger t4 expected to be PAUSED ");
        
        sched.resumeTriggers(GroupMatcher.triggerGroupEquals("g1"));
        s = sched.getTriggerState(triggerKey("t2", "g1"));
        assertEquals(TriggerState.NORMAL, s, "State of trigger t2 expected to be NORMAL ");
        s = sched.getTriggerState(triggerKey("t4", "g1"));
        assertEquals(TriggerState.NORMAL, s, "State of trigger t4 expected to be NORMAL ");
        pausedGroups = sched.getPausedTriggerGroups();
        assertEquals(0, pausedGroups.size(), "Size of paused trigger groups list expected to be 0 ");

        
        assertFalse(sched.unscheduleJob(triggerKey("foasldfksajdflk")), "Scheduler should have returned 'false' from attempt to unschedule non-existing trigger. ");

        assertTrue(sched.unscheduleJob(triggerKey("t3", "g1")), "Scheduler should have returned 'true' from attempt to unschedule existing trigger. ");
        
        jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals("g1"));
        triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals("g1"));

        assertEquals(2, jobKeys.size(), "Number of jobs expected in 'g1' group was 1 "); // job should have been deleted also, because it is non-durable
        assertEquals(2, triggerKeys.size(), "Number of triggers expected in 'g1' group was 1 ");

        assertTrue(sched.unscheduleJob(triggerKey("t1")), "Scheduler should have returned 'true' from attempt to unschedule existing trigger. ");
        
        jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP));
        triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(TriggerKey.DEFAULT_GROUP));

        assertEquals(1, jobKeys.size(), "Number of jobs expected in default group was 1 "); // job should have been left in place, because it is non-durable
        assertEquals(0, triggerKeys.size(), "Number of triggers expected in default group was 0 ");

        sched.shutdown(true);
    }

    @Test
    void testDurableStorageFunctions() throws Exception {
        Scheduler sched = createScheduler("testDurableStorageFunctions", 2);
        try {
            // test basic storage functions of scheduler...

            JobDetail job = newJob()
                    .ofType(TestJob.class)
                    .withIdentity("j1")
                    .storeDurably()
                    .build();

            assertFalse(sched.checkExists(jobKey("j1")), "Unexpected existence of job named 'j1'.");

            sched.addJob(job, false);

            assertTrue(sched.checkExists(jobKey("j1")), "Unexpected non-existence of job named 'j1'.");

            JobDetail nonDurableJob = newJob()
                    .ofType(TestJob.class)
                    .withIdentity("j2")
                    .build();

            try {
                sched.addJob(nonDurableJob, false);
                fail("Storage of non-durable job should not have succeeded.");
            }
            catch(SchedulerException expected) {
                assertFalse(sched.checkExists(jobKey("j2")), "Unexpected existence of job named 'j2'.");
            }

            sched.addJob(nonDurableJob, false, true);

            assertTrue(sched.checkExists(jobKey("j2")), "Unexpected non-existence of job named 'j2'.");
        } finally {
            sched.shutdown(true);
        }
    }

    @Test
    void testShutdownWithSleepReturnsAfterAllThreadsAreStopped() throws Exception {
      Map<Thread, StackTraceElement[]> allThreadsStart = Thread.getAllStackTraces();
      int threadPoolSize = 5;
      Scheduler scheduler = createScheduler("testShutdownWithSleepReturnsAfterAllThreadsAreStopped", threadPoolSize);
      
      Thread.sleep(500L);
      
      Map<Thread, StackTraceElement[]> allThreadsRunning = Thread.getAllStackTraces();

      scheduler.shutdown( true );
      
      Thread.sleep(200L);

      Map<Thread, StackTraceElement[]> allThreadsEnd = Thread.getAllStackTraces();
      Set<Thread> endingThreads = new HashSet<Thread>(allThreadsEnd.keySet());
      // remove all preexisting threads from the set
      for(Thread t: allThreadsStart.keySet()) {
        allThreadsEnd.remove(t);
      }
      // remove threads that are known artifacts of the test
      for(Thread t: endingThreads) {
        if(t.getName().contains("derby") && t.getThreadGroup().getName().contains("derby")) {
          allThreadsEnd.remove(t);
        }
        if(t.getThreadGroup() != null && t.getThreadGroup().getName().equals("system")) {
          allThreadsEnd.remove(t);
          
        }
        if(t.getThreadGroup() != null && t.getThreadGroup().getName().equals("main")) {
          allThreadsEnd.remove(t);
        }
      }
      if(!allThreadsEnd.isEmpty()) {
        // log the additional threads
        for(Thread t: allThreadsEnd.keySet()) {
          System.out.println("*** Found additional thread: " + t.getName() + " (of type " + t.getClass().getName() +")  in group: " + t.getThreadGroup().getName() + " with parent group: " + (t.getThreadGroup().getParent() == null ? "-none-" : t.getThreadGroup().getParent().getName()));
        }          
        // log all threads that were running before shutdown
        for(Thread t: allThreadsRunning.keySet()) {
          System.out.println("- Test runtime thread: " + t.getName() + " (of type " + t.getClass().getName() +")  in group: " + (t.getThreadGroup() == null ? "-none-" : (t.getThreadGroup().getName() + " with parent group: " + (t.getThreadGroup().getParent() == null ? "-none-" : t.getThreadGroup().getParent().getName()))));
        }          
      }
        assertEquals(0, allThreadsEnd.size(), "Found unexpected new threads (see console output for listing)");
    }

    @Test
    void testAbilityToFireImmediatelyWhenStartedBefore() throws Exception {
    	
		List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
		CyclicBarrier barrier = new CyclicBarrier(2);
    	
        Scheduler sched = createScheduler("testAbilityToFireImmediatelyWhenStartedBefore", 5);
        sched.getContext().put(BARRIER, barrier);
        sched.getContext().put(DATE_STAMPS, jobExecTimestamps);
        sched.start();
        
        Thread.yield();
        
		JobDetail job1 = JobBuilder.newJob(TestJobWithSync.class).withIdentity("job1").build();
		Trigger trigger1 = TriggerBuilder.newTrigger().forJob(job1).build(); 
		
		long sTime = System.currentTimeMillis();
		
		sched.scheduleJob(job1, trigger1);
		
	    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

	    sched.shutdown(true);

		long fTime = jobExecTimestamps.get(0);
		
		assertTrue((fTime - sTime  < 7000L), "Immediate trigger did not fire within a reasonable amount of time.");  // This is dangerously subjective!  but what else to do?
    }

    @Test
    void testAbilityToFireImmediatelyWhenStartedBeforeWithTriggerJob() throws Exception {
    	
		List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
		CyclicBarrier barrier = new CyclicBarrier(2);
    	
        Scheduler sched = createScheduler("testAbilityToFireImmediatelyWhenStartedBeforeWithTriggerJob", 5);
        sched.getContext().put(BARRIER, barrier);
        sched.getContext().put(DATE_STAMPS, jobExecTimestamps);

        sched.start();
        
        Thread.yield();

        JobDetail job1 = JobBuilder.newJob(TestJobWithSync.class).withIdentity("job1").storeDurably().build();
		sched.addJob(job1, false);
		
		long sTime = System.currentTimeMillis();
		
		sched.triggerJob(job1.getKey());
		
	    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

	    sched.shutdown(true);

		long fTime = jobExecTimestamps.get(0);
		
		assertTrue((fTime - sTime  < 7000L), "Immediate trigger did not fire within a reasonable amount of time.");  // This is dangerously subjective!  but what else to do?
    }

    @Test
    void testAbilityToFireImmediatelyWhenStartedAfter() throws Exception {
    	
		List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
		CyclicBarrier barrier = new CyclicBarrier(2);
    	
        Scheduler sched = createScheduler("testAbilityToFireImmediatelyWhenStartedAfter", 5);
        sched.getContext().put(BARRIER, barrier);
        sched.getContext().put(DATE_STAMPS, jobExecTimestamps);
        
		JobDetail job1 = JobBuilder.newJob(TestJobWithSync.class).withIdentity("job1").build();
		Trigger trigger1 = TriggerBuilder.newTrigger().forJob(job1).build(); 
		
		long sTime = System.currentTimeMillis();
		
		sched.scheduleJob(job1, trigger1);
        sched.start();
		
	    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	    
	    sched.shutdown(true);

		long fTime = jobExecTimestamps.get(0);
		
		assertTrue((fTime - sTime  < 7000L), "Immediate trigger did not fire within a reasonable amount of time.");  // This is dangerously subjective!  but what else to do?
    }

    @Test
	void testScheduleMultipleTriggersForAJob() throws SchedulerException {

		
		JobDetail job = newJob(TestJob.class).withIdentity("job1", "group1").build();
		Trigger trigger1 = newTrigger()
				.withIdentity("trigger1", "group1")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1)
								.repeatForever())
				.build();
		Trigger trigger2 = newTrigger()
				.withIdentity("trigger2", "group1")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1)
								.repeatForever())
				.build();
		Set<Trigger> triggersForJob = new HashSet<Trigger>(); 
		triggersForJob.add(trigger1);
		triggersForJob.add(trigger2);
		
		Scheduler sched = createScheduler("testScheduleMultipleTriggersForAJob", 5);
		sched.scheduleJob(job,triggersForJob, true);
		
		List<? extends Trigger> triggersOfJob = sched.getTriggersOfJob(job.getKey());
		assertEquals(2,triggersOfJob.size());
		assertTrue(triggersOfJob.contains(trigger1));
		assertTrue(triggersOfJob.contains(trigger2));
		
		sched.shutdown(true);
	}

    @Test
    void testShutdownWithoutWaitIsUnclean() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        Scheduler scheduler = createScheduler("testShutdownWithoutWaitIsUnclean", 8);
        try {
            scheduler.getContext().put(BARRIER, barrier);
            scheduler.start();
            scheduler.addJob(newJob().ofType(UncleanShutdownJob.class).withIdentity("job").storeDurably().build(), false);
            scheduler.scheduleJob(newTrigger().forJob("job").startNow().build());
            while (scheduler.getCurrentlyExecutingJobs().isEmpty()) {
                Thread.sleep(50);
            }
        } finally {
            scheduler.shutdown(false);
        }
        
        barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        Thread jobThread = (Thread) scheduler.getContext().get(JOB_THREAD);
        jobThread.join(TimeUnit.SECONDS.toMillis(TEST_TIMEOUT_SECONDS));
    }
    
    public static class UncleanShutdownJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                SchedulerContext schedulerContext = context.getScheduler().getContext();
                schedulerContext.put(JOB_THREAD, Thread.currentThread());
                CyclicBarrier barrier =  (CyclicBarrier) schedulerContext.get(BARRIER);
                barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new AssertionError("Await on barrier was interrupted: " + e.toString());
            } 
        }
    }

    @Test
    void testShutdownWithWaitIsClean() throws Exception {
        final AtomicBoolean shutdown = new AtomicBoolean(false);
        List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
        CyclicBarrier barrier = new CyclicBarrier(2);
        final Scheduler scheduler = createScheduler("testShutdownWithWaitIsClean", 8);
        try {
            scheduler.getContext().put(BARRIER, barrier);
            scheduler.getContext().put(DATE_STAMPS, jobExecTimestamps);
            scheduler.start();
            scheduler.addJob(newJob().ofType(TestJobWithSync.class).withIdentity("job").storeDurably().build(), false);
            scheduler.scheduleJob(newTrigger().forJob("job").startNow().build());
            while (scheduler.getCurrentlyExecutingJobs().isEmpty()) {
                Thread.sleep(50);
            }
        } finally {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        scheduler.shutdown(true);
                        shutdown.set(true);
                    } catch (SchedulerException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            t.start();
            Thread.sleep(1000);
            assertFalse(shutdown.get());
            barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            t.join();
        }
    }
}
