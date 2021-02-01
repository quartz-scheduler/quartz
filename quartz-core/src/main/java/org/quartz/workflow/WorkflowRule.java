/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import java.io.Serializable;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public interface WorkflowRule extends Serializable {
    long serialVersionUID = 2379672576808405507L;
    String TRIGGER_GROUP_UUID = "/6ec1329e-d962-4dc1-aefa-c8eff7fa9165";
    void apply(Schedulers schedulers) throws SchedulerException;
    static CombinedRule with(String schedulerName, GroupMatcher<JobKey> groupMatcher) {
        return new CombinedRule().with(schedulerName, groupMatcher);
    }
    static CombinedRule with(String schedulerName, JobKey jobKey) {
        return new CombinedRule().with(schedulerName, jobKey);
    }
}