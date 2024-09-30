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
import org.quartz.impl.matchers.GroupMatcher;

public class GroupJobsDoneCondition implements WorkflowCondition {
    private static final long serialVersionUID = WorkflowCondition.serialVersionUID;
    private final String schedulerName;
    private final GroupMatcher<JobKey> currentJobGroupMatcher;

    GroupJobsDoneCondition(GroupMatcher<JobKey> currentJobGroupMatcher) {
        this(null, currentJobGroupMatcher);
    }

    private GroupJobsDoneCondition(String schedulerName, GroupMatcher<JobKey> currentJobGroupMatcher) {
        super();
        this.currentJobGroupMatcher = currentJobGroupMatcher;
        this.schedulerName = schedulerName;
    }
    
    public GroupJobsDoneCondition onScheduler(String schedulerName) {
        return new GroupJobsDoneCondition(schedulerName, currentJobGroupMatcher);
    }

    @Override
    public String description() {
        return  "group " 
                + currentJobGroupMatcher.getCompareWithOperator() 
                + " " + currentJobGroupMatcher.getCompareToValue() + " is done";
    }

    @Override
    public boolean canStartJobs(Schedulers schedulers) throws SchedulerException {
        final Scheduler conditionScheduler = schedulers.byNameOrDefault(schedulerName);
        final boolean ready = conditionScheduler.getJobKeys(currentJobGroupMatcher).isEmpty();
        return ready;
    }
}
