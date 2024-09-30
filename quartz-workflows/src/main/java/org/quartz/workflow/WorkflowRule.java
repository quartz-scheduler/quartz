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

import java.io.Serializable;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

public interface WorkflowRule extends Serializable {
    long serialVersionUID = 2379672576808405507L;
    
    String description();
    
    void apply(RuleParameters ruleParameters, Schedulers schedulers) throws SchedulerException;
    void verify(Schedulers schedulers) throws SchedulerException;
    
    static GroupRule with(GroupMatcher<JobKey> groupMatcher) {
        return new GroupRule(groupMatcher);
    }

    static JobRule with(JobKey jobKey) {
        return new JobRule(jobKey);
    }

    static JobRule with(JobDetail job) {
        return new JobRule(job.getKey());
    }

    static TriggerRule with(String schedulerName, Trigger trigger) {
        return new TriggerRule(schedulerName, trigger);
    }

    static TriggerRule with(Trigger trigger) {
        return new TriggerRule(trigger);
    }

    default CombinedRule with(WorkflowRule rule) {
        return new CombinedRule().with(this).with(rule);
    }

    default ConditionalRule when(WorkflowCondition condition) {
        return new ConditionalRule(condition, this);
    }

    static JobDoneCondition when(JobKey jobKey) {
        return new JobDoneCondition(jobKey);
    }

    static JobDoneCondition when(JobDetail job) {
        return when(job.getKey());
    }

    static GroupJobsDoneCondition when(GroupMatcher<JobKey> groupMatcher) {
        return new GroupJobsDoneCondition(groupMatcher);
    }
}