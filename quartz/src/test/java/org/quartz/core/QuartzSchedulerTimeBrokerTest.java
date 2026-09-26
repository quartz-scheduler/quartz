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

package org.quartz.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Verifies that Quartz uses the configured TimeBroker as its time source,
 * rather than System.currentTimeMillis().
 * @author Thanos Tsiamis https://github.com/ThanosTsiamis
 */
public class QuartzSchedulerTimeBrokerTest {

    /**
     * Simple job that increments a static counter every time it runs.
     */
    public static class CountingJob implements Job {

        private static final AtomicInteger RUN_COUNT = new AtomicInteger(0);
        private static volatile CountDownLatch executionLatch = new CountDownLatch(1);

        public static void reset() {
            RUN_COUNT.set(0);
            executionLatch = new CountDownLatch(1);
        }

        public static int runCount() {
            return RUN_COUNT.get();
        }

        public static boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
            return executionLatch.await(timeout, unit);
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            RUN_COUNT.incrementAndGet();
            executionLatch.countDown();
        }
    }

    private Scheduler createSchedulerWithFakeTimeBroker() throws SchedulerException {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "TimeBrokerTest");
        props.setProperty("org.quartz.threadPool.threadCount", "1");
        props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

        props.setProperty("org.quartz.scheduler.timeBroker.class", "org.quartz.core.FakeTimeBroker");

        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(props);
        return factory.getScheduler();
    }

    /**
     * If broker time is frozen before the trigger's startAt(), the job
     * should not fire, even if real time moves forward.
     */
    @Test
    void jobDoesNotFireWhileBrokerTimeIsFrozen() throws Exception {
        CountingJob.reset();

        long start = System.currentTimeMillis();
        FakeTimeBroker.setNow(start); // broker == system at start

        Scheduler scheduler = createSchedulerWithFakeTimeBroker();
        try {
            scheduler.start();

            JobDetail job = JobBuilder.newJob(CountingJob.class)
                    .withIdentity("job1", "group1")
                    .build();

            // Fire 2 seconds after "broker start"
            Date fireTime = new Date(start + 2_000L);
            Trigger trigger = newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startAt(fireTime)
                    .build();

            scheduler.scheduleJob(job, trigger);

            // Wait longer than 2 seconds in REAL time,
            // but do NOT move FakeTimeBroker
            Thread.sleep(5_000L);

            assertEquals(0, CountingJob.runCount(),
                    "Job should not fire while broker time is frozen");
        } finally {
            scheduler.shutdown(true);
        }
    }

    /**
     * When broker time is advanced while the scheduler is in standby, the job
     * should fire after the scheduler is resumed, even if real time has not
     * reached the trigger's start time.
     */
    @Test
    void jobFiresWhenBrokerTimeAdvancesWhileInStandby() throws Exception {
        CountingJob.reset();

        long start = System.currentTimeMillis();
        FakeTimeBroker.setNow(start);

        Scheduler scheduler = createSchedulerWithFakeTimeBroker();
        try {
            scheduler.start();
            scheduler.standby();

            JobDetail job = JobBuilder.newJob(CountingJob.class)
                    .withIdentity("job2", "group1")
                    .build();

            // Fire 2 seconds after "broker start"
            Date fireTime = new Date(start + 2_000L);
            Trigger trigger = newTrigger()
                    .withIdentity("trigger2", "group1")
                    .startAt(fireTime)
                    .build();

            scheduler.scheduleJob(job, trigger);

            assertEquals(0, CountingJob.runCount(), "Job should not have fired yet");

            // Move broker time past the fire time
            FakeTimeBroker.setNow(start + 5_000L);

            // Resuming wakes the scheduler thread and causes it to re-evaluate
            // triggers using the updated broker time.
            scheduler.start();

            assertTrue(CountingJob.awaitExecution(5, TimeUnit.SECONDS),
                    "Job should fire after the scheduler resumes");
        } finally {
            scheduler.shutdown(true);
        }

        assertEquals(1, CountingJob.runCount(),
                "Job should fire when broker time has passed startAt and the scheduler resumes");
    }
}
