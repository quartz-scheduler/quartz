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
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.terracotta.quartz.TerracottaJobStore;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.util.concurrent.ThreadUtil;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;

public class SimpleClient extends ClientBase {

  private final ToolkitBarrier      barrier;
  public static final CyclicBarrier localBarrier = new CyclicBarrier(2);

  public SimpleClient(String[] args) {
    super(args);
    barrier = getClusteringToolkit().getBarrier("barrier", 2);
  }

  @Override
  public void addSchedulerProperties(Properties props) {
    // these props might generate warning messages, but that is on purpose, they are here to make sure we don't throw
    // an exception (read: please don't remove)

    // Note by HUNG: if I don't comment these out, after porting from dso to express mode, this test will fail
    // props.setProperty("org.quartz.jobStore.tcConfig", "blah blah blah");
    // props.setProperty("org.quartz.jobStore.tcConfigUrl", "blah blah blah");

    // set synch write to make sure it doesn't blow up
    props.setProperty("org.quartz.jobStore.synchronousWrite", "false");

    // set AUTO instance ID (just to make sure it doesn't blow up)
    props.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);

    props.setProperty("org.quartz.threadPool.threadCount", "1");
    props.setProperty("org.quartz.jobStore.class", TerracottaJobStore.class.getName());
    props.setProperty(StdSchedulerFactory.PROP_SCHED_IDLE_WAIT_TIME, "1000");
  }

  @Override
  protected void test(Scheduler sched) throws Throwable {
    int index = barrier.await();

    if (index == 0) {
      JobDetailImpl jobDetail = new JobDetailImpl("testjob", null, TestJob.class);
      jobDetail.setDurability(true);

      long when = System.currentTimeMillis() + 5000L;

      SimpleTriggerImpl trigger1 = new SimpleTriggerImpl("trigger1", "group", new Date(when));
      trigger1.setJobName("testjob");
      SimpleTriggerImpl trigger2 = new SimpleTriggerImpl("trigger2", "group", new Date(when));
      trigger2.setJobName("testjob");

      sched.addJob(jobDetail, false);

      sched.scheduleJob(trigger1);
      sched.scheduleJob(trigger2);
    }

    localBarrier.await();
  }

  public static class TestJob implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
      System.err.println("Hi there");
      ThreadUtil.reallySleep(5000L);
      System.err.println("Done!");
      try {
        localBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
    }
  }
}
