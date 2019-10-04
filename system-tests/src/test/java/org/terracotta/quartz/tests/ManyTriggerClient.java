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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

public class ManyTriggerClient extends ClientBase {

  private final ToolkitBarrier       barrier;
  private final static AtomicInteger counter = new AtomicInteger(0);

  public ManyTriggerClient(String[] args) {
    super(args);
    barrier = getClusteringToolkit().getBarrier("barrier", 2);
  }

  @Override
  protected boolean isStartingScheduler() {
    return false;
  }

  @Override
  protected void test(Scheduler sched) throws Throwable {
    int index = barrier.await();

    int triggers = 1000;
    long now = System.currentTimeMillis() + 5000L;
    if (index == 0) {
      sched.start();
      barrier.await();

      final int duration = 90;
      while (counter.get() < triggers
             && System.currentTimeMillis() < now + (triggers / 10 * 500) + TimeUnit.SECONDS.toMillis(duration)) {
        Thread.sleep(1500);
        System.out.println(new Date() + " - Waiting on another " + (triggers - counter.get()) + " triggers to fire");
      }

      Assert.assertEquals("All " + triggers + " triggers should have fired by now (" + new Date() + ")", triggers,
                          counter.get());

    } else {
      barrier.await();

      JobDetail jobDetail = newJob(MyJob.class).withIdentity("testJob").storeDurably().build();

      sched.addJob(jobDetail, false);

      for (int i = 0; i < triggers; i++) {
        Trigger trigger = newTrigger().forJob("testJob").withIdentity("trigger" + i, "group")
            .startAt(new Date(now + ((i / (triggers / 10)) + 1) * 500))
            .withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow()).build();
        sched.scheduleJob(trigger);
      }
    }

    if (index == 0) {
      sched.shutdown();
    }
  }

  public static class MyJob implements Job {
    public void execute(final JobExecutionContext context) {
      try {
        System.err.println(new Date() + " - " + context.getTrigger().getKey().getGroup() + ": Hi there");
        ManyTriggerClient.counter.incrementAndGet();
      } catch (Exception e) {
        e.printStackTrace(System.err);
        throw new RuntimeException(e);
      }
    }
  }
}
