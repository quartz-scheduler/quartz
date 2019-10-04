/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.quartz.tests;

import org.junit.Assert;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 *
 * @author cdennis
 */
public class CrashingClient extends ClientBase {
  
  private static ThreadLocal<Scheduler> SCHEDULER = new ThreadLocal<Scheduler>();

  public CrashingClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    JobDetail job = new CleverJobDetail(JobBuilder.newJob(NullJob.class).withIdentity("job-name", "job-group").build());
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger-name", "trigger-group").startNow().withSchedule(SimpleScheduleBuilder.repeatSecondlyForever()).build();
    pass();
    SCHEDULER.set(scheduler);
    scheduler.scheduleJob(job, trigger);
    System.exit(-1);
  }

  @Override
  protected boolean isSynchWrite() {
    return true;
  }
  
  public static class CleverJobDetail implements JobDetail {

    private final JobDetail delegate;

    public CleverJobDetail(JobDetail delegate) {
      this.delegate = delegate;
    }

    @Override
    public JobKey getKey() {
      return delegate.getKey();
    }

    @Override
    public String getDescription() {
      return delegate.getDescription();
    }

    @Override
    public Class<? extends Job> getJobClass() {
      return delegate.getJobClass();
    }

    @Override
    public JobDataMap getJobDataMap() {
      return delegate.getJobDataMap();
    }

    @Override
    public boolean isDurable() {
      return delegate.isDurable();
    }

    @Override
    public boolean isPersistJobDataAfterExecution() {
      return delegate.isPersistJobDataAfterExecution();
    }

    @Override
    public boolean isConcurrentExectionDisallowed() {
      try {
        Assert.assertTrue(SCHEDULER.get().checkExists(new JobKey("job-name", "job-group")));
      } catch (SchedulerException e) {
        throw new AssertionError(e);
      }
      Runtime.getRuntime().halt(0);
      throw new AssertionError();
    }

    @Override
    public boolean requestsRecovery() {
      return delegate.requestsRecovery();
    }

    @Override
    public CleverJobDetail clone() {
      return new CleverJobDetail((JobDetail) delegate.clone());
    }

    @Override
    public JobBuilder getJobBuilder() {
      return delegate.getJobBuilder();
    }
  }
}
