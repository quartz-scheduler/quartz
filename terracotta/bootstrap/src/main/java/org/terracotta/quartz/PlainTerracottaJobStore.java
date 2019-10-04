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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;
import org.terracotta.toolkit.internal.ToolkitInternal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

public class PlainTerracottaJobStore<T extends ClusteredJobStore> implements TerracottaJobStoreExtensions {

  private static final long WEEKLY                                  = 7 * 24 * 60 * 60 * 1000L;
  private Timer             updateCheckTimer;
  private volatile T        clusteredJobStore                       = null;
  private Long              misfireThreshold                        = null;
  private String            schedName;
  private String            synchWrite                              = "false";
  private Long              estimatedTimeToReleaseAndAcquireTrigger = null;
  private String            schedInstanceId;
  private long              tcRetryInterval;
  private int               threadPoolSize;
  protected final ToolkitInternal toolkit;

  public PlainTerracottaJobStore(ToolkitInternal toolkit) {
    this.toolkit = toolkit;
  }

  @Override
  public void setSynchronousWrite(String synchWrite) {
    this.synchWrite = synchWrite;
  }

  @Override
  public void setThreadPoolSize(final int size) {
    this.threadPoolSize = size;
  }

  @Override
  public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
      throws JobPersistenceException {
    return clusteredJobStore.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
  }

  @Override
  public List<String> getCalendarNames() throws JobPersistenceException {
    return clusteredJobStore.getCalendarNames();
  }

  @Override
  public List<String> getJobGroupNames() throws JobPersistenceException {
    return clusteredJobStore.getJobGroupNames();
  }

  @Override
  public Set<JobKey> getJobKeys(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.getJobKeys(matcher);
  }

  @Override
  public int getNumberOfCalendars() throws JobPersistenceException {
    return clusteredJobStore.getNumberOfCalendars();
  }

  @Override
  public int getNumberOfJobs() throws JobPersistenceException {
    return clusteredJobStore.getNumberOfJobs();
  }

  @Override
  public int getNumberOfTriggers() throws JobPersistenceException {
    return clusteredJobStore.getNumberOfTriggers();
  }

