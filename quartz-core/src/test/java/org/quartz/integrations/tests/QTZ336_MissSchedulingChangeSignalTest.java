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
 * 
 */

package org.quartz.integrations.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for reproducing QTZ-336 where we don't check for the scheduling change signal.
 */
public class QTZ336_MissSchedulingChangeSignalTest {
    private static final Logger LOG = LoggerFactory.getLogger(QTZ336_MissSchedulingChangeSignalTest.class);
	
    @Test
    public void simpleScheduleAlwaysFiredUnder20s() throws Exception {
        Properties properties = new Properties();
        InputStream propertiesIs = getClass().getResourceAsStream("/org/quartz/quartz.properties");
        try {
            properties.load(propertiesIs);
        } finally {
            propertiesIs.close();
        }
        properties.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        // Use a custom RAMJobStore to produce context switches leading to the race condition
        properties.setProperty("org.quartz.jobStore.class", SlowRAMJobStore.class.getName());
        SchedulerFactory sf = new StdSchedulerFactory(properties);
        Scheduler sched = sf.getScheduler();
		LOG.info("------- Initialization Complete -----------");

		LOG.info("------- Scheduling Job  -------------------");

        JobDetail job = newJob(CollectDuractionBetweenFireTimesJob.class).withIdentity("job", "group").build();

        SimpleTrigger trigger = newTrigger()
	            .withIdentity("trigger1", "group1")
                .startAt(new Date(System.currentTimeMillis() + 1000))
	            .withSchedule(simpleSchedule()
                .withIntervalInSeconds(1)
	            .repeatForever()
	            .withMisfireHandlingInstructionIgnoreMisfires())
	            .build();

        sched.scheduleJob(job, trigger);
	        
		// Start up the scheduler (nothing can actually run until the
		// scheduler has been started)
		sched.start();

		LOG.info("------- Scheduler Started -----------------");
		

        // wait long enough so that the scheduler has an opportunity to
        // run the job in theory around 50 times
		try {
            Thread.sleep(50000L);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        List<Long> durationBetweenFireTimesInMillis = CollectDuractionBetweenFireTimesJob.getDurations();
        
        assertFalse("Job was not executed once!", durationBetweenFireTimesInMillis.isEmpty());
        
        // Let's check that every call for around 1 second and not between 23 and 30 seconds
        // which would be the case if the scheduling change signal were not checked
        for (long durationInMillis : durationBetweenFireTimesInMillis) {
            assertTrue("Missed an execution with one duration being between two fires: " + durationInMillis + " (all: "
                    + durationBetweenFireTimesInMillis + ")", durationInMillis < 20000);
        }
	}

    /**
     * A simple job for collecting fire times in order to check that we did not miss one call, for having the race
     * condition the job must be real quick and not allowing concurrent executions.
     */
    @DisallowConcurrentExecution
    public static class CollectDuractionBetweenFireTimesJob implements Job {
        private static final Logger log = LoggerFactory.getLogger(CollectDuractionBetweenFireTimesJob.class);
        private static final List<Long> durationBetweenFireTimes = Collections.synchronizedList(new ArrayList<Long>());
        private static Long lastFireTime = null;

        public void execute(JobExecutionContext context) throws JobExecutionException {
            Date now = new Date();
            log.info("Fire time: " + now);
            if (lastFireTime != null) {
                durationBetweenFireTimes.add(now.getTime() - lastFireTime);
            }
            lastFireTime = now.getTime();
        }

        /**
         * Retrieves the durations between fire times.
         * 
         * @return the durations in millis as an immutable list.
         */
        public static List<Long> getDurations() {
            synchronized (durationBetweenFireTimes) {
              return Collections.unmodifiableList(new ArrayList<Long>(durationBetweenFireTimes));
            }
        }

    }

    /**
     * Custom RAMJobStore for producing context switches.
     */
    public static class SlowRAMJobStore extends RAMJobStore {
        @Override
        public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) {
            List<OperableTrigger> nextTriggers = super.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
            try {
                // Wait just a bit for hopefully having a context switch leading to the race condition
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            return nextTriggers;
        }
    }
}
