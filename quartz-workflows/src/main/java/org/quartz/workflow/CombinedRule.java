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

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.quartz.SchedulerException;

public class CombinedRule implements WorkflowRule{
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    private final ArrayList<WorkflowRule> rules;

    public CombinedRule() {
        super();
        this.rules = new ArrayList<>();
    }
    
    
    @Override
    public String description() {
        return rules.stream().map(WorkflowRule::description).collect(Collectors.joining(" and "));
    }


    @Override
    public void apply(RuleParameters ruleParameters, Schedulers schedulers) throws SchedulerException {
        for(WorkflowRule rule : rules)
            rule.apply(ruleParameters, schedulers);
    }

    @Override
    public CombinedRule with(WorkflowRule rule) {
        rules.add(rule);
        return this;
    }
    
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    void reset() {
        rules.clear();
    }

    @Override
    public void verify(Schedulers schedulers) throws SchedulerException {
        for(WorkflowRule rule : rules)
            rule.verify(schedulers);
    }
}
