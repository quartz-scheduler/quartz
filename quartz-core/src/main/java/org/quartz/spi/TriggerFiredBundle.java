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

package org.quartz.spi;

import java.util.Date;

import org.quartz.Calendar;
import org.quartz.JobDetail;

/**
 * <p>
 * A simple class (structure) used for returning execution-time data from the
 * JobStore to the <code>QuartzSchedulerThread</code>.
 * </p>
 * 
 * @see org.quartz.core.QuartzSchedulerThread
 * 
 * @author James House
 */
public class TriggerFiredBundle implements java.io.Serializable {
  
    private static final long serialVersionUID = -6414106108306999265L;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private JobDetail job;

    private OperableTrigger trigger;

    private Calendar cal;

    private boolean jobIsRecovering;

    private Date fireTime;

    private Date scheduledFireTime;

    private Date prevFireTime;

    private Date nextFireTime;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public TriggerFiredBundle(JobDetail job, OperableTrigger trigger, Calendar cal,
            boolean jobIsRecovering, Date fireTime, Date scheduledFireTime,
            Date prevFireTime, Date nextFireTime) {
        this.job = job;
        this.trigger = trigger;
        this.cal = cal;
        this.jobIsRecovering = jobIsRecovering;
        this.fireTime = fireTime;
        this.scheduledFireTime = scheduledFireTime;
        this.prevFireTime = prevFireTime;
        this.nextFireTime = nextFireTime;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public JobDetail getJobDetail() {
        return job;
    }

    public OperableTrigger getTrigger() {
        return trigger;
    }

    public Calendar getCalendar() {
        return cal;
    }

    public boolean isRecovering() {
        return jobIsRecovering;
    }

    /**
     * @return Returns the fireTime.
     */
    public Date getFireTime() {
        return fireTime;
    }

    /**
     * @return Returns the nextFireTime.
     */
    public Date getNextFireTime() {
        return nextFireTime;
    }

    /**
     * @return Returns the prevFireTime.
     */
    public Date getPrevFireTime() {
        return prevFireTime;
    }

    /**
     * @return Returns the scheduledFireTime.
     */
    public Date getScheduledFireTime() {
        return scheduledFireTime;
    }

}