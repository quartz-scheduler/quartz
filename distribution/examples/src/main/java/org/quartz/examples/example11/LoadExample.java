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
 
package org.quartz.examples.example11;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example will spawn a large number of jobs to run
 * 
 * @author James House, Bill Kratzer
 */
public class LoadExample {

  private int _numberOfJobs = 500;

  public LoadExample(int inNumberOfJobs) {
    _numberOfJobs = inNumberOfJobs;
  }

  public void run() throws Exception {
    Logger log = LoggerFactory.getLogger(LoadExample.class);

    // First we must get a reference to a scheduler
    SchedulerFactory sf = new StdSchedulerFactory();
    Scheduler sched = sf.getScheduler();

    log.info("------- Initialization Complete -----------");

    // schedule 500 jobs to run
    for (int count = 1; count <= _numberOfJobs; count++) {
      JobDetail job = newJob(SimpleJob.class).withIdentity("job" + count, "group_1").requestRecovery() // ask scheduler
                                                                                                       // to re-execute
                                                                                                       // this job if it
                                                                                                       // was in
                                                                                                       // progress when
                                                                                                       // the scheduler
                                                                                                       // went down...
          .build();

      // tell the job to delay some small amount... to simulate work...
      long timeDelay = (long) (java.lang.Math.random() * 2500);
      job.getJobDataMap().put(SimpleJob.DELAY_TIME, timeDelay);

      Trigger trigger = newTrigger().withIdentity("trigger_" + count, "group_1")
          .startAt(futureDate((10000 + (count * 100)), IntervalUnit.MILLISECOND)) // space fire times a small bit
          .build();

      sched.scheduleJob(job, trigger);
      if (count % 25 == 0) {
        log.info("...scheduled " + count + " jobs");
      }
    }

    log.info("------- Starting Scheduler ----------------");

    // start the schedule
    sched.start();

    log.info("------- Started Scheduler -----------------");

    log.info("------- Waiting five minutes... -----------");

    // wait five minutes to give our jobs a chance to run
    try {
      Thread.sleep(300L * 1000L);
    } catch (Exception e) {
      //
    }

    // shut down the scheduler
    log.info("------- Shutting Down ---------------------");
    sched.shutdown(true);
    log.info("------- Shutdown Complete -----------------");

    SchedulerMetaData metaData = sched.getMetaData();
    log.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
  }

  public static void main(String[] args) throws Exception {

    int numberOfJobs = 500;
    if (args.length == 1) {
      numberOfJobs = Integer.parseInt(args[0]);
    }
    if (args.length > 1) {
      System.out.println("Usage: java " + LoadExample.class.getName() + "[# of jobs]");
      return;
    }
    LoadExample example = new LoadExample(numberOfJobs);
    example.run();
  }

}
