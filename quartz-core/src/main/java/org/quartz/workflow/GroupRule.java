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
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

public class GroupRule implements WorkflowRule{
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;

    private final String schedulerName;
    private final GroupMatcher<JobKey> followingJobMatcher;
    private int triggerPriority = Trigger.DEFAULT_PRIORITY;

    public GroupRule(GroupMatcher<JobKey> followingJobMatcher) {
        this(null, followingJobMatcher);
    }
    
    public GroupRule(String schedulerName, GroupMatcher<JobKey> followingJobMatcher) {
        this.schedulerName = schedulerName;
        this.followingJobMatcher = followingJobMatcher;
    }
    
    public GroupRule setTriggerPriority(int priority) {
        this.triggerPriority = priority;
        return this;
    }

   @Override
    public void apply(Schedulers schedulers) throws SchedulerException{
        startJobs(schedulers, followingJobMatcher);
    }

    void startJobs(Schedulers schedulers, GroupMatcher<JobKey> jobMatcher) throws SchedulerException{
        final Scheduler scheduler = schedulers.byNameOrDefault(schedulerName);
        final Set<JobKey> jobKeys = scheduler.getJobKeys(jobMatcher);
        for(JobKey key : jobKeys)
            JobStarter.startJob(scheduler, key, triggerPriority);
    }
}