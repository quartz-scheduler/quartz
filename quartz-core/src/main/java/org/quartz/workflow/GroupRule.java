/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.util.Set;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public class GroupRule extends WorkflowRule{
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;

    private final GroupMatcher<JobKey> followingJobMatcher;

    public GroupRule(GroupMatcher<JobKey> currentJobGroupMatcher, GroupMatcher<JobKey> followingJobMatcher) {
        super(currentJobGroupMatcher);
        this.followingJobMatcher = followingJobMatcher;
    }
    
    public GroupRule(GroupMatcher<JobKey> followingJobMatcher) {
        super();
        this.followingJobMatcher = followingJobMatcher;
    }
    
    @Override
    void startJobs(Scheduler scheduler) throws SchedulerException{
        startJobs(scheduler, followingJobMatcher);
    }

    void startJobs(Scheduler scheduler, GroupMatcher<JobKey> jobMatcher) throws SchedulerException{
        final Set<JobKey> jobKeys = scheduler.getJobKeys(jobMatcher);
        for(JobKey key : jobKeys)
            startJob(scheduler, key);
    }
}