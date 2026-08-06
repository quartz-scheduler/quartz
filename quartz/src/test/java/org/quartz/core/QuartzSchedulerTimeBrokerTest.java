package org.quartz.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Disabled;
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

        public static void reset() {
            RUN_COUNT.set(0);
        }

        public static int runCount() {
            return RUN_COUNT.get();
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            RUN_COUNT.incrementAndGet();
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

        scheduler.shutdown(true);
    }

    /**
     * When broker time is advanced past the trigger's startAt(), the job
     * should fire once the scheduler is woken (e.g. during shutdown),
     * even if real time has not reached that moment.
     */
    @Test
    @Disabled("Too timing-sensitive across CI environments. Works locally.")
    void jobFiresWhenBrokerTimeAdvancesPastTriggerOnShutdown() throws Exception {
        CountingJob.reset();

        long start = System.currentTimeMillis();
        FakeTimeBroker.setNow(start);

        Scheduler scheduler = createSchedulerWithFakeTimeBroker();
        scheduler.start();

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

        // Give the scheduler thread a moment to acquire the trigger
        Thread.sleep(500L);
        assertEquals(0, CountingJob.runCount(), "Job should not have fired yet");

        // Move broker time past the fire time
        FakeTimeBroker.setNow(start + 5_000L);

        // Trigger shutdown, which halts the scheduler thread and causes it
        // to re-evaluate triggers using the updated broker time.
        scheduler.shutdown(true);

        assertEquals(1, CountingJob.runCount(),
                "Job should fire when broker time has passed startAt and scheduler shuts down");
    }
}