package org.quartz.plugins.interrupt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.spi.ClassLoadHelper;

/**
 * Tests for {@link JobInterruptMonitorPlugin} addressing issues from
 * <a href="https://github.com/quartz-scheduler/quartz/issues/1349">#1349</a>.
 */
class JobInterruptMonitorPluginTest {

    private JobInterruptMonitorPlugin plugin;
    private Scheduler scheduler;

    @BeforeEach
    void setUp() throws SchedulerException {
        plugin = new JobInterruptMonitorPlugin();
        scheduler = mock(Scheduler.class);
        SchedulerContext schedulerContext = new SchedulerContext();
        when(scheduler.getContext()).thenReturn(schedulerContext);
        ListenerManager listenerManager = mock(ListenerManager.class);
        when(scheduler.getListenerManager()).thenReturn(listenerManager);
        plugin.initialize("testPlugin", scheduler, mock(ClassLoadHelper.class));
    }

    // --- Bug 1: ScheduledThreadPoolExecutor memory leak ---

    @Test
    void cancelledMonitorsShouldBeRemovedFromExecutorQueue() throws Exception {
        JobExecutionContext context = createContext("job1", 60_000L);

        plugin.triggerFired(mock(Trigger.class), context);

        // Cancel by completing the job
        plugin.triggerComplete(mock(Trigger.class), context, CompletedExecutionInstruction.NOOP);

        // The cancelled task should be removed from the executor queue immediately
        ScheduledThreadPoolExecutor exec = getExecutor();
        assertEquals(0, exec.getQueue().size(),
                "Cancelled tasks should be removed from the executor queue immediately");
    }

    // --- Bug 2: Single future field overwritten by concurrent jobs ---

    @Test
    void completingOneJobShouldNotCancelAnotherJobsMonitor() throws Exception {
        JobExecutionContext ctx1 = createContext("job1", 200L);
        JobExecutionContext ctx2 = createContext("job2", 200L);

        // Fire both jobs
        plugin.triggerFired(mock(Trigger.class), ctx1);
        plugin.triggerFired(mock(Trigger.class), ctx2);

        // Complete job1 immediately (within timeout)
        plugin.triggerComplete(mock(Trigger.class), ctx1, CompletedExecutionInstruction.NOOP);

        // Wait for the monitors to fire (200ms + buffer)
        Thread.sleep(500);

        // job1 completed normally — should NOT be interrupted
        verify(scheduler, never()).interrupt(JobKey.jobKey("job1"));
        // job2 is still running past timeout — SHOULD be interrupted
        verify(scheduler).interrupt(JobKey.jobKey("job2"));
    }

    @Test
    void completingJobShouldOnlyCancelItsOwnMonitor() throws Exception {
        JobExecutionContext ctx1 = createContext("job1", 60_000L);
        JobExecutionContext ctx2 = createContext("job2", 60_000L);

        // Fire both jobs
        plugin.triggerFired(mock(Trigger.class), ctx1);
        plugin.triggerFired(mock(Trigger.class), ctx2);

        // Complete job1
        plugin.triggerComplete(mock(Trigger.class), ctx1, CompletedExecutionInstruction.NOOP);

        // One monitor should still be pending (job2's)
        ScheduledThreadPoolExecutor exec = getExecutor();
        long pendingTasks = exec.getQueue().size();
        assertEquals(1, pendingTasks,
                "Completing job1 should only cancel job1's monitor; job2's monitor should remain");
    }

    // --- Bug 3: getJobDataMap() vs getMergedJobDataMap() ---

    @Test
    void shouldReadAutoInterruptableFromMergedJobDataMap() throws Exception {
        // AUTO_INTERRUPTIBLE only on the trigger's data map, not on the job's
        JobDataMap jobDataMap = new JobDataMap(); // empty — no AUTO_INTERRUPTIBLE
        JobDataMap mergedDataMap = new JobDataMap();
        mergedDataMap.put(JobInterruptMonitorPlugin.AUTO_INTERRUPTIBLE, true);
        mergedDataMap.put(JobInterruptMonitorPlugin.MAX_RUN_TIME, 200L);

        JobExecutionContext context = createContextWithSeparateMaps(
                "job1", jobDataMap, mergedDataMap);

        plugin.triggerFired(mock(Trigger.class), context);

        // Wait for monitor to fire
        Thread.sleep(500);

        // Should have scheduled a monitor and interrupted the job
        verify(scheduler).interrupt(JobKey.jobKey("job1"));
    }

    @Test
    void shouldReadMaxRunTimeFromMergedJobDataMap() throws Exception {
        // MAX_RUN_TIME only on the trigger's data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(JobInterruptMonitorPlugin.AUTO_INTERRUPTIBLE, true);
        // No MAX_RUN_TIME on job data map

        JobDataMap mergedDataMap = new JobDataMap();
        mergedDataMap.put(JobInterruptMonitorPlugin.AUTO_INTERRUPTIBLE, true);
        mergedDataMap.put(JobInterruptMonitorPlugin.MAX_RUN_TIME, 200L);

        JobExecutionContext context = createContextWithSeparateMaps(
                "job1", jobDataMap, mergedDataMap);

        plugin.triggerFired(mock(Trigger.class), context);

        // Wait for the short merged timeout (200ms) + buffer
        Thread.sleep(500);

        // Should have been interrupted using the merged MAX_RUN_TIME (200ms),
        // NOT the default (300000ms)
        verify(scheduler).interrupt(JobKey.jobKey("job1"));
    }

    // --- Helpers ---

    private JobExecutionContext createContext(String jobName, long maxRunTime) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(JobInterruptMonitorPlugin.AUTO_INTERRUPTIBLE, true);
        dataMap.put(JobInterruptMonitorPlugin.MAX_RUN_TIME, maxRunTime);
        return createContextWithSeparateMaps(jobName, dataMap, dataMap);
    }

    private JobExecutionContext createContextWithSeparateMaps(
            String jobName, JobDataMap jobDetailDataMap, JobDataMap mergedDataMap) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);

        when(jobDetail.getJobDataMap()).thenReturn(jobDetailDataMap);
        when(jobDetail.getKey()).thenReturn(JobKey.jobKey(jobName));
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getMergedJobDataMap()).thenReturn(mergedDataMap);
        when(context.getScheduler()).thenReturn(scheduler);
        when(context.getFireInstanceId()).thenReturn("fire-" + jobName);

        return context;
    }

    private ScheduledThreadPoolExecutor getExecutor() throws Exception {
        java.lang.reflect.Field executorField =
                JobInterruptMonitorPlugin.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        return (ScheduledThreadPoolExecutor) executorField.get(plugin);
    }
}
