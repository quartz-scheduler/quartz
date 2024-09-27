/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
 package org.quartz.examples.example15;

import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.examples.example14.TriggerEchoJob;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * This Example will demonstrate how run a NativeJob.
 *
 * Please see the note int the NativeJob javadoc about security concerns.
 */
public class NativeJobExample {

  public void run() throws Exception {
    Logger log = LoggerFactory.getLogger(NativeJobExample.class);

    log.info("------- Initializing ----------------------");

    // First we must get a reference to a scheduler
    SchedulerFactory sf = new StdSchedulerFactory();
    Scheduler sched = sf.getScheduler();

    log.info("------- Initialization Complete -----------");

    log.info("------- Scheduling Jobs -------------------");

    JobDetail job = newJob(NativeJob.class).withIdentity("MyNativeJob").build();
    job.getJobDataMap().put(NativeJob.PROP_COMMAND, "echo");
    job.getJobDataMap().put(NativeJob.PROP_PARAMETERS, "\"ran the native command and captured the stdout.\"");
    job.getJobDataMap().put(NativeJob.PROP_CONSUME_STREAMS, true);

    // Calculate the start time of all triggers as 5 seconds from now
    Date startTime = futureDate(5, IntervalUnit.SECOND);

    // First trigger has priority of 1, and will repeat after 5 seconds
    Trigger trigger1 = newTrigger().withIdentity("TriggerWith5SecondRepeat").startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(5)).forJob(job).build();

    // Tell quartz to schedule the job using our trigger
    sched.scheduleJob(job, trigger1);

    // Start up the scheduler (nothing can actually run until the
    // scheduler has been started)
    sched.start();
    log.info("------- Started Scheduler -----------------");

    // wait long enough so that the scheduler as an opportunity to
    // fire the triggers
    log.info("------- Waiting 30 seconds... -------------");
    try {
      Thread.sleep(30L * 1000L);
      // executing...
    } catch (Exception e) {
      //
    }

    // shut down the scheduler
    log.info("------- Shutting Down ---------------------");
    sched.shutdown(true);
    log.info("------- Shutdown Complete -----------------");
  }

  public static void main(String[] args) throws Exception {
    NativeJobExample example = new NativeJobExample();
    example.run();
  }
}
