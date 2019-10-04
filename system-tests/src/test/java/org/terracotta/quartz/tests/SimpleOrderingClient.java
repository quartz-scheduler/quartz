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
package org.terracotta.quartz.tests;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleOrderingClient extends ClientBase {
  public static final int         NODE_COUNT = 5;
  public static ToolkitAtomicLong count;
  private final ToolkitBarrier    barrier;

  public SimpleOrderingClient(String[] args) {
    super(args);
    count = getClusteringToolkit().getAtomicLong("count");
    barrier = getClusteringToolkit().getBarrier("barrier", NODE_COUNT);
  }

  @Override
  public void addSchedulerProperties(Properties properties) {
    properties.setProperty(StdSchedulerFactory.PROP_SCHED_IDLE_WAIT_TIME, "1000");
  }

  @Override
  protected boolean isStartingScheduler() {
    return false;
  }

  @Override
  protected void test(Scheduler sched) throws Throwable {
    final long NUM = 25;

    int index = barrier.await();

    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    sched.getListenerManager().addJobListener(new JobListener() {

      public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (jobException != null) {
          jobException.getUnderlyingException().printStackTrace();
          error.set(jobException.getUnderlyingException());
        }
      }

      public void jobToBeExecuted(JobExecutionContext context) {
        //
      }

      public void jobExecutionVetoed(JobExecutionContext context) {
        //
      }

      public String getName() {
        return "listener";
      }
    }, EverythingMatcher.allJobs());

    sched.start();

    barrier.await();

    if (index == 0) {
      final long startBase = System.currentTimeMillis() + 10000L;

      List<Long> numbers = new ArrayList<Long>();
      for (long i = 0; i < NUM; i++) {
        numbers.add(i);
      }

      Collections.shuffle(numbers);
      for (long i : numbers) {
        JobDetail jobDetail = new JobDetailImpl("myJob" + i, null, TestJob.class);
        jobDetail.getJobDataMap().put("number", i);

        long startTime = startBase + (5000L * i);
        Trigger trigger = new SimpleTriggerImpl("trigger" + i, null, new Date(startTime), null, 0, 0L);

        System.err.println("Scheduling job " + i + " to run at " + new Date(startTime));
        sched.scheduleJob(jobDetail, trigger);
      }
    }

    barrier.await();

    if (index == 0) {
      int val;
      while ((val = count.intValue()) != NUM) {
        ThreadUtil.reallySleep(1000L);
        System.err.println("counter: " + val);
      }
    }

    barrier.await();

    sched.shutdown();

    if (error.get() != null) { throw error.get(); }
  }

  public static class TestJob implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
      long expect = context.getJobDetail().getJobDataMap().getLong("number");
      System.err.println("[" + new Date() + "]: expecting " + expect);

      long actual = count.getAndIncrement();

      if (expect != actual) { throw new JobExecutionException(new AssertionError(expect + " != " + actual)); }
    }
  }
}
