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

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class JobRule implements WorkflowRule {
    
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    
    private String schedulerName;
    private final JobKey followingJobKey;
    private int triggerPriority = Trigger.DEFAULT_PRIORITY;
    
    JobRule(JobKey followingJobKey) {
        this.schedulerName = null;
        this.followingJobKey = followingJobKey;
    }
    
    public JobRule onScheduler(String schedulerName) {
        this.schedulerName = schedulerName;
        return this;
    }
    
    @Override
    public String description() {
        return followingJobKey.toString();
    }
    
    public JobRule setTriggerPriority(int priority) {
        this.triggerPriority = priority;
        return this;
    }

    @Override
    public void apply(RuleParameters ruleParameters, Schedulers schedulers) throws SchedulerException{
        final Scheduler scheduler = schedulers.byNameOrDefault(schedulerName);
        WorkflowJobStarter.startJob(scheduler, followingJobKey, triggerPriority, 
                ruleParameters.withCause(followingJobKey + " after " + ruleParameters.getCause()));
    }

    @Override
    public void verify(Schedulers schedulers) throws SchedulerException {
        schedulers.byNameOrDefault(schedulerName);
    }
}