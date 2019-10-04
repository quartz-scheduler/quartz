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
 package org.terracotta.quartz.tests.container;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BasicTestServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    Scheduler scheduler = null;

    try {
      StdSchedulerFactory factory = new StdSchedulerFactory("basic-quartz.properties");
      scheduler = factory.getScheduler();
      scheduler.start();

      JobDetailImpl jobDetail = new JobDetailImpl("testjob", null, BasicContainerTestJob.class);
      jobDetail.setDurability(true);

      SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger1", "group");
      trigger.setRepeatInterval(Long.MAX_VALUE);
      trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
      trigger.setJobName("testjob");

      scheduler.addJob(jobDetail, false);
      scheduler.scheduleJob(trigger);

      BasicContainerTestJob.localBarrier.await();

      findMBean();

      resp.getWriter().println("OK");
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (scheduler != null) {
        try {
          scheduler.shutdown();
        } catch (SchedulerException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private void findMBean() {
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectName> beans = mbs.queryNames(null, null);

    boolean found = false;
    for (ObjectName on : beans) {
      if (on.getDomain().equals("quartz")) {
        found = true;
        System.err.println(on);
      }
    }

    if (!found) { throw new AssertionError("Cannot find bean in set: " + beans); }

    // quartz:type=QuartzScheduler,name=DefaultQuartzScheduler,instance=TERRACOTTA_CLUSTERED_SCHEDULER,node=d4ebfa742e544ba2aef17de1d5deede8
  }

  public static class BasicContainerTestJob implements Job {

    public static final CyclicBarrier localBarrier = new CyclicBarrier(2);

    public void execute(JobExecutionContext jobexecutioncontext) throws JobExecutionException {
      try {
        localBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
    }

  }

}
