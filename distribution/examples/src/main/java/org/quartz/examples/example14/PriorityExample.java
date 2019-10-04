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
 package org.quartz.examples.example14;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This Example will demonstrate how Triggers are ordered by priority.
 */
public class PriorityExample {

  public void run() throws Exception {
    Logger log = LoggerFactory.getLogger(PriorityExample.class);

    log.info("------- Initializing ----------------------");

    // First we must get a reference to a scheduler
    SchedulerFactory sf = new StdSchedulerFactory("org/quartz/examples/example14/quartz_priority.properties");
    Scheduler sched = sf.getScheduler();

    log.info("------- Initialization Complete -----------");

    log.info("------- Scheduling Jobs -------------------");

    JobDetail job = newJob(TriggerEchoJob.class).withIdentity("TriggerEchoJob").build();

    // All three triggers will fire their first time at the same time,
    // ordered by their priority, and then repeat once, firing in a
    // staggered order that therefore ignores priority.
    //
    // We should see the following firing order:
    // 1. Priority10Trigger15SecondRepeat
    // 2. Priority5Trigger10SecondRepeat
    // 3. Priority1Trigger5SecondRepeat
    // 4. Priority1Trigger5SecondRepeat
    // 5. Priority5Trigger10SecondRepeat
    // 6. Priority10Trigger15SecondRepeat

    // Calculate the start time of all triggers as 5 seconds from now
    Date startTime = futureDate(5, IntervalUnit.SECOND);

    // First trigger has priority of 1, and will repeat after 5 seconds
    Trigger trigger1 = newTrigger().withIdentity("Priority1Trigger5SecondRepeat").startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(5)).withPriority(1).forJob(job).build();

    // Second trigger has default priority of 5 (default), and will repeat after 10 seconds
    Trigger trigger2 = newTrigger().withIdentity("Priority5Trigger10SecondRepeat").startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(10)).forJob(job).build();

    // Third trigger has priority 10, and will repeat after 15 seconds
    Trigger trigger3 = newTrigger().withIdentity("Priority10Trigger15SecondRepeat").startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(15)).withPriority(10).forJob(job)
        .build();

    // Tell quartz to schedule the job using our trigger
    sched.scheduleJob(job, trigger1);
    sched.scheduleJob(trigger2);
    sched.scheduleJob(trigger3);

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
    PriorityExample example = new PriorityExample();
    example.run();
  }
}
