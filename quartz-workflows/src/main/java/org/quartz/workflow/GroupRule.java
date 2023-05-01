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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

public class GroupRule implements WorkflowRule{
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;

    private String schedulerName;
    private boolean onAllSchedulers;
    private final GroupMatcher<JobKey> followingJobMatcher;
    private int triggerPriority = Trigger.DEFAULT_PRIORITY;

    GroupRule(GroupMatcher<JobKey> followingJobMatcher) {
        this.schedulerName = null;
        this.followingJobMatcher = followingJobMatcher;
        this.onAllSchedulers = false;
    }
    
    public GroupRule onScheduler(String schedulerName) {
        this.schedulerName = schedulerName;
        this.onAllSchedulers = false;
        return this;
    }
    
    public GroupRule onAllSchedulers() {
        this.schedulerName = null;
        onAllSchedulers = true;
        return this;
    }
    
    public GroupRule onAllSchedulersExcept(String schedulerName) {
        this.schedulerName = schedulerName;
        onAllSchedulers = true;
        return this;
    }
    
    public GroupRule onSameScheduler() {
        this.schedulerName = null;
        this.onAllSchedulers = false;
        return this;
    }
    @Override
    public String description() {
        return "group " + followingJobMatcher.getCompareWithOperator() 
        + " " + followingJobMatcher.getCompareToValue();
    }
    

    
    public GroupRule setTriggerPriority(int priority) {
        this.triggerPriority = priority;
        return this;
    }

    @Override
    public void apply(RuleParameters ruleParameters, Schedulers schedulers) throws SchedulerException{
        startJobs(ruleParameters, schedulers, followingJobMatcher);
    }

    private void startJobs(RuleParameters ruleParameters, Schedulers schedulersByName, GroupMatcher<JobKey> jobMatcher) throws SchedulerException{
        final Collection<Scheduler> schedulers = onAllSchedulers ? WorkflowJobStarter.INSTANCE.allSchedulers()
                : Collections.singletonList(schedulersByName.byNameOrDefault(schedulerName));
        for(Scheduler scheduler: schedulers) {
            if(!onAllSchedulers || schedulerName == null || !schedulerName.equals(scheduler.getSchedulerName())) {
                final Set<JobKey> jobKeys = scheduler.getJobKeys(jobMatcher);
                for(JobKey key : jobKeys)
                    WorkflowJobStarter.startJob(scheduler, key, triggerPriority, 
                            ruleParameters.withCause("\"" + description()  + "\" after " + ruleParameters.getCause()));
            }
        }
    }

    @Override
    public void verify(Schedulers schedulers) throws SchedulerException {
        schedulers.byNameOrDefault(schedulerName);
    }
}