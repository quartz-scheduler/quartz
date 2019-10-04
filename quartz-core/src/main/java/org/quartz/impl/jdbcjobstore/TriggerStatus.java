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

import java.util.Date;

import org.quartz.JobKey;
import org.quartz.TriggerKey;


/**
 * <p>
 * Object representing a job or trigger key.
 * </p>
 * 
 * @author James House
 */
public class TriggerStatus {

    // FUTURE_TODO: Repackage under spi or root pkg ?, put status constants here.
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private TriggerKey key;

    private JobKey jobKey;

    private String status;
    
    private Date nextFireTime;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Construct a new TriggerStatus with the status name and nextFireTime.
     * 
     * @param status
     *          the trigger's status
     * @param nextFireTime
     *          the next time the trigger will fire
     */
    public TriggerStatus(String status, Date nextFireTime) {
        this.status = status;
        this.nextFireTime = nextFireTime;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public JobKey getJobKey() {
        return jobKey;
    }

    public void setJobKey(JobKey jobKey) {
        this.jobKey = jobKey;
    }

    public TriggerKey getKey() {
        return key;
    }

    public void setKey(TriggerKey key) {
        this.key = key;
    }

    /**
     * <p>
     * Get the name portion of the key.
     * </p>
     * 
     * @return the name
     */
    public String getStatus() {
        return status;
    }

    /**
     * <p>
     * Get the group portion of the key.
     * </p>
     * 
     * @return the group
     */
    public Date getNextFireTime() {
        return nextFireTime;
    }

    /**
     * <p>
     * Return the string representation of the TriggerStatus.
     * </p>
     *  
     */
    @Override
    public String toString() {
        return "status: " + getStatus() + ", next Fire = " + getNextFireTime();
    }
}

// EOF
