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

import org.quartz.SchedulerException;

public class ConditionalRule implements WorkflowRule {
    
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    private final WorkflowCondition condition;
    private final WorkflowRule rule;

    
    ConditionalRule(WorkflowCondition condition, WorkflowRule rule) {
        this.condition = condition;
        this.rule = rule;
    }
    
    @Override
    public String description() {
       return rule.description() + " because of " + condition.description();  
    }



    @Override
    public void apply(RuleParameters ruleParameters, Schedulers schedulers) throws SchedulerException{
        if(condition.canStartJobs(schedulers))
            rule.apply(ruleParameters, schedulers);
    }
    
    @Override
    public ConditionalRule when(WorkflowCondition condition) {
        return rule.when(this.condition.and(condition));
    }

    @Override
    public void verify(Schedulers schedulers) throws SchedulerException {
        rule.verify(schedulers);
    }


}