/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class SingleJobRule implements WorkflowRule {
    
    private static final long serialVersionUID = WorkflowRule.serialVersionUID;
    
    private final String schedulerName;
    private final JobKey followingJobKey;
    private int triggerPriority = Trigger.DEFAULT_PRIORITY;
    
    public SingleJobRule(JobKey followingJobKey) {
        this(null, followingJobKey);
    }
   
    public SingleJobRule(String schedulerName, JobKey followingJobKey) {
        this.schedulerName = schedulerName;
        this.followingJobKey = followingJobKey;
    }
    
    public SingleJobRule setTriggerPriority(int priority) {
        this.triggerPriority = priority;
        return this;
    }

    @Override
    public void apply(Schedulers schedulers) throws SchedulerException{
        final Scheduler scheduler = schedulers.byNameOrDefault(schedulerName);
        JobStarter.startJob(scheduler, followingJobKey, triggerPriority);
    }
}