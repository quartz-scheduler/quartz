/*
 * Created on 30 Apr 2023
 *
 * author dimitry
 */
package org.quartz.workflow;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

class ScheduledJobSemaphore implements SchedulerListener {

    private final Semaphore semaphore = new Semaphore(0);

    private final AtomicInteger scheduledJobsCounter = new AtomicInteger(0);

     void waitUntilAllJobsAreUnscheduled() throws InterruptedException {
         assertTrue("jobs done", semaphore.tryAcquire(scheduledJobsCounter.getAndSet(0), 5, TimeUnit.SECONDS));
    }

    @Override
    public void triggersResumed(String triggerGroup) {/**/}

    @Override
    public void triggersPaused(String triggerGroup) {/**/}

    @Override
    public void triggerResumed(TriggerKey triggerKey) {/**/}

    @Override
    public void triggerPaused(TriggerKey triggerKey) {/**/}

    @Override
    public void triggerFinalized(Trigger trigger) {/**/}

    @Override
    public void schedulingDataCleared() {/**/}

    @Override
    public void schedulerStarting() {/**/}

    @Override
    public void schedulerStarted() {/**/}

    @Override
    public void schedulerShuttingdown() {/**/}

    @Override
    public void schedulerShutdown() {/**/}

    @Override
    public void schedulerInStandbyMode() {/**/}

    @Override
    public void schedulerError(String msg, SchedulerException cause) {/**/}

    @Override
    public void jobsResumed(String jobGroup) {/**/}

    @Override
    public void jobsPaused(String jobGroup) {/**/}

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
        semaphore.release();
    }

    @Override
    public void jobScheduled(Trigger trigger) {
        scheduledJobsCounter.incrementAndGet();
    }

    @Override
    public void jobResumed(JobKey jobKey) {/**/}

    @Override
    public void jobPaused(JobKey jobKey) {/**/}

    @Override
    public void jobDeleted(JobKey jobKey) {/**/}

    @Override
    public void jobAdded(JobDetail jobDetail) {/**/}
}