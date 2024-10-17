
/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

package org.quartz.impl.triggers;

import org.quartz.Calendar;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;


/**
 * <p>
 * The base abstract class to be extended by all <code>Trigger</code>s.
 * </p>
 * 
 * <p>
 * <code>Triggers</code> s have a name and group associated with them, which
 * should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * 
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code> s
 * are scheduled. Many <code>Trigger</code> s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * 
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents
 * into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @author James House
 * @author Sharada Jambula
 */
public abstract class AbstractTrigger<T extends Trigger> implements OperableTrigger {

    private static final long serialVersionUID = -3904243490805975570L;

    /*
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    *
    * Data members.
    *
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    */

    private String name;

    private String group = Scheduler.DEFAULT_GROUP;

    private String jobName;

    private String jobGroup = Scheduler.DEFAULT_GROUP;

    private String description;

    private JobDataMap jobDataMap;

    @SuppressWarnings("unused")
    private static final boolean VOLATILITY = false; // still here for serialization backward compatibility

    private String calendarName = null;

    private String fireInstanceId = null;

    private int misfireInstruction = MISFIRE_INSTRUCTION_SMART_POLICY;

    private int priority = DEFAULT_PRIORITY;

    private transient TriggerKey key = null;

    /*
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    *
    * Constructors.
    *
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    */



    /**
     * <p>
     * Create a <code>Trigger</code> with no specified name, group, or <code>{@link org.quartz.JobDetail}</code>.
     * </p>
     * 
     * <p>
     * Note that the {@link #setName(String)},{@link #setGroup(String)}and
     * the {@link #setJobName(String)}and {@link #setJobGroup(String)}methods
     * must be called before the <code>Trigger</code> can be placed into a
     * {@link Scheduler}.
     * </p>
     */
    protected AbstractTrigger() {
        // do nothing...
    }

    /**
     * <p>
     * Create a <code>Trigger</code> with the given name, and default group.
     * </p>
     * 
     * <p>
     * Note that the {@link #setJobName(String)}and
     * {@link #setJobGroup(String)}methods must be called before the <code>Trigger</code>
     * can be placed into a {@link Scheduler}.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     */
    protected AbstractTrigger(String name) {
        setName(name);
        setGroup(null);
    }
    
    /**
     * <p>
     * Create a <code>Trigger</code> with the given name, and group.
     * </p>
     * 
     * <p>
     * Note that the {@link #setJobName(String)}and
     * {@link #setJobGroup(String)}methods must be called before the <code>Trigger</code>
     * can be placed into a {@link Scheduler}.
     * </p>
     * 
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     */
    protected AbstractTrigger(String name, String group) {
        setName(name);
        setGroup(group);
    }

    /**
     * <p>
     * Create a <code>Trigger</code> with the given name, and group.
     * </p>
     * 
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     */
    protected AbstractTrigger(String name, String group, String jobName, String jobGroup) {
        setName(name);
        setGroup(group);
        setJobName(jobName);
        setJobGroup(jobGroup);
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
     * Get the name of this <code>Trigger</code>.
     * </p>
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Set the name of this <code>Trigger</code>.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty.
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Trigger name cannot be null or empty.");
        }

