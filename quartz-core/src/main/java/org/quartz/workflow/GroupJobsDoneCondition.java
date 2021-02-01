/*
 * Created on Jan 28, 2021
 *
 * author dimitry
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

    public GroupJobsDoneCondition(GroupMatcher<JobKey> currentJobGroupMatcher) {
        this(null, currentJobGroupMatcher);
    }

    public GroupJobsDoneCondition(String schedulerName, GroupMatcher<JobKey> currentJobGroupMatcher) {
        super();
        this.currentJobGroupMatcher = currentJobGroupMatcher;
        this.schedulerName = schedulerName;
    }

    @Override
    public boolean canStartJobs(Schedulers schedulers) throws SchedulerException {
        final Scheduler conditionScheduler = schedulers.byNameOrDefault(schedulerName);
        final boolean ready = conditionScheduler.getJobKeys(currentJobGroupMatcher).isEmpty();
        return ready;
    }
}
