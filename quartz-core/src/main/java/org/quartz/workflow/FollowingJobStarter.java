/*
 * Created on Jan 26, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

class FollowingJobStarter implements JobListener{
    
    static final String LISTENER_NAME = FollowingJobStarter.class.getName();
    static final FollowingJobStarter INSTANCE = new FollowingJobStarter();

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {/**/}

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {/**/}

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            startFollowingJobs(context);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void startFollowingJobs(JobExecutionContext context) throws SchedulerException {
        final JobDetail jobDetail = context.getJobDetail();
        final JobDataMap data = jobDetail.getJobDataMap();
        WorkflowRule followingJobStarter = (WorkflowRule) data.get(Workflow.FOLLOWING_JOB_STARTER);
        if(followingJobStarter == null)
            return;
        final Scheduler scheduler = context.getScheduler();
        scheduler.deleteJob(jobDetail.getKey());
        followingJobStarter.startJobsIfReady(scheduler);
    }
}