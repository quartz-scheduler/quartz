/*
 * Created on Jan 26, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;

/*
 * Created on Jan 26, 2021
 *
 * author Dimitry Polivaev
 */
public class Workflow {
    
    static final String WORKFLOW_RULE = WorkflowRule.class.getName();
    private final Map<String, Scheduler > schedulers;
    private final FollowingJobStarter starter;
    
    public Workflow(){
        super();
        this.schedulers = new ConcurrentHashMap<>();
        starter = new FollowingJobStarter(this::getScheduler);
    }

    private void register(Scheduler scheduler) throws SchedulerException {
        final String schedulerName = scheduler.getSchedulerName();
        ListenerManager listenerManager = scheduler.getListenerManager();
        if (null ==  this.schedulers.putIfAbsent(schedulerName, scheduler)) {
            if(listenerManager.getJobListener(starter.getName()) != null)
                throw new SchedulerException("Scheduler" + schedulerName + " is already registered");
            listenerManager.addJobListener(starter, EverythingMatcher.allJobs());
        }
    }
    
    public void remove() throws SchedulerException {
        for(Scheduler scheduler: schedulers.values()) {
            scheduler.getListenerManager().removeJobListener(starter.getName());
        }
        schedulers.clear();
    }
    
    private Scheduler getScheduler(String schedulerName) throws SchedulerException {
        final Scheduler scheduler = schedulers.get(schedulerName);
        if(scheduler == null)
            throw new SchedulerException("Unknown scheduler " +schedulerName);
        return scheduler;
    }

    public void addJob(Scheduler scheduler, JobDetail job, final WorkflowRule workflowRule) throws SchedulerException {
        final JobDataMap data = job.getJobDataMap();
        data.put(WORKFLOW_RULE, workflowRule);
        addJob(scheduler, job);
    }

    public void addJob(Scheduler scheduler, JobDetail job) throws SchedulerException {
        if(job.isDurable())
            throw new IllegalArgumentException("Durable jobs are not allowed in workflow");
        register(scheduler);
        scheduler.addJob(job, false, true);
    }
    
    public void startJobs(Scheduler scheduler, GroupMatcher<JobKey> jobMatcher) throws SchedulerException {
        new GroupRule(jobMatcher).apply(x -> scheduler);
    }

    public void startJob(Scheduler scheduler, JobKey job) throws SchedulerException {
        new SingleJobRule(job).apply(x -> scheduler);
    }

 }
