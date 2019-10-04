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
package org.terracotta.quartz;

import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex Snaps
 */
public abstract class AbstractTerracottaJobStore implements JobStore {
  public static final String                    TC_CONFIG_PROP                          = StdSchedulerFactory.PROP_JOB_STORE_PREFIX
                                                                                          + ".tcConfig";
  public static final String                    TC_CONFIGURL_PROP                       = StdSchedulerFactory.PROP_JOB_STORE_PREFIX
                                                                                          + ".tcConfigUrl";
  public static final String                    TC_REJOIN_PROP                          = StdSchedulerFactory.PROP_JOB_STORE_PREFIX
                                                                                          + ".rejoin";

  private volatile ToolkitInternal              toolkit;
  private volatile TerracottaJobStoreExtensions realJobStore;
  private String                                tcConfig                                = null;
  private String                                tcConfigUrl                             = null;
  private String                                schedInstId                             = null;
  private String                                schedName                               = null;
  private Long                                  misFireThreshold                        = null;
  private String                                synchWrite                              = null;
  private String                                rejoin                                  = null;
  private Long                                  estimatedTimeToReleaseAndAcquireTrigger = null;
  private long                                  tcRetryInterval                         = TimeUnit.SECONDS.toMillis(15);

  private void init() throws SchedulerConfigException {
    if (realJobStore != null) { return; }

    if ((tcConfig != null) && (tcConfigUrl != null)) {
      //
      throw new SchedulerConfigException("Both " + TC_CONFIG_PROP + " and " + TC_CONFIGURL_PROP
                                         + " are set in your properties. Please define only one of them");
    }

    if ((tcConfig == null) && (tcConfigUrl == null)) {
      //
      throw new SchedulerConfigException("Neither " + TC_CONFIG_PROP + " or " + TC_CONFIGURL_PROP
                                         + " are set in your properties. Please define one of them");
    }

    final boolean isURLConfig = tcConfig == null;
    TerracottaToolkitBuilder toolkitBuilder = new TerracottaToolkitBuilder();
    if (isURLConfig) {
      toolkitBuilder.setTCConfigUrl(tcConfigUrl);
    } else {
      toolkitBuilder.setTCConfigSnippet(tcConfig);
    }
    if (rejoin != null) {
      toolkitBuilder.setRejoin(rejoin);
    }
    toolkitBuilder.addTunnelledMBeanDomain("quartz");
    toolkit = (ToolkitInternal) toolkitBuilder.buildToolkit();

    try {
      realJobStore = getRealStore(toolkit);
    } catch (Exception e) {
      throw new SchedulerConfigException("Unable to create Terracotta client", e);
    }
  }

  abstract TerracottaJobStoreExtensions getRealStore(ToolkitInternal toolkitParam);

