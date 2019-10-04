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

import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class NodeDeathTestClient1 extends ClientBase {

  public static final Set<String> run = Collections.synchronizedSet(new HashSet<String>());

  private final int               listenPort;

  public NodeDeathTestClient1(String[] args) {
    super(args);
    this.listenPort = Integer.parseInt(System.getProperty("listenPort"));
  }

  @Override
  protected void test(Scheduler sched) throws Throwable {
    log("Starting listening on " + listenPort);
    ServerSocket ss = new ServerSocket(listenPort);

    JobDetailImpl jobDetail1 = new JobDetailImpl("testjob1", null, NodeDeathTestCronJob.class);
    jobDetail1.setDurability(true);

    JobDetailImpl jobDetail2 = new JobDetailImpl("testjob2", null, NodeDeathTestCronJob.class);
    jobDetail2.setDurability(true);

    AbstractTrigger trigger1 = new CronTriggerImpl("trigger1", "group", "0/10 * * * * ?");
    trigger1.setJobName("testjob1");
    AbstractTrigger trigger2 = new CronTriggerImpl("trigger2", "group", "0/10 * * * * ?");
    trigger2.setJobName("testjob2");

    sched.addJob(jobDetail1, false);
    sched.addJob(jobDetail2, false);

    sched.scheduleJob(trigger1);
    sched.scheduleJob(trigger2);

    AbstractTrigger simpleTrigger = new SimpleTriggerImpl("simpleJob", "simpleGroup",
                                                          SimpleTrigger.REPEAT_INDEFINITELY, 15000);
    simpleTrigger.setJobName("simpleJob");
    simpleTrigger.setJobGroup("simpleGroup");
    JobDetailImpl simpleJob = new JobDetailImpl("simpleJob", "simpleGroup", NodeDeathTestSimpleJob.class);
    simpleJob.setDurability(true);

    sched.addJob(simpleJob, false);
    sched.scheduleJob(simpleTrigger);

    Socket socket = ss.accept();
    log("Got socket connection");

    // let both nodes compete for jobs for a while
    Thread.sleep(25000);

    log("Closing sockets");
    socket.close();
    ss.close();

    // let the other client die and for this node to be notified
    Thread.sleep(15000);

    run.clear();

    Set<String> expect = new HashSet<String>();
    expect.add("testjob1");
    expect.add("testjob2");
    expect.add("simpleJob");

    boolean okay = false;
    long end = System.currentTimeMillis() + 60000L;
    while (System.currentTimeMillis() < end) {
      if (run.equals(expect)) {
        okay = true;
        break;
      }
      Thread.sleep(1000L);
    }

    if (!okay) { throw new AssertionError("run set is: " + run); }
  }

  private void log(String msg) {
    System.err.println("[" + new Date() + "] " + msg);
  }
}
