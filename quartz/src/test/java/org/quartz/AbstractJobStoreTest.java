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

import java.util.Date;
import java.util.List;
import java.util.Set;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit test for JobStores.  These tests were submitted by Johannes Zillmann
 * as part of issue QUARTZ-306.
 */
public abstract class AbstractJobStoreTest  {
    private JobStore fJobStore;
    private JobDetailImpl fJobDetail;
    private SampleSignaler fSignaler;

    @SuppressWarnings("deprecation")
    @BeforeEach
    protected void setUp() throws Exception {
        this.fSignaler = new SampleSignaler();
        ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
        loadHelper.initialize();
        this.fJobStore = createJobStore("AbstractJobStoreTest");
        this.fJobStore.initialize(loadHelper, this.fSignaler);
        this.fJobStore.schedulerStarted();

        this.fJobDetail = new JobDetailImpl("job1", "jobGroup1", MyJob.class);
        this.fJobDetail.setDurability(true);
        this.fJobStore.storeJob(this.fJobDetail, false);
    }

    @AfterEach
    protected void tearDown() {
        destroyJobStore("AbstractJobStoreTest");
    }

    protected abstract JobStore createJobStore(String name);

    protected abstract void destroyJobStore(String name);

    @SuppressWarnings("deprecation")
    @Test
    void testAcquireNextTrigger() throws Exception {
    	
    	Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
    	long baseFireTime = baseFireTimeDate.getTime();
    	
        OperableTrigger trigger1 = 
            new SimpleTriggerImpl("trigger1", "triggerGroup1", this.fJobDetail.getName(), 
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 200000), 
                    new Date(baseFireTime + 200000), 2, 2000);
        OperableTrigger trigger2 = 
            new SimpleTriggerImpl("trigger2", "triggerGroup1", this.fJobDetail.getName(), 
                    this.fJobDetail.getGroup(), new Date(baseFireTime +  50000),
                    new Date(baseFireTime + 200000), 2, 2000);
        OperableTrigger trigger3 = 
            new SimpleTriggerImpl("trigger1", "triggerGroup2", this.fJobDetail.getName(), 
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 100000), 
                    new Date(baseFireTime + 200000), 2, 2000);

        trigger1.computeFirstFireTime(null);
        trigger2.computeFirstFireTime(null);
        trigger3.computeFirstFireTime(null);
        this.fJobStore.storeTrigger(trigger1, false);
        this.fJobStore.storeTrigger(trigger2, false);
        this.fJobStore.storeTrigger(trigger3, false);
        
        long firstFireTime = new Date(trigger1.getNextFireTime().getTime()).getTime();

        assertTrue(this.fJobStore.acquireNextTriggers(10, 1, 0L).isEmpty());
        assertEquals(
            trigger2.getKey(), 
            this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).get(0).getKey());
        assertEquals(
            trigger3.getKey(), 
            this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).get(0).getKey());
        assertEquals(
            trigger1.getKey(), 
            this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).get(0).getKey());
        assertTrue(
            this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).isEmpty());


        // release trigger3
        this.fJobStore.releaseAcquiredTrigger(trigger3);
        assertEquals(
            trigger3, 
            this.fJobStore.acquireNextTriggers(new Date(trigger1.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).get(0));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testAcquireNextTriggerBatch() throws Exception {
    	
    	long baseFireTime = System.currentTimeMillis() - 1000;
    	
        OperableTrigger early =
            new SimpleTriggerImpl("early", "triggerGroup1", this.fJobDetail.getName(),
                    this.fJobDetail.getGroup(), new Date(baseFireTime),
                    new Date(baseFireTime + 5), 2, 2000);
        OperableTrigger trigger1 =
            new SimpleTriggerImpl("trigger1", "triggerGroup1", this.fJobDetail.getName(),
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 200000),
                    new Date(baseFireTime + 200005), 2, 2000);
        OperableTrigger trigger2 =
            new SimpleTriggerImpl("trigger2", "triggerGroup1", this.fJobDetail.getName(),
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 210000),
                    new Date(baseFireTime + 210005), 2, 2000);
        OperableTrigger trigger3 =
            new SimpleTriggerImpl("trigger3", "triggerGroup1", this.fJobDetail.getName(),
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 220000),
                    new Date(baseFireTime + 220005), 2, 2000);
        OperableTrigger trigger4 =
            new SimpleTriggerImpl("trigger4", "triggerGroup1", this.fJobDetail.getName(),
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 230000),
                    new Date(baseFireTime + 230005), 2, 2000);

        OperableTrigger trigger10 =
            new SimpleTriggerImpl("trigger10", "triggerGroup2", this.fJobDetail.getName(),
                    this.fJobDetail.getGroup(), new Date(baseFireTime + 500000),
                    new Date(baseFireTime + 700000), 2, 2000);

        early.computeFirstFireTime(null);
        early.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
        trigger1.computeFirstFireTime(null);
        trigger2.computeFirstFireTime(null);
        trigger3.computeFirstFireTime(null);
        trigger4.computeFirstFireTime(null);
        trigger10.computeFirstFireTime(null);
        this.fJobStore.storeTrigger(early, false);
        this.fJobStore.storeTrigger(trigger1, false);
        this.fJobStore.storeTrigger(trigger2, false);
        this.fJobStore.storeTrigger(trigger3, false);
        this.fJobStore.storeTrigger(trigger4, false);
        this.fJobStore.storeTrigger(trigger10, false);
        
        long firstFireTime = new Date(trigger1.getNextFireTime().getTime()).getTime();

        List<OperableTrigger> acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 4, 1000L);
        assertEquals(1, acquiredTriggers.size());
        assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
        this.fJobStore.releaseAcquiredTrigger(early);

        acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 4, 205000);
        assertEquals(2, acquiredTriggers.size());
        assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
        assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
        this.fJobStore.releaseAcquiredTrigger(early);
        this.fJobStore.releaseAcquiredTrigger(trigger1);
        
        this.fJobStore.removeTrigger(early.getKey());
        
        acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 5, 100000L);
        assertEquals(4, acquiredTriggers.size());
        assertEquals(trigger1.getKey(), acquiredTriggers.get(0).getKey());
        assertEquals(trigger2.getKey(), acquiredTriggers.get(1).getKey());
        assertEquals(trigger3.getKey(), acquiredTriggers.get(2).getKey());
        assertEquals(trigger4.getKey(), acquiredTriggers.get(3).getKey());
        this.fJobStore.releaseAcquiredTrigger(trigger1);
        this.fJobStore.releaseAcquiredTrigger(trigger2);
        this.fJobStore.releaseAcquiredTrigger(trigger3);
        this.fJobStore.releaseAcquiredTrigger(trigger4);

        acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 10000, 6, 100000L);
        assertEquals(4, acquiredTriggers.size());
        assertEquals(trigger1.getKey(), acquiredTriggers.get(0).getKey());
        assertEquals(trigger2.getKey(), acquiredTriggers.get(1).getKey());
        assertEquals(trigger3.getKey(), acquiredTriggers.get(2).getKey());
        assertEquals(trigger4.getKey(), acquiredTriggers.get(3).getKey());
        this.fJobStore.releaseAcquiredTrigger(trigger1);
        this.fJobStore.releaseAcquiredTrigger(trigger2);
        this.fJobStore.releaseAcquiredTrigger(trigger3);
        this.fJobStore.releaseAcquiredTrigger(trigger4);

        acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 1, 5, 0L);
        assertEquals(1, acquiredTriggers.size());
        assertEquals(trigger1.getKey(), acquiredTriggers.get(0).getKey());
        this.fJobStore.releaseAcquiredTrigger(trigger1);

        acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 250, 5, 19999L);
        assertEquals(2, acquiredTriggers.size());
        assertEquals(trigger1.getKey(), acquiredTriggers.get(0).getKey());
        assertEquals(trigger2.getKey(), acquiredTriggers.get(1).getKey());
        this.fJobStore.releaseAcquiredTrigger(trigger1);
        this.fJobStore.releaseAcquiredTrigger(trigger2);
        this.fJobStore.releaseAcquiredTrigger(trigger3);
        
        acquiredTriggers = this.fJobStore.acquireNextTriggers(firstFireTime + 150, 5, 5000L);
        assertEquals(1, acquiredTriggers.size());
        assertEquals(trigger1.getKey(), acquiredTriggers.get(0).getKey());
        this.fJobStore.releaseAcquiredTrigger(trigger1);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testTriggerStates() throws Exception {
        OperableTrigger trigger = 
            new SimpleTriggerImpl("trigger1", "triggerGroup1", this.fJobDetail.getName(), this.fJobDetail.getGroup(), 
                    new Date(System.currentTimeMillis() + 100000), new Date(System.currentTimeMillis() + 200000), 2, 2000);
        trigger.computeFirstFireTime(null);
        assertEquals(TriggerState.NONE, this.fJobStore.getTriggerState(trigger.getKey()));
        this.fJobStore.storeTrigger(trigger, false);
        assertEquals(TriggerState.NORMAL, this.fJobStore.getTriggerState(trigger.getKey()));
    
        this.fJobStore.pauseTrigger(trigger.getKey());
        assertEquals(TriggerState.PAUSED, this.fJobStore.getTriggerState(trigger.getKey()));
    
        this.fJobStore.resumeTrigger(trigger.getKey());
        assertEquals(TriggerState.NORMAL, this.fJobStore.getTriggerState(trigger.getKey()));
    
        trigger = this.fJobStore.acquireNextTriggers(
                new Date(trigger.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).get(0);
        assertNotNull(trigger);
        this.fJobStore.releaseAcquiredTrigger(trigger);
        trigger=this.fJobStore.acquireNextTriggers(
                new Date(trigger.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).get(0);
        assertNotNull(trigger);
        assertTrue(this.fJobStore.acquireNextTriggers(
                new Date(trigger.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).isEmpty());
    }

    // See: http://jira.opensymphony.com/browse/QUARTZ-606
    @SuppressWarnings("deprecation")
    @Disabled
    @Test
    void testStoreTriggerReplacesTrigger() throws Exception {

        String jobName = "StoreTriggerReplacesTrigger";
        String jobGroup = "StoreTriggerReplacesTriggerGroup";
        JobDetailImpl detail = new JobDetailImpl(jobName, jobGroup, MyJob.class);
        fJobStore.storeJob(detail, false);
 
        String trName = "StoreTriggerReplacesTrigger";
        String trGroup = "StoreTriggerReplacesTriggerGroup";
        OperableTrigger tr = new SimpleTriggerImpl(trName ,trGroup, new Date());
        tr.setJobKey(new JobKey(jobName, jobGroup));
        tr.setCalendarName(null);
 
        fJobStore.storeTrigger(tr, false);
        assertEquals(tr,fJobStore.retrieveTrigger(tr.getKey()));
 
        try {
            fJobStore.storeTrigger(tr, false);
            fail("an attempt to store duplicate trigger succeeded");
        } catch(ObjectAlreadyExistsException oaee) {
            // expected
        }

        tr.setCalendarName("QQ");
        fJobStore.storeTrigger(tr, true); //fails here
        assertEquals(tr, fJobStore.retrieveTrigger(tr.getKey()));
        assertEquals( "StoreJob doesn't replace triggers", "QQ", fJobStore.retrieveTrigger(tr.getKey()).getCalendarName());
    }

    @SuppressWarnings("deprecation")
    @Test
    void testPauseJobGroupPausesNewJob() throws Exception
    {
    	// Pausing job groups in JDBCJobStore is broken, see QTZ-208
    	if (fJobStore instanceof JobStoreSupport)
    		return;
    	
    	final String jobName1 = "PauseJobGroupPausesNewJob";
    	final String jobName2 = "PauseJobGroupPausesNewJob2";
    	final String jobGroup = "PauseJobGroupPausesNewJobGroup";
    
    	JobDetailImpl detail = new JobDetailImpl(jobName1, jobGroup, MyJob.class);
    	detail.setDurability(true);
    	fJobStore.storeJob(detail, false);
    	fJobStore.pauseJobs(GroupMatcher.jobGroupEquals(jobGroup));
    
    	detail = new JobDetailImpl(jobName2, jobGroup, MyJob.class);
    	detail.setDurability(true);
    	fJobStore.storeJob(detail, false);
    
    	String trName = "PauseJobGroupPausesNewJobTrigger";
    	String trGroup = "PauseJobGroupPausesNewJobTriggerGroup";
    	OperableTrigger tr = new SimpleTriggerImpl(trName, trGroup, new Date());
        tr.setJobKey(new JobKey(jobName2, jobGroup));
    	fJobStore.storeTrigger(tr, false);
    	assertEquals(TriggerState.PAUSED, fJobStore.getTriggerState(tr.getKey()));
    }
    
@Test
    void testStoreAndRetrieveJobs() throws Exception {
        SchedulerSignaler schedSignaler = new SampleSignaler();
        ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
        loadHelper.initialize();

        JobStore store = createJobStore("testStoreAndRetrieveJobs");
        store.initialize(loadHelper, schedSignaler);
		
		// Store jobs.
		for (int i=0; i < 10; i++) {
            String group =  i < 5 ? "a" : "b";
			JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i, group).build();
			store.storeJob(job, false);
		}
		// Retrieve jobs.
		for (int i=0; i < 10; i++) {
            String group =  i < 5 ? "a" : "b";
			JobKey jobKey = JobKey.jobKey("job" + i, group);
			JobDetail storedJob = store.retrieveJob(jobKey);
			assertEquals(jobKey, storedJob.getKey());
		}
       // Retrieve by group
    assertEquals(5, store.getJobKeys(GroupMatcher.jobGroupEquals("a")).size(), "Wrong number of jobs in group 'a'");
    assertEquals(5, store.getJobKeys(GroupMatcher.jobGroupEquals("b")).size(), "Wrong number of jobs in group 'b'");
}

	@Test
	void testStoreAndRetrieveTriggers() throws Exception {
        SchedulerSignaler schedSignaler = new SampleSignaler();
        ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
        loadHelper.initialize();

        JobStore store = createJobStore("testStoreAndRetrieveTriggers");
        store.initialize(loadHelper, schedSignaler);
		
		// Store jobs and triggers.
		for (int i=0; i < 10; i++) {
            String group =  i < 5 ? "a" : "b";
			JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i, group).build();
			store.storeJob(job, true);
			SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule();
			Trigger trigger = TriggerBuilder.newTrigger().withIdentity("job" + i, group).withSchedule(schedule).forJob(job).build();
			store.storeTrigger((OperableTrigger)trigger, true);
		}
		// Retrieve job and trigger.
		for (int i=0; i < 10; i++) {
            String group =  i < 5 ? "a" : "b";
			JobKey jobKey = JobKey.jobKey("job" + i, group);
			JobDetail storedJob = store.retrieveJob(jobKey);
			assertEquals(jobKey, storedJob.getKey());
			
			TriggerKey triggerKey = TriggerKey.triggerKey("job" + i, group);
			Trigger storedTrigger = store.retrieveTrigger(triggerKey);
			assertEquals(triggerKey, storedTrigger.getKey());
		}
        // Retrieve by group
        assertEquals(5, store.getJobKeys(GroupMatcher.jobGroupEquals("a")).size(), "Wrong number of jobs in group 'a'");
        assertEquals(5, store.getJobKeys(GroupMatcher.jobGroupEquals("b")).size(), "Wrong number of jobs in group 'b'");
    }

    @Test
    void testMatchers() throws Exception {
        SchedulerSignaler schedSignaler = new SampleSignaler();
        ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
        loadHelper.initialize();

        JobStore store = createJobStore("testMatchers");
        store.initialize(loadHelper, schedSignaler);

        JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job1", "aaabbbccc").build();
        store.storeJob(job, true);
        SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trig1", "aaabbbccc").withSchedule(schedule).forJob(job).build();
        store.storeTrigger((OperableTrigger) trigger, true);

        job = JobBuilder.newJob(MyJob.class).withIdentity("job1", "xxxyyyzzz").build();
        store.storeJob(job, true);
        schedule = SimpleScheduleBuilder.simpleSchedule();
        trigger = TriggerBuilder.newTrigger().withIdentity("trig1", "xxxyyyzzz").withSchedule(schedule).forJob(job).build();
        store.storeTrigger((OperableTrigger) trigger, true);

        job = JobBuilder.newJob(MyJob.class).withIdentity("job2", "xxxyyyzzz").build();
        store.storeJob(job, true);
        schedule = SimpleScheduleBuilder.simpleSchedule();
        trigger = TriggerBuilder.newTrigger().withIdentity("trig2", "xxxyyyzzz").withSchedule(schedule).forJob(job).build();
        store.storeTrigger((OperableTrigger) trigger, true);

        Set<JobKey> jkeys = store.getJobKeys(GroupMatcher.anyJobGroup());
        assertEquals(3, jkeys.size(), "Wrong number of jobs found by anything matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupEquals("xxxyyyzzz"));
        assertEquals(2, jkeys.size(), "Wrong number of jobs found by equals matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupEquals("aaabbbccc"));
        assertEquals(1, jkeys.size(), "Wrong number of jobs found by equals matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupStartsWith("aa"));
        assertEquals(1, jkeys.size(), "Wrong number of jobs found by starts with matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupStartsWith("xx"));
        assertEquals(2, jkeys.size(), "Wrong number of jobs found by starts with matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupEndsWith("cc"));
        assertEquals(1, jkeys.size(), "Wrong number of jobs found by ends with matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupEndsWith("zzz"));
        assertEquals(2, jkeys.size(), "Wrong number of jobs found by ends with matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupContains("bc"));
        assertEquals(1, jkeys.size(), "Wrong number of jobs found by contains with matcher");

        jkeys = store.getJobKeys(GroupMatcher.jobGroupContains("yz"));
        assertEquals(2, jkeys.size(), "Wrong number of jobs found by contains with matcher");

        Set<TriggerKey> tkeys = store.getTriggerKeys(GroupMatcher.anyTriggerGroup());
        assertEquals(3, tkeys.size(), "Wrong number of triggers found by anything matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupEquals("xxxyyyzzz"));
        assertEquals(2, tkeys.size(), "Wrong number of triggers found by equals matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupEquals("aaabbbccc"));
        assertEquals(1, tkeys.size(), "Wrong number of triggers found by equals matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("aa"));
        assertEquals(1, tkeys.size(), "Wrong number of triggers found by starts with matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("xx"));
        assertEquals(2, tkeys.size(), "Wrong number of triggers found by starts with matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("cc"));
        assertEquals(1, tkeys.size(), "Wrong number of triggers found by ends with matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("zzz"));
        assertEquals(2, tkeys.size(), "Wrong number of triggers found by ends with matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupContains("bc"));
        assertEquals(1, tkeys.size(), "Wrong number of triggers found by contains with matcher");

        tkeys = store.getTriggerKeys(GroupMatcher.triggerGroupContains("yz"));
        assertEquals(2, tkeys.size(), "Wrong number of triggers found by contains with matcher");
    }

    @Test
	void testAcquireTriggers() throws Exception {
		SchedulerSignaler schedSignaler = new SampleSignaler();
		ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
		loadHelper.initialize();
		
        JobStore store = createJobStore("testAcquireTriggers");
		store.initialize(loadHelper, schedSignaler);
		
		// Setup: Store jobs and triggers.
		long MIN = 60 * 1000L;
		Date startTime0 = new Date(System.currentTimeMillis() + MIN); // a min from now.
		for (int i=0; i < 10; i++) {
			Date startTime = new Date(startTime0.getTime() + i * MIN); // a min apart
			JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i).build();
			SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatMinutelyForever(2);
			OperableTrigger trigger = (OperableTrigger)TriggerBuilder.newTrigger().withIdentity("job" + i).withSchedule(schedule).forJob(job).startAt(startTime).build();
			
			// Manually trigger the first fire time computation that scheduler would do. Otherwise 
			// the store.acquireNextTriggers() will not work properly.
	        Date fireTime = trigger.computeFirstFireTime(null);
            assertNotNull(fireTime);
			
			store.storeJobAndTrigger(job, trigger);
		}
		
		// Test acquire one trigger at a time
		for (int i=0; i < 10; i++) {
			long noLaterThan = (startTime0.getTime() + i * MIN);
			int maxCount = 1;
			long timeWindow = 0;
			List<OperableTrigger> triggers = store.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
			assertEquals(1, triggers.size());
			assertEquals("job" + i, triggers.get(0).getKey().getName());
			
			// Let's remove the trigger now.
			store.removeJob(triggers.get(0).getJobKey());
		}
	}
	@Test
	void testAcquireTriggersInBatch() throws Exception {
		SchedulerSignaler schedSignaler = new SampleSignaler();
		ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
		loadHelper.initialize();
		
        JobStore store = createJobStore("testAcquireTriggersInBatch");
		store.initialize(loadHelper, schedSignaler);
		
		// Setup: Store jobs and triggers.
		long MIN = 60 * 1000L;
		Date startTime0 = new Date(System.currentTimeMillis() + MIN); // a min from now.
		for (int i=0; i < 10; i++) {
			Date startTime = new Date(startTime0.getTime() + i * MIN); // a min apart
			JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i).build();
			SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatMinutelyForever(2);
			OperableTrigger trigger = (OperableTrigger)TriggerBuilder.newTrigger().withIdentity("job" + i).withSchedule(schedule).forJob(job).startAt(startTime).build();
			
			// Manually trigger the first fire time computation that scheduler would do. Otherwise 
			// the store.acquireNextTriggers() will not work properly.
	        Date fireTime = trigger.computeFirstFireTime(null);
            assertNotNull(fireTime);
			
			store.storeJobAndTrigger(job, trigger);
		}
		
		// Test acquire batch of triggers at a time
		long noLaterThan = startTime0.getTime() + 10 * MIN;
		int maxCount = 7;
		// time window needs to be big to be able to pick up multiple triggers when they are a minute apart
		long timeWindow = 8 * MIN; 
		List<OperableTrigger> triggers = store.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
		assertEquals(7, triggers.size());
		for (int i=0; i < 7; i++) {
			assertEquals("job" + i, triggers.get(i).getKey().getName());
		}
	}
@Test
    void testResetErrorTrigger() throws Exception {

        Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
        long baseFireTime = baseFireTimeDate.getTime();

        // create and store a trigger
        OperableTrigger trigger1 =
                new SimpleTriggerImpl("trigger1", "triggerGroup1", this.fJobDetail.getName(),
                        this.fJobDetail.getGroup(), new Date(baseFireTime + 200000),
                        new Date(baseFireTime + 200000), 2, 2000);

        trigger1.computeFirstFireTime(null);
        this.fJobStore.storeTrigger(trigger1, false);

        long firstFireTime = new Date(trigger1.getNextFireTime().getTime()).getTime();


        // pretend to fire it
        List<OperableTrigger> aqTs = this.fJobStore.acquireNextTriggers(
                firstFireTime + 10000, 1, 0L);
        assertEquals(trigger1.getKey(), aqTs.get(0).getKey());

        List<TriggerFiredResult> fTs = this.fJobStore.triggersFired(aqTs);
        TriggerFiredResult ft = fTs.get(0);

        // get the trigger into error state
        this.fJobStore.triggeredJobComplete(ft.getTriggerFiredBundle().getTrigger(), ft.getTriggerFiredBundle().getJobDetail(), Trigger.CompletedExecutionInstruction.SET_TRIGGER_ERROR);
        TriggerState state = this.fJobStore.getTriggerState(trigger1.getKey());
        assertEquals(TriggerState.ERROR, state);

        // test reset
        this.fJobStore.resetTriggerFromErrorState(trigger1.getKey());
        state = this.fJobStore.getTriggerState(trigger1.getKey());
        assertEquals(TriggerState.NORMAL, state);
    }

    @Test
    void testStoreJobReplaceExistingWithIdenticalData() throws Exception {

        JobDetailImpl job = new JobDetailImpl("testJob", "testGroup", MyJob.class);
        job.setDescription("Test job description");
        job.setDurability(true);

        // First store - should succeed
        assertDoesNotThrow(() -> {
            this.fJobStore.storeJob(job, false);
        });

        // Verify job exists
        JobDetail retrievedJob = this.fJobStore.retrieveJob(job.getKey());
        assertNotNull(retrievedJob);
        assertEquals(job.getKey(), retrievedJob.getKey());
        assertEquals(job.getDescription(), retrievedJob.getDescription());

        JobDetailImpl identicalJob = new JobDetailImpl("testJob", "testGroup", MyJob.class);
        identicalJob.setDescription("Test job description");
        identicalJob.setDurability(true);

        assertDoesNotThrow(() -> {
            this.fJobStore.storeJob(identicalJob, true);
        }, "storeJob with replaceExisting=true should not fail when data is identical");

        // Verify job still exists and data is correct
        JobDetail finalJob = this.fJobStore.retrieveJob(job.getKey());
        assertNotNull(finalJob);
        assertEquals(job.getKey(), finalJob.getKey());
        assertEquals(job.getDescription(), finalJob.getDescription());
    }

    public static class SampleSignaler implements SchedulerSignaler {
        volatile int fMisfireCount = 0;

        public void notifyTriggerListenersMisfired(Trigger trigger) {
        	System.out.println("Trigger misfired: " + trigger.getKey() + ", fire time: " + trigger.getNextFireTime());
            fMisfireCount++;
        }

        public void signalSchedulingChange(long candidateNewNextFireTime) {
        }

        public void notifySchedulerListenersFinalized(Trigger trigger) {
        }

        public void notifySchedulerListenersJobDeleted(JobKey jobKey) {
        }
        
        public void notifySchedulerListenersError(String string, SchedulerException jpe) {
        }
    }

    /** An empty job for testing purpose. */
    public static class MyJob implements Job {
        public void execute(JobExecutionContext context) throws JobExecutionException {
            //
        }
    }

}
