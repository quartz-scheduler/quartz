/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public class ConditionalRule implements WorkflowRule {
    
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    private final WorkflowCondition condition;
    private final WorkflowRule rule;

    
    public ConditionalRule(WorkflowCondition condition, WorkflowRule rule) {
        this.condition = condition;
        this.rule = rule;
    }
     
    public ConditionalRule(GroupMatcher<JobKey> currentJobGroupMatcher,JobKey followingJobKey) {
        this(new GroupJobsDoneCondition(currentJobGroupMatcher), new SingleJobRule(followingJobKey));
    }
     
    public ConditionalRule(GroupMatcher<JobKey> currentJobGroupMatcher,GroupMatcher<JobKey> followingJobMatcher) {
        this(new GroupJobsDoneCondition(currentJobGroupMatcher), new GroupRule(followingJobMatcher));
    }
     
    @Override
    public void apply(Schedulers schedulers) throws SchedulerException{
        if(condition.canStartJobs(schedulers))
            rule.apply(schedulers);
    }

}