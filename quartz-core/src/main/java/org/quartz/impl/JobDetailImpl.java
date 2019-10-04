
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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.StatefulJob;
import org.quartz.Trigger;
import org.quartz.utils.ClassUtils;


/**
 * <p>
 * Conveys the detail properties of a given <code>Job</code> instance.
 * </p>
 * 
 * <p>
 * Quartz does not store an actual instance of a <code>Job</code> class, but
 * instead allows you to define an instance of one, through the use of a <code>JobDetail</code>.
 * </p>
 * 
 * <p>
 * <code>Job</code>s have a name and group associated with them, which
 * should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * 
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s
 * are scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * 
 * @see Job
 * @see StatefulJob
 * @see JobDataMap
 * @see Trigger
 * 
 * @author James House
 * @author Sharada Jambula
 */
@SuppressWarnings("deprecation")
public class JobDetailImpl implements Cloneable, java.io.Serializable, JobDetail {

    private static final long serialVersionUID = -6069784757781506897L;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private String name;

    private String group = Scheduler.DEFAULT_GROUP;

    private String description;

    private Class<? extends Job> jobClass;

    private JobDataMap jobDataMap;

    private boolean durability = false;

    private boolean shouldRecover = false;

    private transient JobKey key = null;

    /*
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    *
    * Constructors.
    *
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    */

    /**
     * <p>
     * Create a <code>JobDetail</code> with no specified name or group, and
     * the default settings of all the other properties.
     * </p>
     * 
     * <p>
     * Note that the {@link #setName(String)},{@link #setGroup(String)}and
     * {@link #setJobClass(Class)}methods must be called before the job can be
     * placed into a {@link Scheduler}
     * </p>
     */
    public JobDetailImpl() {
        // do nothing...
    }

    /**
     * <p>
     * Create a <code>JobDetail</code> with the given name, given class, default group, 
     * and the default settings of all the other properties.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     *              
     * @deprecated use {@link JobBuilder}              
     */
    public JobDetailImpl(String name, Class<? extends Job> jobClass) {
        this(name, null, jobClass);
    }

    /**
     * <p>
     * Create a <code>JobDetail</code> with the given name, group and class, 
     * and the default settings of all the other properties.
     * </p>
     * 
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     *              
     * @deprecated use {@link JobBuilder}              
     */
    public JobDetailImpl(String name, String group, Class<? extends Job> jobClass) {
        setName(name);
        setGroup(group);
        setJobClass(jobClass);
    }

    /**
     * <p>
     * Create a <code>JobDetail</code> with the given name, and group, and
     * the given settings of all the other properties.
     * </p>
     * 
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     *              
     * @deprecated use {@link JobBuilder}              
     */
    public JobDetailImpl(String name, String group, Class<? extends Job> jobClass,
                     boolean durability, boolean recover) {
        setName(name);
        setGroup(group);
        setJobClass(jobClass);
        setDurability(durability);
        setRequestsRecovery(recover);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the name of this <code>Job</code>.
     * </p>
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Set the name of this <code>Job</code>.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty.
     */
    public void setName(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Job name cannot be empty.");
        }

        this.name = name;
        this.key = null;
    }

    /**
     * <p>
     * Get the group of this <code>Job</code>.
     * </p>
     */
    public String getGroup() {
        return group;
    }

    /**
     * <p>
     * Set the group of this <code>Job</code>.
     * </p>
     * 
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if the group is an empty string.
     */
    public void setGroup(String group) {
        if (group != null && group.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Group name cannot be empty.");
        }

        if (group == null) {
            group = Scheduler.DEFAULT_GROUP;
        }

        this.group = group;
        this.key = null;
    }

