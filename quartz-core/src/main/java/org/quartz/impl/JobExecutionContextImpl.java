
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

package org.quartz.impl;

import java.util.Date;
import java.util.HashMap;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;


public class JobExecutionContextImpl implements java.io.Serializable, JobExecutionContext {

    private static final long serialVersionUID = -8139417614523942021L;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private transient Scheduler scheduler;

    private Trigger trigger;

    private JobDetail jobDetail;
    
    private JobDataMap jobDataMap;

    private transient Job job;
    
    private Calendar calendar;

    private boolean recovering = false;

    private int numRefires = 0;

    private Date fireTime;

    private Date scheduledFireTime;

    private Date prevFireTime;

    private Date nextFireTime;
    
    private long jobRunTime = -1;
    
    private Object result;
    
    private HashMap<Object, Object> data = new HashMap<Object, Object>();

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a JobExcecutionContext with the given context data.
     * </p>
     */
    public JobExecutionContextImpl(Scheduler scheduler,
            TriggerFiredBundle firedBundle, Job job) {
        this.scheduler = scheduler;
        this.trigger = firedBundle.getTrigger();
        this.calendar = firedBundle.getCalendar();
        this.jobDetail = firedBundle.getJobDetail();
        this.job = job;
        this.recovering = firedBundle.isRecovering();
        this.fireTime = firedBundle.getFireTime();
        this.scheduledFireTime = firedBundle.getScheduledFireTime();
        this.prevFireTime = firedBundle.getPrevFireTime();
        this.nextFireTime = firedBundle.getNextFireTime();
        
        this.jobDataMap = new JobDataMap();
        this.jobDataMap.putAll(jobDetail.getJobDataMap());
        this.jobDataMap.putAll(trigger.getJobDataMap());
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * {@inheritDoc}
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * {@inheritDoc}
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getCalendar() {
        return calendar;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRecovering() {
        return recovering;
    }

    public TriggerKey getRecoveringTriggerKey() {
        if (isRecovering()) {
            return new TriggerKey(jobDataMap.getString(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME),
                                  jobDataMap.getString(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_GROUP));
        } else {
            throw new IllegalStateException("Not a recovering job");
        }
    }
    
    public void incrementRefireCount() {
        numRefires++;
    }

    /**
     * {@inheritDoc}
     */
    public int getRefireCount() {
        return numRefires;
    }

    /**
     * {@inheritDoc}
     */
    public JobDataMap getMergedJobDataMap() {
        return jobDataMap;
    }

    /**
     * {@inheritDoc}
     */
    public JobDetail getJobDetail() {
        return jobDetail;
    }

    /**
     * {@inheritDoc}
     */
    public Job getJobInstance() {
        return job;
    }

    /**
     * {@inheritDoc}
     */
    public Date getFireTime() {
        return fireTime;
    }

    /**
     * {@inheritDoc}
     */
    public Date getScheduledFireTime() {
        return scheduledFireTime;
    }

    /**
     * {@inheritDoc}
     */
    public Date getPreviousFireTime() {
        return prevFireTime;
    }

    /**
     * {@inheritDoc}
     */
    public Date getNextFireTime() {
        return nextFireTime;
    }

    @Override
    public String toString() {
        return "JobExecutionContext:" + " trigger: '"
                + getTrigger().getKey() + " job: "
                + getJobDetail().getKey() + " fireTime: '" + getFireTime()
                + " scheduledFireTime: " + getScheduledFireTime()
                + " previousFireTime: '" + getPreviousFireTime()
                + " nextFireTime: " + getNextFireTime() + " isRecovering: "
                + isRecovering() + " refireCount: " + getRefireCount();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult() {
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setResult(Object result) {
        this.result = result;
    }
    
    /**
     * {@inheritDoc}
     */
    public long getJobRunTime() {
        return jobRunTime;
    }
    
    /**
     * @param jobRunTime The jobRunTime to set.
     */
    public void setJobRunTime(long jobRunTime) {
        this.jobRunTime = jobRunTime;
    }

    /**
     * {@inheritDoc}
     */
    public void put(Object key, Object value) {
        data.put(key, value);
    }
    
    /**
     * {@inheritDoc}
     */
    public Object get(Object key) {
        return data.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public String getFireInstanceId() {
        return ((OperableTrigger)trigger).getFireInstanceId();
    }
}