  @Override
  public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
    return clusteredJobStore.getPausedTriggerGroups();
  }

  @Override
  public List<String> getTriggerGroupNames() throws JobPersistenceException {
    return clusteredJobStore.getTriggerGroupNames();
  }

  @Override
  public Set<TriggerKey> getTriggerKeys(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.getTriggerKeys(matcher);
  }

  @Override
  public List<OperableTrigger> getTriggersForJob(final JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.getTriggersForJob(jobKey);
  }

  @Override
  public Trigger.TriggerState getTriggerState(final TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.getTriggerState(triggerKey);
  }

  @Override
  public void resetTriggerFromErrorState(final TriggerKey triggerKey) throws JobPersistenceException {
    clusteredJobStore.resetTriggerFromErrorState(triggerKey);
  }

  @Override
  public synchronized void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
      throws SchedulerConfigException {
    if (clusteredJobStore != null) { throw new IllegalStateException("already initialized"); }

    clusteredJobStore = createNewJobStoreInstance(schedName, Boolean.valueOf(synchWrite));
    clusteredJobStore.setThreadPoolSize(threadPoolSize);

    // apply deferred misfire threshold if present
    if (misfireThreshold != null) {
      clusteredJobStore.setMisfireThreshold(misfireThreshold);
      misfireThreshold = null;
    }

    if (estimatedTimeToReleaseAndAcquireTrigger != null) {
      clusteredJobStore.setEstimatedTimeToReleaseAndAcquireTrigger(estimatedTimeToReleaseAndAcquireTrigger);
      estimatedTimeToReleaseAndAcquireTrigger = null;
    }
    clusteredJobStore.setInstanceId(schedInstanceId);
    clusteredJobStore.setTcRetryInterval(tcRetryInterval);
    clusteredJobStore.initialize(loadHelper, signaler);

  }

  @Override
  public void pauseAll() throws JobPersistenceException {
    clusteredJobStore.pauseAll();
  }

  @Override
  public void pauseJob(final JobKey jobKey) throws JobPersistenceException {
    clusteredJobStore.pauseJob(jobKey);
  }

  @Override
  public Collection<String> pauseJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.pauseJobs(matcher);
  }

  @Override
  public void pauseTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    clusteredJobStore.pauseTrigger(triggerKey);
  }

  @Override
  public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.pauseTriggers(matcher);
  }

  @Override
  public void releaseAcquiredTrigger(final OperableTrigger trigger) {
    clusteredJobStore.releaseAcquiredTrigger(trigger);
  }

  @Override
  public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> triggers) throws JobPersistenceException {
    return clusteredJobStore.triggersFired(triggers);
  }

  @Override
  public boolean removeCalendar(String calName) throws JobPersistenceException {
    return clusteredJobStore.removeCalendar(calName);
  }

  @Override
  public boolean removeJob(JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.removeJob(jobKey);
  }

  @Override
  public boolean removeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.removeTrigger(triggerKey);
  }

  @Override
  public boolean removeJobs(List<JobKey> jobKeys) throws JobPersistenceException {
    return clusteredJobStore.removeJobs(jobKeys);
  }

  @Override
  public boolean removeTriggers(List<TriggerKey> triggerKeys) throws JobPersistenceException {
    return clusteredJobStore.removeTriggers(triggerKeys);
  }

  @Override
  public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace)
      throws JobPersistenceException {
    clusteredJobStore.storeJobsAndTriggers(triggersAndJobs, replace);
  }

  @Override
  public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger) throws JobPersistenceException {
    return clusteredJobStore.replaceTrigger(triggerKey, newTrigger);
  }

  @Override
  public void resumeAll() throws JobPersistenceException {
    clusteredJobStore.resumeAll();
  }

  @Override
  public void resumeJob(JobKey jobKey) throws JobPersistenceException {
    clusteredJobStore.resumeJob(jobKey);
  }

  @Override
  public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.resumeJobs(matcher);
  }

  @Override
  public void resumeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    clusteredJobStore.resumeTrigger(triggerKey);
  }

  @Override
  public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.resumeTriggers(matcher);
  }

  @Override
  public Calendar retrieveCalendar(String calName) throws JobPersistenceException {
    return clusteredJobStore.retrieveCalendar(calName);
  }

  @Override
  public JobDetail retrieveJob(JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.retrieveJob(jobKey);
  }

  @Override
  public OperableTrigger retrieveTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.retrieveTrigger(triggerKey);
  }

  @Override
  public boolean checkExists(final JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.checkExists(jobKey);
  }

  @Override
  public boolean checkExists(final TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.checkExists(triggerKey);
  }

  @Override
  public void clearAllSchedulingData() throws JobPersistenceException {
    clusteredJobStore.clearAllSchedulingData();
  }

  @Override
  public void schedulerStarted() throws SchedulerException {
    clusteredJobStore.schedulerStarted();
  }

  @Override
  public void schedulerPaused() {
      if (clusteredJobStore != null) {
          clusteredJobStore.schedulerPaused();
      }
  }

  @Override
  public void schedulerResumed() {
    clusteredJobStore.schedulerResumed();
  }

  @Override
  public void shutdown() {
    if (clusteredJobStore != null) {
      clusteredJobStore.shutdown();
    }
    if (updateCheckTimer != null) {
      updateCheckTimer.cancel();
    }
  }

  @Override
  public void storeCalendar(String name, Calendar calendar, boolean replaceExisting, boolean updateTriggers)
      throws JobPersistenceException {
    clusteredJobStore.storeCalendar(name, calendar, replaceExisting, updateTriggers);
  }

  @Override
  public void storeJob(JobDetail newJob, boolean replaceExisting) throws JobPersistenceException {
    clusteredJobStore.storeJob(newJob, replaceExisting);
  }

  @Override
  public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws JobPersistenceException {
    clusteredJobStore.storeJobAndTrigger(newJob, newTrigger);
  }

  @Override
  public void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting) throws JobPersistenceException {
    clusteredJobStore.storeTrigger(newTrigger, replaceExisting);
  }

  @Override
  public boolean supportsPersistence() {
    return true;
  }

  @Override
  public String toString() {
    return clusteredJobStore.toString();
  }

  @Override
  public void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail,
                                   CompletedExecutionInstruction triggerInstCode) {
    clusteredJobStore.triggeredJobComplete(trigger, jobDetail, triggerInstCode);
  }

  @Override
  public synchronized void setMisfireThreshold(long threshold) {
    ClusteredJobStore cjs = clusteredJobStore;
    if (cjs != null) {
      cjs.setMisfireThreshold(threshold);
    } else {
      misfireThreshold = Long.valueOf(threshold);
    }
  }

  @Override
  public synchronized void setEstimatedTimeToReleaseAndAcquireTrigger(long estimate) {
    ClusteredJobStore cjs = clusteredJobStore;
    if (cjs != null) {
      cjs.setEstimatedTimeToReleaseAndAcquireTrigger(estimate);
    } else {
      this.estimatedTimeToReleaseAndAcquireTrigger = estimate;
    }
  }

  @Override
  public void setInstanceId(String schedInstId) {
    this.schedInstanceId = schedInstId;
  }

  @Override
  public void setInstanceName(String schedName) {
    this.schedName = schedName;
  }

  @Override
  public void setTcRetryInterval(long retryInterval) {
    this.tcRetryInterval = retryInterval;
  }

  @Override
  public long getAcquireRetryDelay(int failureCount) {
    return tcRetryInterval;
  }

  @Override
  public String getUUID() {
    return toolkit.getClientUUID();
  }

  protected T createNewJobStoreInstance(String schedulerName, final boolean useSynchWrite) {
    return (T) new DefaultClusteredJobStore(useSynchWrite, toolkit, schedulerName);
  }

  @Override
  public long getEstimatedTimeToReleaseAndAcquireTrigger() {
    return clusteredJobStore.getEstimatedTimeToReleaseAndAcquireTrigger();
  }

  @Override
  public boolean isClustered() {
    return true;
  }

  protected T getClusteredJobStore() {
    return clusteredJobStore;
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public void jobToBeExecuted(final JobExecutionContext context) {
    //
  }

  @Override
  public void jobExecutionVetoed(final JobExecutionContext context) {
    //
  }

  @Override
  public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
    //
  }
}
