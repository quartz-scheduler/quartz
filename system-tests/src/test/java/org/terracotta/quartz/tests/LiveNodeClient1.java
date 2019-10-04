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
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class LiveNodeClient1 extends ClientBase {

  private final int listenPort;

  public LiveNodeClient1(String[] args) {
    super(args);
    this.listenPort = Integer.parseInt(System.getProperty("listenPort"));
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    log("Starting listening on " + listenPort);
    ServerSocket ss = new ServerSocket(listenPort);

    Socket socket = ss.accept();
    log("Got socket connection");

    JobDetailImpl jobDetail = new JobDetailImpl("testjob", null, SimpleJob.class);
    jobDetail.setDurability(true);

    SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger1", "group");
    trigger.setStartTime(new Date(System.currentTimeMillis() + 30000L));
    trigger.setJobName("testjob");

    scheduler.addJob(jobDetail, false);
    scheduler.scheduleJob(trigger);

    // not strictly necessary but this should make the newly added trigger get ACQUIRED locally
    Thread.sleep(5000L);

    log("Closing sockets");
    socket.close();
    ss.close();
  }

  private void log(String msg) {
    System.err.println("[" + new Date() + "] " + msg);
  }
}
