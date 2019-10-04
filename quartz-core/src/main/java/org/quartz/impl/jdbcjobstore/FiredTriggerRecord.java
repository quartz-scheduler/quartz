/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.impl.jdbcjobstore;

import org.quartz.JobKey;
import org.quartz.TriggerKey;

/**
 * <p>
 * Conveys the state of a fired-trigger record.
 * </p>
 * 
 * @author James House
 */
public class FiredTriggerRecord implements java.io.Serializable {

    private static final long serialVersionUID = -7183096398865657533L;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private String fireInstanceId;

    private long fireTimestamp;

    private long scheduleTimestamp;
    
    private String schedulerInstanceId;

    private TriggerKey triggerKey;

    private String fireInstanceState;

    private JobKey jobKey;

    private boolean jobDisallowsConcurrentExecution;

    private boolean jobRequestsRecovery;

    private int priority;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public String getFireInstanceId() {
        return fireInstanceId;
    }

    public long getFireTimestamp() {
        return fireTimestamp;
    }

    public long getScheduleTimestamp() {
        return scheduleTimestamp;
    }

    public boolean isJobDisallowsConcurrentExecution() {
        return jobDisallowsConcurrentExecution;
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public String getSchedulerInstanceId() {
        return schedulerInstanceId;
    }

    public TriggerKey getTriggerKey() {
        return triggerKey;
    }

    public String getFireInstanceState() {
        return fireInstanceState;
    }

    public void setFireInstanceId(String string) {
        fireInstanceId = string;
    }

    public void setFireTimestamp(long l) {
        fireTimestamp = l;
    }

    public void setScheduleTimestamp(long l) {
        scheduleTimestamp = l;
    }

    public void setJobDisallowsConcurrentExecution(boolean b) {
        jobDisallowsConcurrentExecution = b;
    }

    public void setJobKey(JobKey key) {
        jobKey = key;
    }

    public void setSchedulerInstanceId(String string) {
        schedulerInstanceId = string;
    }

    public void setTriggerKey(TriggerKey key) {
        triggerKey = key;
    }

    public void setFireInstanceState(String string) {
        fireInstanceState = string;
    }

    public boolean isJobRequestsRecovery() {
        return jobRequestsRecovery;
    }

    public void setJobRequestsRecovery(boolean b) {
        jobRequestsRecovery = b;
    }

    public int getPriority() {
        return priority;
    }
    

    public void setPriority(int priority) {
        this.priority = priority;
    }
    

}

// EOF
