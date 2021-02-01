/*
 * Created on Jan 28, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public class CombinedCondition implements WorkflowCondition{
    private static final long serialVersionUID = WorkflowCondition.serialVersionUID;
    private final List<WorkflowCondition> conditions;

    public CombinedCondition() {
        conditions = new ArrayList<>();
    }
    
    @Override
    public boolean canStartJobs(Schedulers schedulers) throws SchedulerException {
        for(WorkflowCondition condition : conditions)
            if(! condition.canStartJobs(schedulers))
                return false;
        return true;
    }

    public CombinedCondition with(String schedulerName, GroupMatcher<JobKey> groupMatcher) {
        return with(new GroupJobsDoneCondition(schedulerName, groupMatcher));
    }


    public CombinedCondition with(String schedulerName, JobKey jobKey) {
        return with (new SingleJobDoneCondition(schedulerName, jobKey));
    }
    
    public CombinedCondition with(WorkflowCondition condition) {
        conditions.add(condition);
        return this;
    }
}
