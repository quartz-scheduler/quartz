/*
 * Created on Jan 28, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.util.ArrayList;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public class CombinedRule implements WorkflowRule{
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    private final ArrayList<WorkflowRule> rules;

    public CombinedRule() {
        super();
        this.rules = new ArrayList<>();
    }

    @Override
    public void apply(Schedulers schedulers) throws SchedulerException {
        for(WorkflowRule rule : rules)
            rule.apply(schedulers);
    }

    public CombinedRule with(WorkflowRule rule) {
        rules.add(rule);
        return this;
    }

    public CombinedRule with(String schedulerName, GroupMatcher<JobKey> groupMatcher) {
       return with(new GroupRule(schedulerName, groupMatcher));
    }

    public CombinedRule with(String schedulerName, JobKey jobKey) {
        return with(new SingleJobRule(schedulerName, jobKey));
    }
}
