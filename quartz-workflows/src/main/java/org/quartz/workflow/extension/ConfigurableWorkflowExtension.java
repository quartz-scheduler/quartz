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

package org.quartz.workflow.extension;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.workflow.GroupRule;
import org.quartz.workflow.Workflow;
import org.quartz.workflow.WorkflowRule;

public class ConfigurableWorkflowExtension implements WorkflowExtender{
    private final Workflow workflow;
    private final String uniqueExtensionId;

    
    public ConfigurableWorkflowExtension(Workflow workflow, String uniqueExtensionId) {
        super();
        this.workflow = workflow;
        this.uniqueExtensionId = uniqueExtensionId;
    }

    public void scheduleAfter(Trigger trigger) throws SchedulerException {
        workflow.addRule(trigger, followingJobsRule());
    }

    public void scheduleAfter(JobDetail job) throws SchedulerException {
        workflow.addRule(job, followingJobsRule());
    }

    public void scheduleAtWorkflowStart() throws SchedulerException {
        workflow.addStartRule(followingJobsRule());
    }
    
    @Override
    public JobDetail addSynchronizationJob(String label) throws SchedulerException {
        JobDetail job = createExtensionJob(label);
        workflow.addJob(job);
        return job;
    }
    
    @Override
    public JobDetail addSynchronizationJob(String label, Scheduler scheduler) throws SchedulerException {
        JobDetail job = createExtensionJob(label);
        workflow.addJob(scheduler, job);
        return job;
    }

    private JobDetail createExtensionJob(String label) {
        JobDetail job = SynchronizationJob.newJob(label, firedJobGroup()).build();
        return job;
    }

    @Override
    public void addExtensionJob(String label, Scheduler scheduler, JobDetail followingJob) throws SchedulerException {
        JobDetail extension = addSynchronizationJob(label, scheduler);
        workflow.addJob(scheduler, followingJob);
        workflow.addRule(extension, WorkflowRule.with(followingJob.getKey()));
    }

    @Override
    public void addExtensionJob(String label, JobDetail followingJob) throws SchedulerException {
        JobDetail extension = addSynchronizationJob(label);
        workflow.addJob(followingJob);
        workflow.addRule(extension, WorkflowRule.with(followingJob.getKey()));
    }

    private GroupRule followingJobsRule() {
        return WorkflowRule.with(GroupMatcher.jobGroupEquals(firedJobGroup()))
                .onAllSchedulers()
                .setTriggerPriority(100);
    }
    
    private String firedJobGroup() {
        return "WorkflowExtension/" + uniqueExtensionId;
    }

}
