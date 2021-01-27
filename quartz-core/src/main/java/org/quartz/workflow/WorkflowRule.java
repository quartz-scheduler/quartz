/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.io.Serializable;

import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

abstract public class WorkflowRule implements Serializable{
    static final long serialVersionUID = 2379672576808405507L;
    final static String TRIGGER_GROUP_UUID = "/6ec1329e-d962-4dc1-aefa-c8eff7fa9165";

    private final GroupMatcher<JobKey> currentJobGroupMatcher;
    private int triggerPriority = Trigger.DEFAULT_PRIORITY;
    
    WorkflowRule() {
        this(null);
    }
    WorkflowRule(GroupMatcher<JobKey> currentJobGroupMatcher) {
        super();
        this.currentJobGroupMatcher = currentJobGroupMatcher;
    }

    abstract void startJobs(Scheduler scheduler) throws SchedulerException;

    public WorkflowRule setTriggerPriority(int priority) {
        this.triggerPriority = priority;
        return this;
    }
    
    void startJobsIfReady(Scheduler scheduler) throws SchedulerException {
        final boolean ready = currentJobGroupMatcher == null || scheduler.getJobKeys(currentJobGroupMatcher).isEmpty();
        if(ready)
            startJobs(scheduler);
    }


    void startJob(final Scheduler scheduler, JobKey job)
            throws SchedulerException {
        TriggerKey triggerKey = new TriggerKey(job.getName(), 
                job.getGroup() + WorkflowRule.TRIGGER_GROUP_UUID);
        try {
            scheduler.scheduleJob(TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withPriority(triggerPriority)
                    .forJob(job).startNow().build());
        } catch (ObjectAlreadyExistsException e) {
            ignoreRaceConditions();
        } catch (JobPersistenceException e) {
            if(scheduler.checkExists(job))
                throw e;
            else
                ignoreRaceConditions();
        }
    }

    private void ignoreRaceConditions() {/**/}
}