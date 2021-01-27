/*
 * Created on Jan 26, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;

public class Workflow {
    
    static final String FOLLOWING_JOB_STARTER = FollowingJobStarter.class.getName();
    
    private final Scheduler scheduler;

    public Workflow(Scheduler scheduler){
        super();
        this.scheduler = scheduler;
        try {
            ListenerManager listenerManager = scheduler.getListenerManager();
            if (listenerManager.getJobListener(FollowingJobStarter.LISTENER_NAME) == null)
                listenerManager.addJobListener(FollowingJobStarter.INSTANCE, EverythingMatcher.allJobs());
        } catch (SchedulerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void addJob(JobDetail job, final WorkflowRule workflowRule) throws SchedulerException {
        final JobDataMap data = job.getJobDataMap();
        data.put(FOLLOWING_JOB_STARTER, workflowRule);
        addJob(job);
    }

    public void addJob(JobDetail job) throws SchedulerException {
        if(job.isDurable())
            throw new IllegalArgumentException("Durable jobs are not allowed in workflow");
        scheduler.addJob(job, false, true);
    }
    
    public void startJobs(GroupMatcher<JobKey> jobMatcher) throws SchedulerException {
        new GroupRule(jobMatcher).startJobs(scheduler);
    }

    public void startJob(JobKey job) throws SchedulerException {
        new SingleJobRule(job).startJobs(scheduler);
    }

 }
