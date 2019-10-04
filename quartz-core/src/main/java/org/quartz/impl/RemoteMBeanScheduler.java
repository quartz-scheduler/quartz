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

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.Trigger.TriggerState;
import org.quartz.core.jmx.JobDetailSupport;
import org.quartz.core.jmx.TriggerSupport;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.spi.JobFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that remotely
 * proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code>
 * instance, via JMX.
 * </p>
 * 
 * <p>
 * A user must create a subclass to implement the actual connection to the remote 
 * MBeanServer using their application specific connector.
 * </p>
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzScheduler
 */
public abstract class RemoteMBeanScheduler implements Scheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private ObjectName schedulerObjectName;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public RemoteMBeanScheduler() { 
    }
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Properties.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    /**
     * Get the name under which the Scheduler MBean is registered on the
     * remote MBean server.
     */
    protected ObjectName getSchedulerObjectName() {
        return schedulerObjectName;
    }

    /**
     * Set the name under which the Scheduler MBean is registered on the
     * remote MBean server.
     */
    public void setSchedulerObjectName(String schedulerObjectName)  throws SchedulerException {
        try {
            this.schedulerObjectName = new ObjectName(schedulerObjectName);
        } catch (MalformedObjectNameException e) {
            throw new SchedulerException("Failed to parse Scheduler MBean name: " + schedulerObjectName, e);
        }
    }

    /**
     * Set the name under which the Scheduler MBean is registered on the
     * remote MBean server.
     */
    public void setSchedulerObjectName(ObjectName schedulerObjectName)  throws SchedulerException {
        this.schedulerObjectName = schedulerObjectName;
    }

    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Abstract methods.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Initialize this RemoteMBeanScheduler instance, connecting to the
     * remote MBean server.
     */
    public abstract void initialize() throws SchedulerException;

    /**
     * Get the given attribute of the remote Scheduler MBean.
     */
    protected abstract Object getAttribute(
            String attribute) throws SchedulerException;
        
    /**
     * Get the given attributes of the remote Scheduler MBean.
     */
    protected abstract AttributeList getAttributes(String[] attributes)
        throws SchedulerException;
    
    /**
     * Invoke the given operation on the remote Scheduler MBean.
     */
    protected abstract Object invoke(
        String operationName,
        Object[] params,
        String[] signature) throws SchedulerException;
        

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Returns the name of the <code>Scheduler</code>.
     * </p>
     */
    public String getSchedulerName() throws SchedulerException {
        return (String)getAttribute("SchedulerName");
    }

    /**
     * <p>
     * Returns the instance Id of the <code>Scheduler</code>.
     * </p>
     */
    public String getSchedulerInstanceId() throws SchedulerException {
        return (String)getAttribute("SchedulerInstanceId");
    }

    public SchedulerMetaData getMetaData() throws SchedulerException {
        AttributeList attributeList =
            getAttributes(
                new String[] {
                    "SchedulerName",
                    "SchedulerInstanceId",
                    "StandbyMode",
                    "Shutdown",
                    "JobStoreClassName",
                    "ThreadPoolClassName",
                    "ThreadPoolSize",
                    "Version",
                    "PerformanceMetrics"
                });

        try {
            return new SchedulerMetaData(
                    (String)getAttribute(attributeList, 0).getValue(),
                    (String)getAttribute(attributeList, 1).getValue(),
                    getClass(), true, false,
                    (Boolean)getAttribute(attributeList, 2).getValue(),
                    (Boolean)getAttribute(attributeList, 3).getValue(),
                    null,
                    Integer.parseInt(((Map)getAttribute(attributeList, 8).getValue()).get("JobsExecuted").toString()),
                    Class.forName((String)getAttribute(attributeList, 4).getValue()),
                    false,
                    false,
                    Class.forName((String)getAttribute(attributeList, 5).getValue()),
                    (Integer)getAttribute(attributeList, 6).getValue(),
                    (String)getAttribute(attributeList, 7).getValue());
        } catch (ClassNotFoundException e) {
            throw new SchedulerException(e);
        }
    }

    private Attribute getAttribute(AttributeList attributeList, int index) {
        return (Attribute)attributeList.get(index);
    }

    /**
     * <p>
     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
     * </p>
     */
    public SchedulerContext getContext() throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Schedululer State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void start() throws SchedulerException {
        invoke("start", new Object[] {}, new String[] {});
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void startDelayed(int seconds) throws SchedulerException {
        invoke("startDelayed", new Object[] {seconds}, new String[] {int.class.getName()});
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void standby() throws SchedulerException {
        invoke("standby", new Object[] {}, new String[] {});
    }

    /**
     * Whether the scheduler has been started.  
     * 
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever
     * been called on this Scheduler, so it will return <code>true</code> even 
     * if the <code>Scheduler</code> is currently in standby mode or has been 
     * since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */    
    public boolean isStarted() throws SchedulerException {
        return (Boolean) getAttribute("Started");
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean isInStandbyMode() throws SchedulerException {
        return (Boolean)getAttribute("StandbyMode");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void shutdown() throws SchedulerException {
        // Have to get the scheduler name before we actually call shutdown.
        String schedulerName = getSchedulerName();
        
        invoke("shutdown", new Object[] {}, new String[] {});
        SchedulerRepository.getInstance().remove(schedulerName);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean isShutdown() throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger)
        throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void addJob(JobDetail jobDetail, boolean replace)
        throws SchedulerException {
        invoke(
            "addJob", 
            new Object[] { JobDetailSupport.toCompositeData(jobDetail), replace },
            new String[] { CompositeData.class.getName(), boolean.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling)
            throws SchedulerException {
        invoke(
                "addJob",
                new Object[] { JobDetailSupport.toCompositeData(jobDetail), replace , storeNonDurableWhileAwaitingScheduling},
                new String[] { CompositeData.class.getName(), boolean.class.getName(), boolean.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public boolean deleteJob(JobKey jobKey)
        throws SchedulerException {
        return (Boolean)invoke(
                "deleteJob",
                new Object[] { jobKey.getName(), jobKey.getGroup() },
                new String[] { String.class.getName(), String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public boolean unscheduleJob(TriggerKey triggerKey)
        throws SchedulerException {
        return (Boolean)invoke(
                "unscheduleJob",
                new Object[] { triggerKey.getName(), triggerKey.getGroup() },
                new String[] { String.class.getName(), String.class.getName() });
    }


    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Date rescheduleJob(TriggerKey triggerKey,
            Trigger newTrigger) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }
    
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void triggerJob(JobKey jobKey) throws SchedulerException {
        triggerJob(jobKey, null);
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseTrigger(TriggerKey triggerKey) throws SchedulerException {
        invoke(
            "pauseTrigger", 
            new Object[] { triggerKey.getName(), triggerKey.getGroup() },
            new String[] { String.class.getName(), String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        String operation = null;
        switch (matcher.getCompareWithOperator()) {
            case EQUALS:
                operation = "pauseTriggerGroup";
                break;
            case CONTAINS:
                operation = "pauseTriggersContaining";
                break;
            case STARTS_WITH:
                operation = "pauseTriggersStartingWith";
                break;
            case ENDS_WITH:
                operation = "pauseTriggersEndingWith";
            case ANYTHING:
                operation = "pauseTriggersAll";
        }

        if (operation != null) {
            invoke(
                    operation,
                    new Object[] { matcher.getCompareToValue() },
                    new String[] { String.class.getName() });
        } else {
            throw new SchedulerException("Unsupported GroupMatcher kind for pausing triggers: " + matcher.getCompareWithOperator());
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        invoke(
            "pauseJob", 
            new Object[] { jobKey.getName(), jobKey.getGroup() },
            new String[] { String.class.getName(), String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        String operation = null;
        switch (matcher.getCompareWithOperator()) {
            case EQUALS:
                operation = "pauseJobGroup";
                break;
            case STARTS_WITH:
                operation = "pauseJobsStartingWith";
                break;
            case ENDS_WITH:
                operation = "pauseJobsEndingWith";
                break;
            case CONTAINS:
                operation = "pauseJobsContaining";
            case ANYTHING:
                operation = "pauseJobsAll";
        }

        invoke(
                operation,
                new Object[] { matcher.getCompareToValue() },
                new String[] { String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        invoke(
            "resumeTrigger", 
            new Object[] { triggerKey.getName(), triggerKey.getGroup() },
            new String[] { String.class.getName(), String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        String operation = null;
        switch (matcher.getCompareWithOperator()) {
            case EQUALS:
                operation = "resumeTriggerGroup";
                break;
            case CONTAINS:
                operation = "resumeTriggersContaining";
                break;
            case STARTS_WITH:
                operation = "resumeTriggersStartingWith";
                break;
            case ENDS_WITH:
                operation = "resumeTriggersEndingWith";
            case ANYTHING:
                operation = "resumeTriggersAll";
        }

        if (operation != null) {
            invoke(
                    operation,
                    new Object[] { matcher.getCompareToValue() },
                    new String[] { String.class.getName() });
        } else {
            throw new SchedulerException("Unsupported GroupMatcher kind for resuming triggers: " + matcher.getCompareWithOperator());
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeJob(JobKey jobKey)
        throws SchedulerException {
        invoke(
            "resumeJob", 
            new Object[] { jobKey.getName(), jobKey.getGroup() },
            new String[] { String.class.getName(), String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        String operation = null;
        switch (matcher.getCompareWithOperator()) {
            case EQUALS:
                operation = "resumeJobGroup";
                break;
            case STARTS_WITH:
                operation = "resumeJobsStartingWith";
                break;
            case ENDS_WITH:
                operation = "resumeJobsEndingWith";
                break;
            case CONTAINS:
                operation = "resumeJobsContaining";
            case ANYTHING:
                operation = "resumeJobsAll";
        }

        invoke(
                operation,
                new Object[] { matcher.getCompareToValue() },
                new String[] { String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseAll() throws SchedulerException {
        invoke(
            "pauseAllTriggers",
            new Object[] { }, 
            new String[] { });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeAll() throws SchedulerException {
        invoke(
            "resumeAllTriggers",
            new Object[] { }, 
            new String[] { });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public List<String> getJobGroupNames() throws SchedulerException {
        return (List<String>)getAttribute("JobGroupNames");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
        if (matcher.getCompareWithOperator().equals(StringMatcher.StringOperatorName.EQUALS)) {
            List<JobKey> keys = (List<JobKey>)invoke(
                    "getJobNames",
                    new Object[] { matcher.getCompareToValue() },
                    new String[] { String.class.getName() });

            return new HashSet<JobKey>(keys);
        } else {
            throw new SchedulerException("Only equals matcher are supported for looking up JobKeys");
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public List<Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public List<String> getTriggerGroupNames() throws SchedulerException {
        return (List<String>)getAttribute("TriggerGroupNames");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        try {
            return JobDetailSupport.newJobDetail((CompositeData)invoke(
                    "getJobDetail",
                    new Object[] { jobKey.getName(), jobKey.getGroup() },
                    new String[] { String.class.getName(), String.class.getName() }));
        } catch (ClassNotFoundException e) {
            throw new SchedulerException("Unable to resolve job class", e);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
        return (Boolean)invoke(
                "checkExists", 
                new Object[] { triggerKey }, 
                new String[] { TriggerKey.class.getName() });
    }
    
    public void clear() throws SchedulerException {
        invoke(
                "clear", 
                new Object[] {  }, 
                new String[] {  });
    }


    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public TriggerState getTriggerState(TriggerKey triggerKey)
        throws SchedulerException {
        return TriggerState.valueOf((String)invoke(
                "getTriggerState",
                new Object[] { triggerKey.getName(), triggerKey.getGroup() },
                new String[] { String.class.getName(), String.class.getName() }));
    }


    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resetTriggerFromErrorState(TriggerKey triggerKey)
            throws SchedulerException {
        invoke(
            "resetTriggerFromErrorState",
            new Object[] { triggerKey.getName(), triggerKey.getGroup() },
            new String[] { String.class.getName(), String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException {
        invoke(
            "addCalendar", 
            new Object[] { calName, calendar, replace, updateTriggers },
            new String[] { String.class.getName(), 
                    Calendar.class.getName(), boolean.class.getName(), boolean.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public boolean deleteCalendar(String calName) throws SchedulerException {
        invoke("deleteCalendar",
                new Object[] { calName },
                new String[] { String.class.getName() });
        return true;
    }

    /**
     * <p>
     * Calls th0e equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Calendar getCalendar(String calName) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public List<String> getCalendarNames() throws SchedulerException {
        return (List<String>)getAttribute("CalendarNames");
    }

    /**
     * @see org.quartz.Scheduler#getPausedTriggerGroups()
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPausedTriggerGroups() throws SchedulerException {
        return (Set<String>)getAttribute("PausedTriggerGroups");
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Other Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public ListenerManager getListenerManager() throws SchedulerException {
        throw new SchedulerException(
                "Operation not supported for remote schedulers.");
    }

    /**
     * @see org.quartz.Scheduler#interrupt(JobKey)
     */
    public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException  {
        try {
            return (Boolean)invoke(
                    "interruptJob",
                    new Object[] { jobKey.getName(), jobKey.getGroup() },
                    new String[] { String.class.getName(), String.class.getName() });
        } catch (SchedulerException se) {
            throw new UnableToInterruptJobException(se);
        }
    }



    public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
        try {
            return (Boolean)invoke(
                    "interruptJob",
                    new Object[] { fireInstanceId },
                    new String[] { String.class.getName() });
        } catch (SchedulerException se) {
            throw new UnableToInterruptJobException(se);
        }
    }
    
    /**
     * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
     */
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }
    
}