        this.name = name;
        this.key = null;
    }

    /**
     * <p>
     * Get the group of this <code>Trigger</code>.
     * </p>
     */
    public String getGroup() {
        return group;
    }

    /**
     * <p>
     * Set the name of this <code>Trigger</code>. 
     * </p>
     * 
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if group is an empty string.
     */
    public void setGroup(String group) {
        if (group != null && group.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Group name cannot be an empty string.");
        }

        if(group == null) {
            group = Scheduler.DEFAULT_GROUP;
        }

        this.group = group;
        this.key = null;
    }

    public void setKey(TriggerKey key) {
        setName(key.getName());
        setGroup(key.getGroup());
        this.key = key;
    }

    /**
     * <p>
     * Get the name of the associated <code>{@link org.quartz.JobDetail}</code>.
     * </p>
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * <p>
     * Set the name of the associated <code>{@link org.quartz.JobDetail}</code>.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if jobName is null or empty.
     */
    public void setJobName(String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Job name cannot be null or empty.");
        }

        this.jobName = jobName;
    }

    /**
     * <p>
     * Get the name of the associated <code>{@link org.quartz.JobDetail}</code>'s
     * group.
     * </p>
     */
    public String getJobGroup() {
        return jobGroup;
    }

    /**
     * <p>
     * Set the name of the associated <code>{@link org.quartz.JobDetail}</code>'s
     * group.
     * </p>
     * 
     * @param jobGroup if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * 
     * @exception IllegalArgumentException
     *              if group is an empty string.
     */
    public void setJobGroup(String jobGroup) {
        if (jobGroup != null && jobGroup.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Group name cannot be null or empty.");
        }

        if(jobGroup == null) {
            jobGroup = Scheduler.DEFAULT_GROUP;
        }

        this.jobGroup = jobGroup;
    }

    public void setJobKey(JobKey key) {
        setJobName(key.getName());
        setJobGroup(key.getGroup());
    }


    /**
     * <p>
     * Returns the 'full name' of the <code>Trigger</code> in the format
     * "group.name".
     * </p>
     */
    public String getFullName() {
        return group + "." + name;
    }

    public TriggerKey getKey() {
        if(key == null) {
            if(getName() == null)
                return null;
            key = new TriggerKey(getName(), getGroup());
        }

        return key;
    }

    public JobKey getJobKey() {
        if(getJobName() == null)
            return null;

        return new JobKey(getJobName(), getJobGroup());
    }

    /**
     * <p>
     * Returns the 'full name' of the <code>Job</code> that the <code>Trigger</code>
     * points to, in the format "group.name".
     * </p>
     */
    public String getFullJobName() {
        return jobGroup + "." + jobName;
    }

    /**
     * <p>
     * Return the description given to the <code>Trigger</code> instance by
     * its creator (if any).
     * </p>
     * 
     * @return null if no description was set.
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Set a description for the <code>Trigger</code> instance - may be
     * useful for remembering/displaying the purpose of the trigger, though the
     * description has no meaning to Quartz.
     * </p>
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>
     * Associate the <code>{@link Calendar}</code> with the given name with
     * this Trigger.
     * </p>
     * 
     * @param calendarName
     *          use <code>null</code> to dis-associate a Calendar.
     */
    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    /**
     * <p>
     * Get the name of the <code>{@link Calendar}</code> associated with this
     * Trigger.
     * </p>
     * 
     * @return <code>null</code> if there is no associated Calendar.
     */
    public String getCalendarName() {
        return calendarName;
    }

    /**
     * <p>
     * Get the <code>JobDataMap</code> that is associated with the 
     * <code>Trigger</code>.
     * </p>
     * 
     * <p>
     * Changes made to this map during job execution are not re-persisted, and
     * in fact typically result in an <code>IllegalStateException</code>.
     * </p>
     */
    public JobDataMap getJobDataMap() {
        if (jobDataMap == null) {
            jobDataMap = new JobDataMap();
        }
        return jobDataMap;
    }


    /**
     * <p>
     * Set the <code>JobDataMap</code> to be associated with the 
     * <code>Trigger</code>.
     * </p>
     */
    public void setJobDataMap(JobDataMap jobDataMap) {
        this.jobDataMap = jobDataMap;
    }

    /**
     * The priority of a <code>Trigger</code> acts as a tiebreaker such that if 
     * two <code>Trigger</code>s have the same scheduled fire time, then the
     * one with the higher priority will get first access to a worker
     * thread.
     * 
     * <p>
     * If not explicitly set, the default value is <code>5</code>.
     * </p>
     * 
     * @see #DEFAULT_PRIORITY
     */
    public int getPriority() {
        return priority;
    }


    /**
     * The priority of a <code>Trigger</code> acts as a tie breaker such that if 
     * two <code>Trigger</code>s have the same scheduled fire time, then Quartz
     * will do its best to give the one with the higher priority first access 
     * to a worker thread.
     * 
     * <p>
     * If not explicitly set, the default value is <code>5</code>.
     * </p>
     * 
     * @see #DEFAULT_PRIORITY
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }


    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     * 
     * <p>
     * Called after the <code>{@link Scheduler}</code> has executed the
     * <code>{@link org.quartz.JobDetail}</code> associated with the <code>Trigger</code>
     * in order to get the final instruction code from the trigger.
     * </p>
     * 
     * @param context
     *          is the <code>JobExecutionContext</code> that was used by the
     *          <code>Job</code>'s<code>execute(xx)</code> method.
     * @param result
     *          is the <code>JobExecutionException</code> thrown by the
     *          <code>Job</code>, if any (may be null).
     * @return one of the CompletedExecutionInstruction constants.
     * 
     * @see org.quartz.Trigger.CompletedExecutionInstruction
     * @see #triggered(Calendar)
     */
    public CompletedExecutionInstruction executionComplete(JobExecutionContext context,
                                          JobExecutionException result)
    {
        if (result != null && result.refireImmediately()) {
            return CompletedExecutionInstruction.RE_EXECUTE_JOB;
        }
    
        if (result != null && result.unscheduleFiringTrigger()) {
            return CompletedExecutionInstruction.SET_TRIGGER_COMPLETE;
        }
    
        if (result != null && result.unscheduleAllTriggers()) {
            return CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE;
        }
    
        if (!mayFireAgain()) {
            return CompletedExecutionInstruction.DELETE_TRIGGER;
        }
    
        return CompletedExecutionInstruction.NOOP;
    }



    /**
     * <p>
     * Set the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be passed to this method.
     * </p>
     * 
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * </p>
     * 
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see #updateAfterMisfire(Calendar)
     * @see SimpleTrigger
     * @see CronTrigger
     */
    public void setMisfireInstruction(int misfireInstruction) {
        if (!validateMisfireInstruction(misfireInstruction)) {
            throw new IllegalArgumentException(
                        "The misfire instruction code is invalid for this type of trigger.");
        }
        this.misfireInstruction = misfireInstruction;
    }

    protected abstract boolean validateMisfireInstruction(int candidateMisfireInstruction);

    /**
     * <p>
     * Get the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be passed to this method.
     * </p>
     * 
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * </p>
     * 
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see #updateAfterMisfire(Calendar)
     * @see SimpleTrigger
     * @see CronTrigger
     */
    public int getMisfireInstruction() {
        return misfireInstruction;
    }

    /**
     * <p>
     * Validates whether the properties of the <code>JobDetail</code> are
     * valid for submission into a <code>Scheduler</code>.
     * 
     * @throws IllegalStateException
     *           if a required property (such as Name, Group, Class) is not
     *           set.
     */
    public void validate() throws SchedulerException {
        if (name == null) {
            throw new SchedulerException("Trigger's name cannot be null");
        }

        if (group == null) {
            throw new SchedulerException("Trigger's group cannot be null");
        }

        if (jobName == null) {
            throw new SchedulerException(
                        "Trigger's related Job's name cannot be null");
        }

        if (jobGroup == null) {
            throw new SchedulerException(
                        "Trigger's related Job's group cannot be null");
        }
    }

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     * 
     * <p>
     * Usable by <code>{@link org.quartz.spi.JobStore}</code>
     * implementations, in order to facilitate 'recognizing' instances of fired
     * <code>Trigger</code> s as their jobs complete execution.
     * </p>
     * 
     *  
     */
    public void setFireInstanceId(String id) {
        this.fireInstanceId = id;
    }

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     */
    public String getFireInstanceId() {
        return fireInstanceId;
    }

    /**
     * <p>
     * Return a simple string representation of this object.
     * </p>
     */
    @Override
    public String toString() {
        return "Trigger '" + getFullName() + "':  triggerClass: '"
                + getClass().getName() + " calendar: '" + getCalendarName() 
                + "' misfireInstruction: " + getMisfireInstruction() 
                + " nextFireTime: " + getNextFireTime();
    }

    /**
     * <p>
     * Compare the next fire time of this <code>Trigger</code> to that of
     * another by comparing their keys, or in other words, sorts them
     * according to the natural (i.e. alphabetical) order of their keys.
     * </p>
     */
    public int compareTo(Trigger other) {

        if(other.getKey() == null && getKey() == null)
            return 0;
        if(other.getKey() == null)
            return -1;
        if(getKey() == null)
            return 1;
        
        return getKey().compareTo(other.getKey());
    }

    /**
     * Trigger equality is based upon the equality of the TriggerKey.
     * 
     * @return true if the key of this Trigger equals that of the given Trigger.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Trigger))
            return false;
        
        Trigger other = (Trigger)o;

        return !(other.getKey() == null || getKey() == null) && getKey().equals(other.getKey());

    }


    @Override
    public int hashCode() {
        if(getKey() == null)
            return super.hashCode();
        
        return getKey().hashCode();
    }

    @Override
    public Object clone() {
        AbstractTrigger<?> copy;
        try {
            copy = (AbstractTrigger<?>) super.clone();

            // Shallow copy the jobDataMap.  Note that this means that if a user
            // modifies a value object in this map from the cloned Trigger
            // they will also be modifying this Trigger. 
            if (jobDataMap != null) {
                copy.jobDataMap = (JobDataMap)jobDataMap.clone();
            }

        } catch (CloneNotSupportedException ex) {
            throw new IncompatibleClassChangeError("Not Cloneable.");
        }
        return copy;
    }
    
    public TriggerBuilder<T> getTriggerBuilder() {
        return TriggerBuilder.newTrigger()
            .forJob(getJobKey())
            .modifiedByCalendar(getCalendarName())
            .usingJobData(getJobDataMap())
            .withDescription(getDescription())
            .endAt(getEndTime())
            .withIdentity(getKey())
            .withPriority(getPriority())
            .startAt(getStartTime())
            .withSchedule(getScheduleBuilder());
    }

    public abstract ScheduleBuilder<T> getScheduleBuilder();
}
