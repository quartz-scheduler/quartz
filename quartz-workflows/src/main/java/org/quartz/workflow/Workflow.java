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

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/*
 * Created on Jan 26, 2021
 *
 * author Dimitry Polivaev
 */
public class Workflow {

    static final String WORKFLOW_RULE = WorkflowRule.class.getName();
    private final AllScheduledJobs schedulers;
    private final CombinedRule startRule;

    public Workflow(){
        super();
        this.schedulers = new AllScheduledJobs();
        startRule = new CombinedRule();
    }

    public Workflow addJob(Scheduler scheduler, JobDetail job) throws SchedulerException {
        schedulers.register(scheduler, job);
        return this;
    }

    public Workflow addJob(JobDetail job) throws SchedulerException {
        schedulers.register(job);
        return this;
    }

    public Workflow addScheduler(Scheduler scheduler) throws SchedulerException {
        schedulers.register(scheduler);
        return this;
    }

    public Workflow setDefaultScheduler(Scheduler scheduler) throws SchedulerException {
        schedulers.setDefaultScheduler(scheduler);
        return this;
    }

    public Workflow addRule(Trigger trigger, WorkflowRule rule) throws SchedulerException {
        WorkflowJobStarter.INSTANCE.verify(rule);
        final JobDataMap data = trigger.getJobDataMap();
        addRule(data, rule);
        return this;
    }

    public Workflow addRule(JobDetail job, final WorkflowRule rule) throws SchedulerException {
        WorkflowJobStarter.INSTANCE.verify(rule);
        final JobDataMap data = job.getJobDataMap();
        addRule(data, rule);
        return this;
    }

    private void addRule(final JobDataMap data, WorkflowRule rule) {
        if(null == data.computeIfPresent(WORKFLOW_RULE, (key, oldRule) -> ((WorkflowRule)oldRule).with(rule)))
            data.put(WORKFLOW_RULE, rule);
    }
    
    public Workflow addStartRule(final WorkflowRule rule) throws SchedulerException {
        WorkflowJobStarter.INSTANCE.verify(rule);
        startRule.with(rule);
        return this;
    }

    public Workflow addStartRule(final Runnable runnable) {
        startRule.with(new StartRule(runnable));
        return this;
    }

    public void start() throws SchedulerException {
        try {
            schedulers.storeJobs();
            schedulers.apply(startRule);
            schedulers.commit();
            startRule.reset();
        } catch (SchedulerException|RuntimeException e) {
            schedulers.rollback();
            throw e;
        }
    }
 }
