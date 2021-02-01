/*
 * Created on Jan 28, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

class JobStarter {
    static void startJob(final Scheduler scheduler, JobKey job, int triggerPriority)
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

    static void ignoreRaceConditions() {/**/}

}
