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

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerRule implements WorkflowRule {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(TriggerRule.class);
    
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    
    private final String schedulerName;
    private final Trigger followingTrigger;
    
    TriggerRule(Trigger followingTrigger) {
        this(null, followingTrigger);
    }
   
    TriggerRule(String schedulerName, Trigger followingTrigger) {
        this.schedulerName = schedulerName;
        this.followingTrigger = followingTrigger;
    }

    @Override
    public String description() {
        return "trigger";
    }
    
    
    @Override
    public void apply(RuleParameters ruleParameters, Schedulers schedulers) throws SchedulerException{
        final Scheduler scheduler = schedulers.byNameOrDefault(schedulerName);
        LOGGER.info("Job execution for job " + followingTrigger.getJobKey() 
        + " is scheduled after " + ruleParameters.getCause() + 
        " with trigger " +followingTrigger.getKey());
        ruleParameters.setInput(followingTrigger);
        scheduler.scheduleJob(followingTrigger);

    }

    @Override
    public void verify(Schedulers schedulers) throws SchedulerException {
        schedulers.byNameOrDefault(schedulerName);
    }
}