  public String getUUID() {
    if (realJobStore == null) {
      try {
        init();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return realJobStore.getUUID();
  }

  public void setMisfireThreshold(long threshold) {
    this.misFireThreshold = threshold;
  }

  public void setTcRetryInterval(long tcRetryInterval) {
    this.tcRetryInterval = tcRetryInterval;
  }

  @Override
  public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
      throws JobPersistenceException {
    try {
      return realJobStore.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger acquisition failed due to client rejoin", e);
    }
  }

  @Override
  public List<String> getCalendarNames() throws JobPersistenceException {
    try {
      return realJobStore.getCalendarNames();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Calendar name retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public List<String> getJobGroupNames() throws JobPersistenceException {
    try {
      return realJobStore.getJobGroupNames();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Job name retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    try {
      return realJobStore.getJobKeys(matcher);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Job key retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public int getNumberOfCalendars() throws JobPersistenceException {
    try {
      return realJobStore.getNumberOfCalendars();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Calendar count retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public int getNumberOfJobs() throws JobPersistenceException {
    try {
      return realJobStore.getNumberOfJobs();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Job count retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public int getNumberOfTriggers() throws JobPersistenceException {
    try {
      return realJobStore.getNumberOfTriggers();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger count retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
    try {
      return realJobStore.getPausedTriggerGroups();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Paused trigger group retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public List<String> getTriggerGroupNames() throws JobPersistenceException {
    try {
      return realJobStore.getTriggerGroupNames();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger group retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    try {
      return realJobStore.getTriggerKeys(matcher);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger key retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public List<OperableTrigger> getTriggersForJob(JobKey jobKey) throws JobPersistenceException {
    try {
      return realJobStore.getTriggersForJob(jobKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public Trigger.TriggerState getTriggerState(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      return realJobStore.getTriggerState(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger state retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public void resetTriggerFromErrorState(final TriggerKey triggerKey) throws JobPersistenceException {
    try {
      realJobStore.resetTriggerFromErrorState(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger state reset failed due to client rejoin", e);
    }
  }

  @Override
  public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
    init();
    realJobStore.setInstanceId(schedInstId);
    realJobStore.setInstanceName(schedName);
    realJobStore.setTcRetryInterval(tcRetryInterval);

    if (misFireThreshold != null) {
      realJobStore.setMisfireThreshold(misFireThreshold);
    }

    if (synchWrite != null) {
      realJobStore.setSynchronousWrite(synchWrite);
    }

    if (estimatedTimeToReleaseAndAcquireTrigger != null) {
      realJobStore.setEstimatedTimeToReleaseAndAcquireTrigger(estimatedTimeToReleaseAndAcquireTrigger);
    }

    realJobStore.initialize(loadHelper, signaler);
  }

  @Override
  public void pauseAll() throws JobPersistenceException {
    try {
      realJobStore.pauseAll();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Pausing failed due to client rejoin", e);
    }
  }

  @Override
  public void pauseJob(JobKey jobKey) throws JobPersistenceException {
    try {
      realJobStore.pauseJob(jobKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Pausing job failed due to client rejoin", e);
    }
  }

  @Override
  public Collection<String> pauseJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    try {
      return realJobStore.pauseJobs(matcher);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Pausing jobs failed due to client rejoin", e);
    }
  }

  @Override
  public void pauseTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      realJobStore.pauseTrigger(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Pausing trigger failed due to client rejoin", e);
    }
  }

  @Override
  public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    try {
      return realJobStore.pauseTriggers(matcher);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Pausing triggers failed due to client rejoin", e);
    }
  }

  @Override
  public void releaseAcquiredTrigger(OperableTrigger trigger) {
    realJobStore.releaseAcquiredTrigger(trigger);
  }

  @Override
  public boolean removeCalendar(String calName) throws JobPersistenceException {
    try {
      return realJobStore.removeCalendar(calName);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Removing calendar failed due to client rejoin", e);
    }
  }

  @Override
  public boolean removeJob(JobKey jobKey) throws JobPersistenceException {
    try {
      return realJobStore.removeJob(jobKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Removing job failed due to client rejoin", e);
    }
  }

  @Override
  public boolean removeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      return realJobStore.removeTrigger(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Removing trigger failed due to client rejoin", e);
    }
  }

  @Override
  public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger) throws JobPersistenceException {
    try {
      return realJobStore.replaceTrigger(triggerKey, newTrigger);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Replacing trigger failed due to client rejoin", e);
    }
  }

  @Override
  public void resumeAll() throws JobPersistenceException {
    try {
      realJobStore.resumeAll();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Resuming failed due to client rejoin", e);
    }
  }

  @Override
  public void resumeJob(JobKey jobKey) throws JobPersistenceException {
    try {
      realJobStore.resumeJob(jobKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Reusming job failed due to client rejoin", e);
    }
  }

  @Override
  public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    try {
      return realJobStore.resumeJobs(matcher);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Resuming jobs failed due to client rejoin", e);
    }
  }

  @Override
  public void resumeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      realJobStore.resumeTrigger(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Resuming trigger failed due to client rejoin", e);
    }
  }

  @Override
  public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    try {
      return realJobStore.resumeTriggers(matcher);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Resuming triggers failed due to client rejoin", e);
    }
  }

  @Override
  public Calendar retrieveCalendar(String calName) throws JobPersistenceException {
    try {
      return realJobStore.retrieveCalendar(calName);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Calendar retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public JobDetail retrieveJob(JobKey jobKey) throws JobPersistenceException {
    try {
      return realJobStore.retrieveJob(jobKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Job retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public OperableTrigger retrieveTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      return realJobStore.retrieveTrigger(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger retrieval failed due to client rejoin", e);
    }
  }

  @Override
  public void schedulerStarted() throws SchedulerException {
    try {
      realJobStore.schedulerStarted();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Scheduler start failed due to client rejoin", e);
    }
  }

  @Override
  public void schedulerPaused() {
    realJobStore.schedulerPaused();
  }

  @Override
  public void schedulerResumed() {
    realJobStore.schedulerResumed();
  }

  @Override
  public void setInstanceId(String schedInstId) {
    this.schedInstId = schedInstId;
  }

  @Override
  public void setInstanceName(String schedName) {
    this.schedName = schedName;
  }

  @Override
  public void setThreadPoolSize(final int poolSize) {
    realJobStore.setThreadPoolSize(poolSize);
  }

  @Override
  public void shutdown() {
    if (realJobStore != null) {
      realJobStore.shutdown();
    }
    if (toolkit != null) {
      toolkit.shutdown();
    }
  }

  @Override
  public void storeCalendar(String name, Calendar calendar, boolean replaceExisting, boolean updateTriggers)
      throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      realJobStore.storeCalendar(name, calendar, replaceExisting, updateTriggers);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Storing calendar failed due to client rejoin", e);
    }
  }

  @Override
  public void storeJob(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException,
      JobPersistenceException {
    try {
      realJobStore.storeJob(newJob, replaceExisting);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Storing job failed due to client rejoin", e);
    }
  }

  @Override
  public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws ObjectAlreadyExistsException,
      JobPersistenceException {
    try {
      realJobStore.storeJobAndTrigger(newJob, newTrigger);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Storing job and trigger failed due to client rejoin", e);
    }
  }

  @Override
  public void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting) throws ObjectAlreadyExistsException,
      JobPersistenceException {
    try {
      realJobStore.storeTrigger(newTrigger, replaceExisting);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Storing trigger failed due to client rejoin", e);
    }
  }

  @Override
  public boolean supportsPersistence() {
    return true;
  }

  @Override
  public void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail, Trigger.CompletedExecutionInstruction instruction) {
    realJobStore.triggeredJobComplete(trigger, jobDetail, instruction);
  }

  @Override
  public List<TriggerFiredResult> triggersFired(List<OperableTrigger> triggers) throws JobPersistenceException {
    try {
      return realJobStore.triggersFired(triggers);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger fire marking failed due to client rejoin", e);
    }
  }

  public void setTcConfig(String tcConfig) {
    this.tcConfig = tcConfig.trim();
  }

  public void setTcConfigUrl(String tcConfigUrl) {
    this.tcConfigUrl = tcConfigUrl.trim();
  }

  public void setSynchronousWrite(String synchWrite) {
    this.synchWrite = synchWrite;
  }

  public void setRejoin(String rejoin) {
    this.rejoin = rejoin;
    setSynchronousWrite(Boolean.TRUE.toString());
  }

  @Override
  public long getEstimatedTimeToReleaseAndAcquireTrigger() {
    return realJobStore.getEstimatedTimeToReleaseAndAcquireTrigger();
  }

  public void setEstimatedTimeToReleaseAndAcquireTrigger(long estimate) {
    this.estimatedTimeToReleaseAndAcquireTrigger = estimate;
  }

  @Override
  public boolean isClustered() {
    return true;
  }

  @Override
  public boolean checkExists(final JobKey jobKey) throws JobPersistenceException {
    try {
      return realJobStore.checkExists(jobKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Job existence check failed due to client rejoin", e);
    }
  }

  @Override
  public boolean checkExists(final TriggerKey triggerKey) throws JobPersistenceException {
    try {
      return realJobStore.checkExists(triggerKey);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Trigger existence check failed due to client rejoin", e);
    }
  }

  @Override
  public void clearAllSchedulingData() throws JobPersistenceException {
    try {
      realJobStore.clearAllSchedulingData();
    } catch (RejoinException e) {
      throw new JobPersistenceException("Scheduler data clear failed due to client rejoin", e);
    }
  }

  @Override
  public boolean removeTriggers(List<TriggerKey> arg0) throws JobPersistenceException {
    try {
      return realJobStore.removeTriggers(arg0);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Remvoing triggers failed due to client rejoin", e);
    }
  }

  @Override
  public boolean removeJobs(List<JobKey> arg0) throws JobPersistenceException {
    try {
      return realJobStore.removeJobs(arg0);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Removing jobs failed due to client rejoin", e);
    }
  }

  @Override
  public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> arg0, boolean arg1)
      throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      realJobStore.storeJobsAndTriggers(arg0, arg1);
    } catch (RejoinException e) {
      throw new JobPersistenceException("Store jobs and triggers failed due to client rejoin", e);
    }
  }

  @Override
  public long getAcquireRetryDelay(int failureCount) {
    try {
      return realJobStore.getAcquireRetryDelay(failureCount);
    } catch (Exception e) {
      return 20;
    }
  }

  protected TerracottaJobStoreExtensions getRealJobStore() {
    return realJobStore;
  }
}
