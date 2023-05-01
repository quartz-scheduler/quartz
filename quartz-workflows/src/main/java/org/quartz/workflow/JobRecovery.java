/*
 * Copyright 2023 Dimitry Polivaev, Unite
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quartz.workflow;

import java.util.Date;
import java.util.Objects;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JobRecovery {

    @FunctionalInterface
    public interface Factory {
        JobRecovery create(JobExecutionContext jobExecutionContext, int initialRecoveryAttempts, long delayMillis);
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(JobRecovery.class);

    private static String TRIGGER_GROUP = JobRecovery.class.getSimpleName();

    //** @VisibleForTesting
    static final String RECOVERY_ATTEMPTS = JobRecovery.class.getSimpleName() + ".recoveryAttempts";

    private final JobExecutionContext jobExecutionContext;
    private final int initialRecoveryAttempts;
    private final int remainingRecoveryAttempts;

    private final long delayMillis;


    public JobRecovery(JobExecutionContext jobExecutionContext,
            int initialRecoveryAttempts, long delayMillis) {
        this.jobExecutionContext = jobExecutionContext;
        this.initialRecoveryAttempts = initialRecoveryAttempts;
        JobDataMap triggerDataMap = jobExecutionContext.getTrigger().getJobDataMap();
        this.remainingRecoveryAttempts = triggerDataMap.containsKey(RECOVERY_ATTEMPTS) ? triggerDataMap.getInt(RECOVERY_ATTEMPTS) : initialRecoveryAttempts;
        this.delayMillis = delayMillis;
    }

    public void start()  throws AllRecoveryAttemptsFailedException, JobExecutionException {
        start(() -> {/**/});
    }

    public void start(Runnable reset)  throws AllRecoveryAttemptsFailedException, JobExecutionException {
        if(remainingRecoveryAttempts <= 0)
            throw new AllRecoveryAttemptsFailedException("Can not schedule recovery");
        try {
            reset.run();
            scheduleRecoveryJob();
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        }
    }

     private void scheduleRecoveryJob() throws SchedulerException {
        Date triggerStartTime = new Date(System.currentTimeMillis() + delayMillis);
        if(jobExecutionContext.getJobDetail().isConcurrentExectionDisallowed()) {
            sleepUntilStartTime();
        }

        Trigger trigger = jobExecutionContext.getTrigger();
        int attempt = initialRecoveryAttempts - remainingRecoveryAttempts + 1;
        boolean isFirstRecoveryAttempt = remainingRecoveryAttempts == initialRecoveryAttempts;
        int triggerPriority = trigger.getPriority() + (isFirstRecoveryAttempt ?  1 : 0);
        String triggerGroup = (isFirstRecoveryAttempt ? TRIGGER_GROUP : "")
                + trigger.getKey().getGroup() + "_" + attempt;
        @SuppressWarnings("unchecked")
        TriggerBuilder<Trigger> triggerBuilder = (TriggerBuilder<Trigger>) trigger.getTriggerBuilder();
        Trigger recoveryTrigger = triggerBuilder
        .withIdentity(trigger.getKey().getName(), triggerGroup)
            .startAt(triggerStartTime)
            .endAt(null)
            .withSchedule(
                    SimpleScheduleBuilder.simpleSchedule().
                    withMisfireHandlingInstructionIgnoreMisfires()
                )
            .withPriority(triggerPriority)
            .usingJobData(RECOVERY_ATTEMPTS, remainingRecoveryAttempts - 1).build();
        Objects.requireNonNull(recoveryTrigger);
        Scheduler scheduler = jobExecutionContext.getScheduler();
        scheduler.scheduleJob(recoveryTrigger);
        LOGGER.info("job recovery scheduled, trigger:" + recoveryTrigger.getKey() + ", attempt:" + attempt);
        jobExecutionContext.getMergedJobDataMap().remove(Workflow.WORKFLOW_RULE);
    }

    private void sleepUntilStartTime() {
        try {
            if(delayMillis > 0)
                Thread.sleep(delayMillis);
        } catch (InterruptedException ie) {
            LOGGER.warn("Job unexpectedly interrupted", ie);
        }
    }
}
