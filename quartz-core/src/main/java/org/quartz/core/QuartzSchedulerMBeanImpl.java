package org.quartz.core;

import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.core.jmx.JobDetailSupport;
import org.quartz.core.jmx.JobExecutionContextSupport;
import org.quartz.core.jmx.QuartzSchedulerMBean;
import org.quartz.core.jmx.TriggerSupport;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.spi.OperableTrigger;

public class QuartzSchedulerMBeanImpl extends StandardMBean implements
        NotificationEmitter, QuartzSchedulerMBean, JobListener,
        SchedulerListener {
    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

    private final QuartzScheduler scheduler;
    private boolean sampledStatisticsEnabled;
    private SampledStatistics sampledStatistics;

    private final static SampledStatistics NULL_SAMPLED_STATISTICS = new NullSampledStatisticsImpl();

    static {
        final String[] notifTypes = new String[] { SCHEDULER_STARTED,
                SCHEDULER_PAUSED, SCHEDULER_SHUTDOWN, };
        final String name = Notification.class.getName();
        final String description = "QuartzScheduler JMX Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(
                notifTypes, name, description), };
    }

    /**
     * emitter
     */
    protected final Emitter emitter = new Emitter();

    /**
     * sequenceNumber
     */
    protected final AtomicLong sequenceNumber = new AtomicLong();

    /**
     * QuartzSchedulerMBeanImpl
     * 
     * @throws NotCompliantMBeanException
     */
    protected QuartzSchedulerMBeanImpl(QuartzScheduler scheduler)
            throws NotCompliantMBeanException {
        super(QuartzSchedulerMBean.class);
        this.scheduler = scheduler;
        this.scheduler.addInternalJobListener(this);
        this.scheduler.addInternalSchedulerListener(this);
        this.sampledStatistics = NULL_SAMPLED_STATISTICS;
        this.sampledStatisticsEnabled = false;
    }

    public TabularData getCurrentlyExecutingJobs() throws Exception {
        try {
            List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
            return JobExecutionContextSupport.toTabularData(currentlyExecutingJobs);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public TabularData getAllJobDetails() throws Exception {
        try {
            List<JobDetail> detailList = new ArrayList<JobDetail>();
            for (String jobGroupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroupName))) {
                    detailList.add(scheduler.getJobDetail(jobKey));
                }
            }
            return JobDetailSupport.toTabularData(detailList.toArray(new JobDetail[detailList.size()]));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<CompositeData> getAllTriggers() throws Exception {
        try {
            List<Trigger> triggerList = new ArrayList<Trigger>();
            for (String triggerGroupName : scheduler.getTriggerGroupNames()) {
                for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroupName))) {
                    triggerList.add(scheduler.getTrigger(triggerKey));
                }
            }
            return TriggerSupport.toCompositeList(triggerList);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void addJob(CompositeData jobDetail, boolean replace) throws Exception {
        try {
            scheduler.addJob(JobDetailSupport.newJobDetail(jobDetail), replace);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    private static void invokeSetter(Object target, String attribute, Object value) throws Exception {
        String setterName = "set" + Character.toUpperCase(attribute.charAt(0)) + attribute.substring(1);
        Class<?>[] argTypes = {value.getClass()};
        Method setter = findMethod(target.getClass(), setterName, argTypes);
        if(setter != null) {
            setter.invoke(target, value);
        } else {
            throw new Exception("Unable to find setter for attribute '" + attribute
                    + "' and value '" + value + "'");
        }
    }
    
    private static Class<?> getWrapperIfPrimitive(Class<?> c) {
        Class<?> result = c;
        try {
            Field f = c.getField("TYPE");
            f.setAccessible(true);
            result = (Class<?>) f.get(null);
        } catch (Exception e) {
            /**/
        }
        return result;
    }
    
    private static Method findMethod(Class<?> targetType, String methodName,
            Class<?>[] argTypes) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(targetType);
        if (beanInfo != null) {
            for(MethodDescriptor methodDesc: beanInfo.getMethodDescriptors()) {
                Method method = methodDesc.getMethod();
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (methodName.equals(method.getName()) && argTypes.length == parameterTypes.length) {
                    boolean matchedArgTypes = true;
                    for (int i = 0; i < argTypes.length; i++) { 
                        if (getWrapperIfPrimitive(argTypes[i]) != parameterTypes[i]) {
                            matchedArgTypes = false;
                            break;
                        }
                    }
                    if (matchedArgTypes) {
                        return method;
                    }
                }
            }
        }
        return null;
    }
    
    public void scheduleBasicJob(Map<String, Object> jobDetailInfo,
            Map<String, Object> triggerInfo) throws Exception {
        try {
            JobDetail jobDetail = JobDetailSupport.newJobDetail(jobDetailInfo);
            OperableTrigger trigger = TriggerSupport.newTrigger(triggerInfo);
            scheduler.deleteJob(jobDetail.getKey());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (ParseException pe) {
            throw pe;
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void scheduleJob(Map<String, Object> abstractJobInfo,
            Map<String, Object> abstractTriggerInfo) throws Exception {
        try {
            String triggerClassName = (String) abstractTriggerInfo.remove("triggerClass");
            if(triggerClassName == null) {
                throw new IllegalArgumentException("No triggerClass specified");
            }
            Class<?> triggerClass = Class.forName(triggerClassName);
            Trigger trigger = (Trigger) triggerClass.newInstance();
            
            String jobDetailClassName = (String) abstractJobInfo.remove("jobDetailClass");
            if(jobDetailClassName == null) {
                throw new IllegalArgumentException("No jobDetailClass specified");
            }
            Class<?> jobDetailClass = Class.forName(jobDetailClassName);
            JobDetail jobDetail = (JobDetail) jobDetailClass.newInstance();
            
            String jobClassName = (String) abstractJobInfo.remove("jobClass");
            if(jobClassName == null) {
                throw new IllegalArgumentException("No jobClass specified");
            }
            Class<?> jobClass = Class.forName(jobClassName);
            abstractJobInfo.put("jobClass", jobClass);
            
            for(Map.Entry<String, Object> entry : abstractTriggerInfo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if("jobDataMap".equals(key)) {
                    value = new JobDataMap((Map<?, ?>)value);
                }
                invokeSetter(trigger, key, value);
            }
            
            for(Map.Entry<String, Object> entry : abstractJobInfo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if("jobDataMap".equals(key)) {
                    value = new JobDataMap((Map<?, ?>)value);
                }
                invokeSetter(jobDetail, key, value);
            }
    
            AbstractTrigger<?> at = (AbstractTrigger<?>)trigger;
            at.setKey(new TriggerKey(at.getName(), at.getGroup()));
            
            Date startDate = at.getStartTime();
            if(startDate == null || startDate.before(new Date())) {
                at.setStartTime(new Date());
            }
            
            scheduler.deleteJob(jobDetail.getKey());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    public void scheduleJob(String jobName, String jobGroup,
            Map<String, Object> abstractTriggerInfo) throws Exception {
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if(jobDetail == null) {
                throw new IllegalArgumentException("No such job '" + jobKey + "'");
            }
            
            String triggerClassName = (String) abstractTriggerInfo.remove("triggerClass");
            if(triggerClassName == null) {
                throw new IllegalArgumentException("No triggerClass specified");
            }
            Class<?> triggerClass = Class.forName(triggerClassName);
            Trigger trigger = (Trigger) triggerClass.newInstance();
            
            for(Map.Entry<String, Object> entry : abstractTriggerInfo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if("jobDataMap".equals(key)) {
                    value = new JobDataMap((Map<?, ?>)value);
                }
                invokeSetter(trigger, key, value);
            }
            
            AbstractTrigger<?> at = (AbstractTrigger<?>)trigger;
            at.setKey(new TriggerKey(at.getName(), at.getGroup()));
            
            Date startDate = at.getStartTime();
            if(startDate == null || startDate.before(new Date())) {
                at.setStartTime(new Date());
            }
            
            scheduler.scheduleJob(trigger);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    public void addJob(Map<String, Object> abstractJobInfo,    boolean replace) throws Exception {
        try {
            String jobDetailClassName = (String) abstractJobInfo.remove("jobDetailClass");
            if(jobDetailClassName == null) {
                throw new IllegalArgumentException("No jobDetailClass specified");
            }
            Class<?> jobDetailClass = Class.forName(jobDetailClassName);
            JobDetail jobDetail = (JobDetail) jobDetailClass.newInstance();
            
            String jobClassName = (String) abstractJobInfo.remove("jobClass");
            if(jobClassName == null) {
                throw new IllegalArgumentException("No jobClass specified");
            }
            Class<?> jobClass = Class.forName(jobClassName);
            abstractJobInfo.put("jobClass", jobClass);
            
            for(Map.Entry<String, Object> entry : abstractJobInfo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if("jobDataMap".equals(key)) {
                    value = new JobDataMap((Map<?, ?>)value);
                }
                invokeSetter(jobDetail, key, value);
            }
    
            scheduler.addJob(jobDetail, replace);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    private Exception newPlainException(Exception e) {
        String type = e.getClass().getName();
        if(type.startsWith("java.") || type.startsWith("javax.")) {
            return e;
        } else {
            Exception result = new Exception(e.getMessage());
            result.setStackTrace(e.getStackTrace());
            return result;
        }
    }
    
    public void deleteCalendar(String calendarName) throws Exception {
        try {
            scheduler.deleteCalendar(calendarName);
        } catch(Exception e) {
            throw newPlainException(e);
        }
    }

    public boolean deleteJob(String jobName, String jobGroupName) throws Exception {
        try {
            return scheduler.deleteJob(jobKey(jobName, jobGroupName));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<String> getCalendarNames()    throws Exception {
        try {
            return scheduler.getCalendarNames();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public CompositeData getJobDetail(String jobName, String jobGroupName)
      throws Exception {
        try {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey(jobName, jobGroupName));
            return JobDetailSupport.toCompositeData(jobDetail);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<String> getJobGroupNames()    throws Exception {
        try {
            return scheduler.getJobGroupNames();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<String> getJobNames(String groupName) throws Exception {
        try {
            List<String> jobNames = new ArrayList<String>();
            for(JobKey key: scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                jobNames.add(key.getName());
            }
            return jobNames;
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public String getJobStoreClassName() {
        return scheduler.getJobStoreClass().getName();
    }

    public Set<String> getPausedTriggerGroups() throws Exception {
        try {
            return scheduler.getPausedTriggerGroups();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public CompositeData getTrigger(String name, String groupName) throws Exception {
        try {
            Trigger trigger = scheduler.getTrigger(triggerKey(name, groupName));
            return TriggerSupport.toCompositeData(trigger);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<String> getTriggerGroupNames()    throws Exception {
        try {
            return scheduler.getTriggerGroupNames();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<String> getTriggerNames(String groupName) throws Exception {
        try {
            List<String> triggerNames = new ArrayList<String>();
            for(TriggerKey key: scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupName))) {
                triggerNames.add(key.getName());
            }
            return triggerNames;
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public String getTriggerState(String triggerName, String triggerGroupName) throws Exception {
        try {
            TriggerKey triggerKey = triggerKey(triggerName, triggerGroupName);
            TriggerState ts = scheduler.getTriggerState(triggerKey);
            return ts.name();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public List<CompositeData> getTriggersOfJob(String jobName, String jobGroupName) throws Exception {
        try {
            JobKey jobKey = jobKey(jobName, jobGroupName);
            return TriggerSupport.toCompositeList(scheduler.getTriggersOfJob(jobKey));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public boolean interruptJob(String jobName, String jobGroupName) throws Exception {
        try {
            return scheduler.interrupt(jobKey(jobName, jobGroupName));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public boolean interruptJob(String fireInstanceId) throws Exception {
        try {
            return scheduler.interrupt(fireInstanceId);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public Date scheduleJob(String jobName, String jobGroup,
            String triggerName, String triggerGroup) throws Exception {
        try {
            JobKey jobKey = jobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail == null) {
                throw new IllegalArgumentException("No such job: " + jobKey);
            }
            TriggerKey triggerKey = triggerKey(triggerName, triggerGroup);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                throw new IllegalArgumentException("No such trigger: " + triggerKey);
            }
            return scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public boolean unscheduleJob(String triggerName, String triggerGroup) throws Exception {
        try {
            return scheduler.unscheduleJob(triggerKey(triggerName, triggerGroup));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

   public void clear() throws Exception {
       try {
           scheduler.clear();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public String getVersion() {
        return scheduler.getVersion();
    }

    public boolean isShutdown() {
        return scheduler.isShutdown();
    }

    public boolean isStarted() {
        return scheduler.isStarted();
    }

    public void start() throws Exception {
        try {
            scheduler.start();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public void standby() {
        scheduler.standby();
    }

    public boolean isStandbyMode() {
        return scheduler.isInStandbyMode();
    }

    public String getSchedulerName() {
        return scheduler.getSchedulerName();
    }

    public String getSchedulerInstanceId() {
        return scheduler.getSchedulerInstanceId();
    }

    public String getThreadPoolClassName() {
        return scheduler.getThreadPoolClass().getName();
    }

    public int getThreadPoolSize() {
        return scheduler.getThreadPoolSize();
    }

    public void pauseJob(String jobName, String jobGroup) throws Exception {
        try {
            scheduler.pauseJob(jobKey(jobName, jobGroup));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void pauseJobs(GroupMatcher<JobKey> matcher) throws Exception {
        try {
            scheduler.pauseJobs(matcher);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    public void pauseJobGroup(String jobGroup) throws Exception {
        pauseJobs(GroupMatcher.<JobKey>groupEquals(jobGroup));
    }

    public void pauseJobsStartingWith(String jobGroupPrefix) throws Exception {
        pauseJobs(GroupMatcher.<JobKey>groupStartsWith(jobGroupPrefix));
    }

    public void pauseJobsEndingWith(String jobGroupSuffix) throws Exception {
        pauseJobs(GroupMatcher.<JobKey>groupEndsWith(jobGroupSuffix));
    }

    public void pauseJobsContaining(String jobGroupToken) throws Exception {
        pauseJobs(GroupMatcher.<JobKey>groupContains(jobGroupToken));
    }

    public void pauseJobsAll() throws Exception {
        pauseJobs(GroupMatcher.anyJobGroup());
    }

    public void pauseAllTriggers() throws Exception {
        try {
            scheduler.pauseAll();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    private void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws Exception {
        try {
            scheduler.pauseTriggers(matcher);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    public void pauseTriggerGroup(String triggerGroup) throws Exception {
        pauseTriggers(GroupMatcher.<TriggerKey>groupEquals(triggerGroup));
    }

    public void pauseTriggersStartingWith(String triggerGroupPrefix) throws Exception {
        pauseTriggers(GroupMatcher.<TriggerKey>groupStartsWith(triggerGroupPrefix));
    }

    public void pauseTriggersEndingWith(String triggerGroupSuffix) throws Exception {
        pauseTriggers(GroupMatcher.<TriggerKey>groupEndsWith(triggerGroupSuffix));
    }

    public void pauseTriggersContaining(String triggerGroupToken) throws Exception {
        pauseTriggers(GroupMatcher.<TriggerKey>groupContains(triggerGroupToken));
    }

    public void pauseTriggersAll() throws Exception {
        pauseTriggers(GroupMatcher.anyTriggerGroup());
    }

    public void pauseTrigger(String triggerName, String triggerGroup) throws Exception {
        try {
            scheduler.pauseTrigger(triggerKey(triggerName, triggerGroup));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void resumeAllTriggers() throws Exception {
        try {
            scheduler.resumeAll();
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void resumeJob(String jobName, String jobGroup) throws Exception {
        try {
            scheduler.resumeJob(jobKey(jobName, jobGroup));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void resumeJobs(GroupMatcher<JobKey> matcher) throws Exception {
        try {
            scheduler.resumeJobs(matcher);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void resumeJobGroup(String jobGroup) throws Exception {
        resumeJobs(GroupMatcher.<JobKey>groupEquals(jobGroup));
    }

    public void resumeJobsStartingWith(String jobGroupPrefix) throws Exception {
        resumeJobs(GroupMatcher.<JobKey>groupStartsWith(jobGroupPrefix));
    }

    public void resumeJobsEndingWith(String jobGroupSuffix) throws Exception {
        resumeJobs(GroupMatcher.<JobKey>groupEndsWith(jobGroupSuffix));
    }

    public void resumeJobsContaining(String jobGroupToken) throws Exception {
        resumeJobs(GroupMatcher.<JobKey>groupContains(jobGroupToken));
    }

    public void resumeJobsAll() throws Exception {
        resumeJobs(GroupMatcher.anyJobGroup());
    }

    public void resumeTrigger(String triggerName, String triggerGroup) throws Exception {
        try {
            scheduler.resumeTrigger(triggerKey(triggerName, triggerGroup));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    private void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws Exception {
        try {
            scheduler.resumeTriggers(matcher);
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    public void resumeTriggerGroup(String triggerGroup) throws Exception {
        resumeTriggers(GroupMatcher.<TriggerKey>groupEquals(triggerGroup));
    }

    public void resumeTriggersStartingWith(String triggerGroupPrefix) throws Exception {
        resumeTriggers(GroupMatcher.<TriggerKey>groupStartsWith(triggerGroupPrefix));
    }

    public void resumeTriggersEndingWith(String triggerGroupSuffix) throws Exception {
        resumeTriggers(GroupMatcher.<TriggerKey>groupEndsWith(triggerGroupSuffix));
    }

    public void resumeTriggersContaining(String triggerGroupToken) throws Exception {
        resumeTriggers(GroupMatcher.<TriggerKey>groupContains(triggerGroupToken));
    }

    public void resumeTriggersAll() throws Exception {
        resumeTriggers(GroupMatcher.anyTriggerGroup());
    }

    public void triggerJob(String jobName, String jobGroup, Map<String, String> jobDataMap)
            throws Exception {
        try {
            scheduler.triggerJob(jobKey(jobName, jobGroup), new JobDataMap(jobDataMap));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }

    public void triggerJob(CompositeData trigger) throws Exception {
        try {
            scheduler.triggerJob(TriggerSupport.newTrigger(trigger));
        } catch (Exception e) {
            throw newPlainException(e);
        }
    }
    
    // ScheduleListener

    public void jobAdded(JobDetail jobDetail) {
        sendNotification(JOB_ADDED, JobDetailSupport.toCompositeData(jobDetail));
    }

    public void jobDeleted(JobKey jobKey) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("jobName", jobKey.getName());
        map.put("jobGroup", jobKey.getGroup());
        sendNotification(JOB_DELETED, map);
    }

    public void jobScheduled(Trigger trigger) {
        sendNotification(JOB_SCHEDULED, TriggerSupport.toCompositeData(trigger));
    }

    public void jobUnscheduled(TriggerKey triggerKey) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("triggerName", triggerKey.getName());
        map.put("triggerGroup", triggerKey.getGroup());
        sendNotification(JOB_UNSCHEDULED, map);
    }
    
    public void schedulingDataCleared() {
        sendNotification(SCHEDULING_DATA_CLEARED);
    }
    
    public void jobPaused(JobKey jobKey) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("jobName", jobKey.getName());
        map.put("jobGroup", jobKey.getGroup());
        sendNotification(JOBS_PAUSED, map);
    }

    public void jobsPaused(String jobGroup) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("jobName", null);
        map.put("jobGroup", jobGroup);
        sendNotification(JOBS_PAUSED, map);
    }
    
    public void jobsResumed(String jobGroup) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("jobName", null);
        map.put("jobGroup", jobGroup);
        sendNotification(JOBS_RESUMED, map);
    }

    public void jobResumed(JobKey jobKey) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("jobName", jobKey.getName());
        map.put("jobGroup", jobKey.getGroup());
        sendNotification(JOBS_RESUMED, map);
    }
    
    public void schedulerError(String msg, SchedulerException cause) {
        sendNotification(SCHEDULER_ERROR, cause.getMessage());
    }

    public void schedulerStarted() {
        sendNotification(SCHEDULER_STARTED);
    }
    
    //not doing anything, just like schedulerShuttingdown
    public void schedulerStarting() {
    }

    public void schedulerInStandbyMode() {
        sendNotification(SCHEDULER_PAUSED);
    }

    public void schedulerShutdown() {
        scheduler.removeInternalSchedulerListener(this);
        scheduler.removeInternalJobListener(getName());

        sendNotification(SCHEDULER_SHUTDOWN);
    }

    public void schedulerShuttingdown() {
    }

    public void triggerFinalized(Trigger trigger) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("triggerName", trigger.getKey().getName());
        map.put("triggerGroup", trigger.getKey().getGroup());
        sendNotification(TRIGGER_FINALIZED, map);
    }

    public void triggersPaused(String triggerGroup) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("triggerName", null);
        map.put("triggerGroup", triggerGroup);
        sendNotification(TRIGGERS_PAUSED, map);
    }

    public void triggerPaused(TriggerKey triggerKey) {
        Map<String, String> map = new HashMap<String, String>();
        if(triggerKey != null) {
            map.put("triggerName", triggerKey.getName());
            map.put("triggerGroup", triggerKey.getGroup());
        }
        sendNotification(TRIGGERS_PAUSED, map);
    }

    public void triggersResumed(String triggerGroup) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("triggerName", null);
        map.put("triggerGroup", triggerGroup);
        sendNotification(TRIGGERS_RESUMED, map);
    }

    public void triggerResumed(TriggerKey triggerKey) {
        Map<String, String> map = new HashMap<String, String>();
        if(triggerKey != null) {
            map.put("triggerName", triggerKey.getName());
            map.put("triggerGroup", triggerKey.getGroup());
        }
        sendNotification(TRIGGERS_RESUMED, map);
    }
    
    // JobListener

    public String getName() {
        return "QuartzSchedulerMBeanImpl.listener";
    }

    public void jobExecutionVetoed(JobExecutionContext context) {
        try {
            sendNotification(JOB_EXECUTION_VETOED, JobExecutionContextSupport
                    .toCompositeData(context));
        } catch (Exception e) {
            throw new RuntimeException(newPlainException(e));
        }
    }

    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            sendNotification(JOB_TO_BE_EXECUTED, JobExecutionContextSupport
                    .toCompositeData(context));
        } catch (Exception e) {
            throw new RuntimeException(newPlainException(e));
        }
    }

    public void jobWasExecuted(JobExecutionContext context,
            JobExecutionException jobException) {
        try {
            sendNotification(JOB_WAS_EXECUTED, JobExecutionContextSupport
                    .toCompositeData(context));
        } catch (Exception e) {
            throw new RuntimeException(newPlainException(e));
        }
    }

    // NotificationBroadcaster

    /**
     * sendNotification
     * 
     * @param eventType
     */
    public void sendNotification(String eventType) {
        sendNotification(eventType, null, null);
    }

    /**
     * sendNotification
     * 
     * @param eventType
     * @param data
     */
    public void sendNotification(String eventType, Object data) {
        sendNotification(eventType, data, null);
    }

    /**
     * sendNotification
     * 
     * @param eventType
     * @param data
     * @param msg
     */
    public void sendNotification(String eventType, Object data, String msg) {
        Notification notif = new Notification(eventType, this, sequenceNumber
                .incrementAndGet(), System.currentTimeMillis(), msg);
        if (data != null) {
            notif.setUserData(data);
        }
        emitter.sendNotification(notif);
    }

    /**
     * @author gkeim
     */
    private class Emitter extends NotificationBroadcasterSupport {
        /**
         * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
         */
        @Override
        public MBeanNotificationInfo[] getNotificationInfo() {
            return QuartzSchedulerMBeanImpl.this.getNotificationInfo();
        }
    }

    /**
     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void addNotificationListener(NotificationListener notif,
            NotificationFilter filter, Object callBack) {
        emitter.addNotificationListener(notif, filter, callBack);
    }

    /**
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        return NOTIFICATION_INFO;
    }

    /**
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
    }

    /**
     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void removeNotificationListener(NotificationListener notif,
            NotificationFilter filter, Object callBack)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(notif, filter, callBack);
    }

    public synchronized boolean isSampledStatisticsEnabled() {
        return sampledStatisticsEnabled;
    }

    public void setSampledStatisticsEnabled(boolean enabled) {
        if (enabled != this.sampledStatisticsEnabled) {
            this.sampledStatisticsEnabled = enabled;
            if(enabled) {
                this.sampledStatistics = new SampledStatisticsImpl(scheduler);
            }
            else {
                 this.sampledStatistics.shutdown(); 
                 this.sampledStatistics = NULL_SAMPLED_STATISTICS;
            }
            sendNotification(SAMPLED_STATISTICS_ENABLED, Boolean.valueOf(enabled));
        }
    }

    public long getJobsCompletedMostRecentSample() {
        return this.sampledStatistics.getJobsCompletedMostRecentSample();
    }

    public long getJobsExecutedMostRecentSample() {
        return this.sampledStatistics.getJobsExecutingMostRecentSample();
    }

    public long getJobsScheduledMostRecentSample() {
        return this.sampledStatistics.getJobsScheduledMostRecentSample();
    }

    public Map<String, Long> getPerformanceMetrics() {
        Map<String, Long> result = new HashMap<String, Long>();
        result.put("JobsCompleted", Long
                .valueOf(getJobsCompletedMostRecentSample()));
        result.put("JobsExecuted", Long
                .valueOf(getJobsExecutedMostRecentSample()));
        result.put("JobsScheduled", Long
                .valueOf(getJobsScheduledMostRecentSample()));
        return result;
    }
}
