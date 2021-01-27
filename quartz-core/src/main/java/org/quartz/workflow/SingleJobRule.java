/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public class SingleJobRule extends WorkflowRule {
    
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    
    private final JobKey followingJobKey;
    
    public SingleJobRule(GroupMatcher<JobKey> currentJobGroupMatcher,JobKey followingJobKey) {
        super(currentJobGroupMatcher);
        this.followingJobKey = followingJobKey;
    }
    
    public SingleJobRule(JobKey followingJobKey) {
        super();
        this.followingJobKey = followingJobKey;
    }
     
    @Override
    void startJobs(Scheduler scheduler) throws SchedulerException{
        startJob(scheduler, followingJobKey);
    }
}