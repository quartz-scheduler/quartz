/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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

package org.quartz.examples.example16;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.RRuleScheduleBuilder.rruleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;

import org.quartz.JobDetail;
import org.quartz.RRuleTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example demonstrates scheduling jobs with RFC 5545 <code>RRULE</code>
 * (iCalendar recurrence rule) triggers &ndash; the RRULE analogue of
 * {@link org.quartz.examples.example3.CronTriggerExample}.
 *
 * <p>
 * Unlike a cron expression, an <code>RRULE</code> is evaluated relative to the
 * trigger's start-time, which plays the role of the RFC 5545 <code>DTSTART</code>
 * anchor. As with cron, the start-time is a lower bound: the start instant only
 * fires if it matches the rule.
 * </p>
 */
public class RRuleTriggerExample {

  public void run() throws Exception {
    Logger log = LoggerFactory.getLogger(RRuleTriggerExample.class);

    log.info("------- Initializing -------------------");

    // First we must get a reference to a scheduler
    SchedulerFactory sf = new StdSchedulerFactory();
    Scheduler sched = sf.getScheduler();

    log.info("------- Initialization Complete --------");

    log.info("------- Scheduling Jobs ----------------");

    // jobs can be scheduled before sched.start() has been called

    // job 1 will run every 20 seconds
    JobDetail job = newJob(SimpleJob.class).withIdentity("job1", "group1").build();

    RRuleTrigger trigger = newTrigger().withIdentity("trigger1", "group1")
        .withSchedule(rruleSchedule("FREQ=SECONDLY;INTERVAL=20")).build();

    Date ft = sched.scheduleJob(job, trigger);
    log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
             + trigger.getRRuleExpression());

    // job 2 will run at 10:00am every weekday (Monday through Friday)
    job = newJob(SimpleJob.class).withIdentity("job2", "group1").build();

    trigger = newTrigger().withIdentity("trigger2", "group1")
        .withSchedule(rruleSchedule("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=10;BYMINUTE=0;BYSECOND=0")).build();

    ft = sched.scheduleJob(job, trigger);
    log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
             + trigger.getRRuleExpression());

    // job 3 will run at 10:00pm on the last Friday of every month
    job = newJob(SimpleJob.class).withIdentity("job3", "group1").build();

    trigger = newTrigger().withIdentity("trigger3", "group1")
        .withSchedule(rruleSchedule("FREQ=MONTHLY;BYDAY=-1FR;BYHOUR=22;BYMINUTE=0;BYSECOND=0")).build();

    ft = sched.scheduleJob(job, trigger);
    log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
             + trigger.getRRuleExpression());

    // job 4 will run at 10:00am on the 1st and 15th days of every month
    job = newJob(SimpleJob.class).withIdentity("job4", "group1").build();

    trigger = newTrigger().withIdentity("trigger4", "group1")
        .withSchedule(rruleSchedule("FREQ=MONTHLY;BYMONTHDAY=1,15;BYHOUR=10;BYMINUTE=0;BYSECOND=0")).build();

    ft = sched.scheduleJob(job, trigger);
    log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
             + trigger.getRRuleExpression());

    // job 5 will run every 30 seconds but only on weekends (Saturday and Sunday)
    job = newJob(SimpleJob.class).withIdentity("job5", "group1").build();

    trigger = newTrigger().withIdentity("trigger5", "group1")
        .withSchedule(rruleSchedule("FREQ=SECONDLY;INTERVAL=30;BYDAY=SA,SU")).build();

    ft = sched.scheduleJob(job, trigger);
    log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
             + trigger.getRRuleExpression());

    // job 6 will run at noon, but only on the next 5 days (COUNT-bounded)
    job = newJob(SimpleJob.class).withIdentity("job6", "group1").build();

    trigger = newTrigger().withIdentity("trigger6", "group1")
        .withSchedule(rruleSchedule("FREQ=DAILY;COUNT=5;BYHOUR=12;BYMINUTE=0;BYSECOND=0")).build();

    ft = sched.scheduleJob(job, trigger);
    log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
             + trigger.getRRuleExpression());

    log.info("------- Starting Scheduler ----------------");

    // All of the jobs have been added to the scheduler, but none of the
    // jobs will run until the scheduler has been started
    sched.start();

    log.info("------- Started Scheduler -----------------");

    log.info("------- Waiting five minutes... ------------");
    try {
      // wait five minutes to show jobs
      Thread.sleep(300L * 1000L);
      // executing...
    } catch (Exception e) {
      //
    }

    log.info("------- Shutting Down ---------------------");

    sched.shutdown(true);

    log.info("------- Shutdown Complete -----------------");

    SchedulerMetaData metaData = sched.getMetaData();
    log.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");

  }

  public static void main(String[] args) throws Exception {

    RRuleTriggerExample example = new RRuleTriggerExample();
    example.run();
  }

}
