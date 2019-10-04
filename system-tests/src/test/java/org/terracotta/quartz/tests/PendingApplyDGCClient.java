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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.util.concurrent.ThreadUtil;

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

public class PendingApplyDGCClient extends ClientBase {
  public static final int      DGC_SECONDS       = 1;

  private final ToolkitBarrier barrier;
  private final AtomicInteger  index             = new AtomicInteger();

  static final CyclicBarrier   localBarrier      = new CyclicBarrier(2);
  static final AtomicInteger   fastJobsCompleted = new AtomicInteger();

  public PendingApplyDGCClient(String[] args) {
    super(args);
    barrier = getClusteringToolkit().getBarrier("barrier", 2);
  }

  @Override
  public void addSchedulerProperties(Properties properties) {
    properties.setProperty(StdSchedulerFactory.PROP_SCHED_IDLE_WAIT_TIME, "1000");

    try {
      index.set(barrier.await());
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    properties.setProperty("org.quartz.threadPool.threadCount", index.get() == 0 ? "1" : "10");
  }

  @Override
  protected boolean isStartingScheduler() {
    return false;
  }

  @Override
  protected void test(Scheduler sched) throws Throwable {
    if (index.get() == 0) {
      sched.start();
      JobDetailImpl jobDetail = new JobDetailImpl("blockjob", null, BlockJob.class);
      jobDetail.setDurability(true);

      SimpleTriggerImpl trigger = new SimpleTriggerImpl("blockTrigger", "group");
      trigger.setJobName("blockjob");

      sched.addJob(jobDetail, false);
      sched.scheduleJob(trigger);

      // Wait for the one scheduler thread to get tied up in the block job
      localBarrier.await();
    }

    barrier.await();

    if (index.get() != 0) {
      sched.start();

      JobDetailImpl jobDetail = new JobDetailImpl("fastjob", null, FastJob.class);
      jobDetail.setDurability(true);
      sched.addJob(jobDetail, false);

      // start lots of fast running jobs while the scheduler on node 0 is blocked
      for (int i = 0; i < 10; i++) {
        SimpleTriggerImpl trigger = new SimpleTriggerImpl("fastTrigger" + i, "group");
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        trigger.setRepeatInterval(1);
        trigger.setJobName("fastjob");
        sched.scheduleJob(trigger);
      }
    }

    barrier.await();

    ThreadUtil.reallySleep(DGC_SECONDS * 1000L * 10);

    if (index.get() == 0) {
      Assert.assertEquals(0, fastJobsCompleted.get());

      // trip local barrier again to get pending queue applied
      localBarrier.await();

      // Make sure some number of jobs actually run in node0
      // This is the best way I can find to detect if the scheduler was able to actually start running more jobs
      while (true) {
        int count = fastJobsCompleted.get();
        if (count < 100) {
          System.err.println("Only " + count + " fast jobs completed in node " + index);
          ThreadUtil.reallySleep(1000);
        } else {
          break;
        }
      }
    }

    barrier.await();

    sched.shutdown();
  }

  public static class FastJob implements Job {

    public void execute(JobExecutionContext context) {
      fastJobsCompleted.incrementAndGet();
    }
  }

  public static class BlockJob implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        localBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }

      try {
        localBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
    }
  }
}
