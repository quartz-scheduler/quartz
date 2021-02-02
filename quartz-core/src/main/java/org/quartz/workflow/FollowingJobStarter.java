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
    
    private final Schedulers schedulers;
    

    public FollowingJobStarter(Schedulers schedulers) {
        super();
        this.schedulers = schedulers;
    }

    private static final String LISTENER_NAME = FollowingJobStarter.class.getName();
    
    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {/**/}

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {/**/}

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) throws SchedulerException {
        startFollowingJobs(context);
    }
    
    private void startFollowingJobs(JobExecutionContext context) throws SchedulerException {
        final JobDetail jobDetail = context.getJobDetail();
        final JobDataMap data = jobDetail.getJobDataMap();
        WorkflowRule rule = (WorkflowRule) data.get(Workflow.WORKFLOW_RULE);
        if(rule == null)
            return;
        final Scheduler scheduler = context.getScheduler();
        scheduler.deleteJob(jobDetail.getKey());
        rule.apply(schedulerName -> byNameOrDefault(schedulerName, scheduler));
    }

    private Scheduler byNameOrDefault(String schedulerName, final Scheduler fallback)
            throws SchedulerException {
        return schedulerName != null ? schedulers.byNameOrDefault(schedulerName) : fallback;
    }

}