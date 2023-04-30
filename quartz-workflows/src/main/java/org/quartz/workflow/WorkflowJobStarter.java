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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.JobPersistenceException;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.EverythingMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowJobStarter implements JobListener{
    private static final String SQL_ERROR_CLASS_INTEGRITY_CONSTRAINT_VIOLATION = "23";

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkflowJobStarter.class);

    private static String TRIGGER_GROUP = WorkflowJobStarter.class.getSimpleName();
    static void startJob(final Scheduler scheduler, JobKey job, int triggerPriority, RuleParameters ruleParameters)
            throws SchedulerException {
        try {
            Trigger trigger = JobAndTriggerFactory.newTrigger(job)
                    .withIdentity(job.getName(),
                TRIGGER_GROUP + "/" + job.getGroup())
                    .startAt(ruleParameters.getScheduledStartTime())
                    .withPriority(triggerPriority)
                    .build();
            ruleParameters.setInput(trigger);
            LOGGER.info("Job execution for job " + job
                    + " is scheduled by rule " + ruleParameters.getCause()
                    + " with trigger " + trigger.getKey());
            scheduler.scheduleJob(trigger);
        } catch (ObjectAlreadyExistsException e) {
            ignoreRaceConditions();
        } catch (JobPersistenceException e) {
            if(isJobAlreadyScheduled(e))
                ignoreRaceConditions();
            else if(scheduler.checkExists(job))
                throw e;
            else
                ignoreRaceConditions();
        }
    }

    private static boolean isJobAlreadyScheduled(JobPersistenceException e) {
        Throwable cause = e.getCause();
        return cause instanceof SQLException
                && ((SQLException)cause).getSQLState().startsWith(SQL_ERROR_CLASS_INTEGRITY_CONSTRAINT_VIOLATION);
    }

    private static void ignoreRaceConditions() {/**/}


    public final static WorkflowJobStarter INSTANCE = new WorkflowJobStarter();

    private final Map<String, Scheduler> schedulers;


    private WorkflowJobStarter() {
        schedulers = new ConcurrentHashMap<String, Scheduler>();
    }

    private static final String LISTENER_NAME = WorkflowJobStarter.class.getName();

    public void removeAllSchedulers() {
        schedulers.values().forEach(
                s -> {
                    try {
                        s.getListenerManager().removeJobListener(LISTENER_NAME);
                    } catch (SchedulerException e) {
                        throw new IllegalStateException(e);
                    }
                }
                );
        schedulers.clear();
    }

    public void removeScheduler(Scheduler scheduler) {
        try {
            final Scheduler removed = schedulers.remove(scheduler.getSchedulerName());
            if(removed != null) {
                removed.getListenerManager().removeJobListener(WorkflowJobStarter.LISTENER_NAME);
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
    }

    public void addScheduler(Scheduler scheduler) {
        try {
            schedulers.computeIfAbsent(scheduler.getSchedulerName(), x -> schedulerWithListener(scheduler));
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
    }

    private Scheduler schedulerWithListener(Scheduler scheduler){
        try {
            ListenerManager listenerManager = scheduler.getListenerManager();
            if(listenerManager.getJobListener(WorkflowJobStarter.LISTENER_NAME) != null)
                throw new SchedulerException("Scheduler " + scheduler.getSchedulerName() + " is already registered");
            listenerManager.addJobListener(this, EverythingMatcher.allJobs());
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
        return scheduler;
    }

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        LOGGER.info("Job execution is starting, trigger " + context.getTrigger().getKey());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        LOGGER.info("Job execution is vetoed, trigger " + context.getTrigger().getKey());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) throws SchedulerException {
        try {
            unscheduleFinishedJob(context);
            scheduleFollowingJobs(context);
        }
        finally {
            LOGGER.info("Job execution has finished, trigger " + context.getTrigger().getKey());
        }
    }

    public void scheduleJob(final String schedulerName, JobKey job, String cause, int triggerPriority) throws SchedulerException {
        final Scheduler scheduler = byNameOrDefault(schedulerName, null);
        WorkflowJobStarter.startJob(scheduler, job, triggerPriority, RuleParameters.startParameters(cause));
    }

    private void unscheduleFinishedJob(JobExecutionContext context) throws SchedulerException {
        final Trigger trigger = context.getTrigger();
        if(! trigger.mayFireAgain()) {
            final Scheduler scheduler = context.getScheduler();
            scheduler.unscheduleJob(trigger.getKey());
        }
    }

    private void scheduleFollowingJobs(JobExecutionContext context) throws SchedulerException {
        final JobDataMap data = context.getMergedJobDataMap();
        WorkflowRule rule = (WorkflowRule) data.get(Workflow.WORKFLOW_RULE);
        if(rule != null) {
            final Trigger trigger = context.getTrigger();
            if(! trigger.mayFireAgain()) {
                RuleParameters ruleParameters = RuleParameters.from(context);
                final Scheduler scheduler = context.getScheduler();
                rule.apply(ruleParameters, schedulerName -> byNameOrDefault(schedulerName, scheduler));
            }
        }
    }

    private Scheduler byNameOrDefault(String schedulerName, final Scheduler fallback) throws SchedulerException{
        if(schedulerName == null)
            return fallback;
        final Scheduler scheduler = schedulers.get(schedulerName);
        if(scheduler == null)
            throw new SchedulerException("Unknown scheduler " + schedulerName);
        return scheduler;
    }

    void verify(WorkflowRule rule) throws SchedulerException {
        rule.verify(schedulerName -> byNameOrDefault(schedulerName, null));
    }

    public Collection<Scheduler> allSchedulers() {
        return schedulers.values();
    }

}