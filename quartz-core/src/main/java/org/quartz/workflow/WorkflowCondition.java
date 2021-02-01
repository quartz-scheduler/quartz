/*
 * Created on Jan 28, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.io.Serializable;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public interface WorkflowCondition extends Serializable {
    long serialVersionUID = 2379672576808405507L;
    boolean canStartJobs(Schedulers schedulers) throws SchedulerException;
    static CombinedCondition with(String schedulerName, GroupMatcher<JobKey> groupMatcher) {
        return new CombinedCondition().with(schedulerName, groupMatcher);
    }
    static CombinedCondition with(String schedulerName, JobKey key) {
        return new CombinedCondition().with(schedulerName, key);
    }

}