    /**
     * <p>
     * Returns the 'full name' of the <code>JobDetail</code> in the format
     * "group.name".
     * </p>
     */
    public String getFullName() {
        return group + "." + name;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getKey()
     */
    public JobKey getKey() {
        if(key == null) {
            if(getName() == null)
                return null;
            key = new JobKey(getName(), getGroup());
        }

        return key;
    }
    
    public void setKey(JobKey key) {
        if(key == null)
            throw new IllegalArgumentException("Key cannot be null!");

        setName(key.getName());
        setGroup(key.getGroup());
        this.key = key;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Set a description for the <code>Job</code> instance - may be useful
     * for remembering/displaying the purpose of the job, though the
     * description has no meaning to Quartz.
     * </p>
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getJobClass()
     */
    public Class<? extends Job> getJobClass() {
        return jobClass;
    }

    /**
     * <p>
     * Set the instance of <code>Job</code> that will be executed.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if jobClass is null or the class is not a <code>Job</code>.
     */
    public void setJobClass(Class<? extends Job> jobClass) {
        if (jobClass == null) {
            throw new IllegalArgumentException("Job class cannot be null.");
        }

        if (!Job.class.isAssignableFrom(jobClass)) {
            throw new IllegalArgumentException(
                    "Job class must implement the Job interface.");
        }

        this.jobClass = jobClass;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getJobDataMap()
     */
    public JobDataMap getJobDataMap() {
        if (jobDataMap == null) {
            jobDataMap = new JobDataMap();
        }
        return jobDataMap;
    }

    /**
     * <p>
     * Set the <code>JobDataMap</code> to be associated with the <code>Job</code>.
     * </p>
     */
    public void setJobDataMap(JobDataMap jobDataMap) {
        this.jobDataMap = jobDataMap;
    }

    /**
     * <p>
     * Set whether or not the <code>Job</code> should remain stored after it
     * is orphaned (no <code>{@link Trigger}s</code> point to it).
     * </p>
     * 
     * <p>
     * If not explicitly set, the default value is <code>false</code>.
     * </p>
     */
    public void setDurability(boolean durability) {
        this.durability = durability;
    }

    /**
     * <p>
     * Set whether or not the the <code>Scheduler</code> should re-execute
     * the <code>Job</code> if a 'recovery' or 'fail-over' situation is
     * encountered.
     * </p>
     * 
     * <p>
     * If not explicitly set, the default value is <code>false</code>.
     * </p>
     * 
     * @see JobExecutionContext#isRecovering()
     */
    public void setRequestsRecovery(boolean shouldRecover) {
        this.shouldRecover = shouldRecover;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#isDurable()
     */
    public boolean isDurable() {
        return durability;
    }

    /**
     * @return whether the associated Job class carries the {@link PersistJobDataAfterExecution} annotation.
     */
    public boolean isPersistJobDataAfterExecution() {

        return ClassUtils.isAnnotationPresent(jobClass, PersistJobDataAfterExecution.class);
    }

    /**
     * @return whether the associated Job class carries the {@link DisallowConcurrentExecution} annotation.
     */
    public boolean isConcurrentExectionDisallowed() {
        
        return ClassUtils.isAnnotationPresent(jobClass, DisallowConcurrentExecution.class);
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#requestsRecovery()
     */
    public boolean requestsRecovery() {
        return shouldRecover;
    }

    /**
     * <p>
     * Return a simple string representation of this object.
     * </p>
     */
    @Override
    public String toString() {
        return "JobDetail '" + getFullName() + "':  jobClass: '"
                + ((getJobClass() == null) ? null : getJobClass().getName())
                + " concurrentExectionDisallowed: " + isConcurrentExectionDisallowed() 
                + " persistJobDataAfterExecution: " + isPersistJobDataAfterExecution() 
                + " isDurable: " + isDurable() + " requestsRecovers: " + requestsRecovery();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JobDetail)) {
            return false;
        }

        JobDetail other = (JobDetail) obj;

        if(other.getKey() == null || getKey() == null)
            return false;
        
        if (!other.getKey().equals(getKey())) {
            return false;
        }
            
        return true;
    }

    @Override
    public int hashCode() {
        JobKey key = getKey();
        return key == null ? 0 : getKey().hashCode();
    }
    
    @Override
    public Object clone() {
        JobDetailImpl copy;
        try {
            copy = (JobDetailImpl) super.clone();
            if (jobDataMap != null) {
                copy.jobDataMap = (JobDataMap) jobDataMap.clone();
            }
        } catch (CloneNotSupportedException ex) {
            throw new IncompatibleClassChangeError("Not Cloneable.");
        }

        return copy;
    }

    public JobBuilder getJobBuilder() {
        JobBuilder b = JobBuilder.newJob()
            .ofType(getJobClass())
            .requestRecovery(requestsRecovery())
            .storeDurably(isDurable())
            .usingJobData(getJobDataMap())
            .withDescription(getDescription())
            .withIdentity(getKey());
        return b;
    }
}
