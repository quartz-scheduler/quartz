/*
 * Created on Jan 28, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class SingleJobDoneCondition implements WorkflowCondition {
    private static final long serialVersionUID = WorkflowCondition.serialVersionUID;
    private final String schedulerName;
    private final JobKey jobKey;

    public SingleJobDoneCondition(JobKey jobKey) {
        this(null, jobKey);
    }

    public SingleJobDoneCondition(String schedulerName, JobKey jobKey) {
        super();
        this.jobKey = jobKey;
        this.schedulerName = schedulerName;
    }

    @Override
    public boolean canStartJobs(Schedulers schedulers) throws SchedulerException {
        final Scheduler conditionScheduler = schedulers.byNameOrDefault(schedulerName);
        final boolean ready = ! conditionScheduler.checkExists(jobKey);
        return ready;
    }
}
