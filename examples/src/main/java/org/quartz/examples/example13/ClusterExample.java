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
 
package org.quartz.examples.example13;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to test/show the clustering features of JDBCJobStore (JobStoreTX or JobStoreCMT).
 * <p>
 * All instances MUST use a different properties file, because their instance Ids must be different, however all other
 * properties should be the same.
 * </p>
 * <p>
 * If you want it to clear out existing jobs and triggers, pass a command-line argument called "clearJobs".
 * </p>
 * <p>
 * You should probably start with a "fresh" set of tables (assuming you may have some data lingering in it from other
 * tests), since mixing data from a non-clustered setup with a clustered one can be bad.
 * </p>
 * <p>
 * Try killing one of the cluster instances while they are running, and see that the remaining instance(s) recover the
 * in-progress jobs. Note that detection of the failure may take up to 15 or so seconds with the default settings.
 * </p>
 * <p>
 * Also try running it with/without the shutdown-hook plugin registered with the scheduler.
 * (org.quartz.plugins.management.ShutdownHookPlugin).
 * </p>
 * <p>
 * <i>Note:</i> Never run clustering on separate machines, unless their clocks are synchronized using some form of
 * time-sync service (such as an NTP daemon).
 * </p>
 *
 * Configure the instance1.properties file and the instance2.properties
 * file as necesary (see the "Configuration" section below for details).
 *
 * This example uses a database to maintain scheduling information in a
 * clustered environment.   You will need to first install the Quartz
 * database tables.  SQL table creation scripts are included with the Quartz
 * distribution for many popular database platforms.
 *
 * You will need a JDBC Driver for your database. The example uses Postgres to demonstrate
 * You can download Postgres JDBC driver here http://jdbc.postgresql.org
 * Just put the jar under "lib" folder of the Quartz distribution
 *
 * After you have installed the database scripts, you will need to
 * configure both properties file so that Quartz knows how to connect to
 * your database.
 *
 * The following parameters need to be set: (this shows a PostgreSQL example)
 *
 * <pre>
 * org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
 * org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
 * org.quartz.jobStore.useProperties=false
 * org.quartz.jobStore.dataSource=myDS
 * org.quartz.jobStore.tablePrefix=QRTZ_
 * org.quartz.jobStore.isClustered=true
 *
 * org.quartz.dataSource.myDS.driver = org.postgresql.Driver
 * org.quartz.dataSource.myDS.URL = jdbc:postgresql://localhost:5432/quartz
 * org.quartz.dataSource.myDS.user = quartz
 * org.quartz.dataSource.myDS.password = quartz
 * org.quartz.dataSource.myDS.maxConnections = 5
 * org.quartz.dataSource.myDS.validationQuery=
 * </pre>
 *
 * @see SimpleRecoveryJob
 * @see SimpleRecoveryStatefulJob
 * @author James House
 */
public class ClusterExample {

  private static Logger _log = LoggerFactory.getLogger(ClusterExample.class);

  public void run(boolean inClearJobs, boolean inScheduleJobs) throws Exception {

    // First we must get a reference to a scheduler
    SchedulerFactory sf = new StdSchedulerFactory();
    Scheduler sched = sf.getScheduler();

    if (inClearJobs) {
      _log.warn("***** Deleting existing jobs/triggers *****");
      sched.clear();
    }

    _log.info("------- Initialization Complete -----------");

    if (inScheduleJobs) {

      _log.info("------- Scheduling Jobs ------------------");

      String schedId = sched.getSchedulerInstanceId();

      int count = 1;

      JobDetail job = newJob(SimpleRecoveryJob.class).withIdentity("job_" + count, schedId) // put triggers in group
                                                                                            // named after the cluster
                                                                                            // node instance just to
                                                                                            // distinguish (in logging)
                                                                                            // what was scheduled from
                                                                                            // where
          .requestRecovery() // ask scheduler to re-execute this job if it was in progress when the scheduler went
                             // down...
          .build();

      SimpleTrigger trigger = newTrigger().withIdentity("triger_" + count, schedId)
          .startAt(futureDate(1, IntervalUnit.SECOND))
          .withSchedule(simpleSchedule().withRepeatCount(20).withIntervalInSeconds(5)).build();

      _log.info(job.getKey() + " will run at: " + trigger.getNextFireTime() + " and repeat: "
                + trigger.getRepeatCount() + " times, every " + trigger.getRepeatInterval() / 1000 + " seconds");
      sched.scheduleJob(job, trigger);

      count++;

      job = newJob(SimpleRecoveryJob.class).withIdentity("job_" + count, schedId) // put triggers in group named after
                                                                                  // the cluster node instance just to
                                                                                  // distinguish (in logging) what was
                                                                                  // scheduled from where
          .requestRecovery() // ask scheduler to re-execute this job if it was in progress when the scheduler went
                             // down...
          .build();

      trigger = newTrigger().withIdentity("triger_" + count, schedId).startAt(futureDate(2, IntervalUnit.SECOND))
          .withSchedule(simpleSchedule().withRepeatCount(20).withIntervalInSeconds(5)).build();

      _log.info(job.getKey() + " will run at: " + trigger.getNextFireTime() + " and repeat: "
                + trigger.getRepeatCount() + " times, every " + trigger.getRepeatInterval() / 1000 + " seconds");
      sched.scheduleJob(job, trigger);

      count++;

      job = newJob(SimpleRecoveryStatefulJob.class).withIdentity("job_" + count, schedId) // put triggers in group named
                                                                                          // after the cluster node
                                                                                          // instance just to
                                                                                          // distinguish (in logging)
                                                                                          // what was scheduled from
                                                                                          // where
          .requestRecovery() // ask scheduler to re-execute this job if it was in progress when the scheduler went
                             // down...
          .build();

      trigger = newTrigger().withIdentity("triger_" + count, schedId).startAt(futureDate(1, IntervalUnit.SECOND))
          .withSchedule(simpleSchedule().withRepeatCount(20).withIntervalInSeconds(3)).build();

      _log.info(job.getKey() + " will run at: " + trigger.getNextFireTime() + " and repeat: "
                + trigger.getRepeatCount() + " times, every " + trigger.getRepeatInterval() / 1000 + " seconds");
      sched.scheduleJob(job, trigger);

      count++;

      job = newJob(SimpleRecoveryJob.class).withIdentity("job_" + count, schedId) // put triggers in group named after
                                                                                  // the cluster node instance just to
                                                                                  // distinguish (in logging) what was
                                                                                  // scheduled from where
          .requestRecovery() // ask scheduler to re-execute this job if it was in progress when the scheduler went
                             // down...
          .build();

      trigger = newTrigger().withIdentity("triger_" + count, schedId).startAt(futureDate(1, IntervalUnit.SECOND))
          .withSchedule(simpleSchedule().withRepeatCount(20).withIntervalInSeconds(4)).build();

      _log.info(job.getKey() + " will run at: " + trigger.getNextFireTime() + " & repeat: " + trigger.getRepeatCount()
                + "/" + trigger.getRepeatInterval());
      sched.scheduleJob(job, trigger);

      count++;

      job = newJob(SimpleRecoveryJob.class).withIdentity("job_" + count, schedId) // put triggers in group named after
                                                                                  // the cluster node instance just to
                                                                                  // distinguish (in logging) what was
                                                                                  // scheduled from where
          .requestRecovery() // ask scheduler to re-execute this job if it was in progress when the scheduler went
                             // down...
          .build();

      trigger = newTrigger().withIdentity("triger_" + count, schedId).startAt(futureDate(1, IntervalUnit.SECOND))
          .withSchedule(simpleSchedule().withRepeatCount(20).withIntervalInMilliseconds(4500L)).build();

      _log.info(job.getKey() + " will run at: " + trigger.getNextFireTime() + " & repeat: " + trigger.getRepeatCount()
                + "/" + trigger.getRepeatInterval());
      sched.scheduleJob(job, trigger);
    }

    // jobs don't start firing until start() has been called...
    _log.info("------- Starting Scheduler ---------------");
    sched.start();
    _log.info("------- Started Scheduler ----------------");

    _log.info("------- Waiting for one hour... ----------");
    try {
      Thread.sleep(3600L * 1000L);
    } catch (Exception e) {
      //
    }

    _log.info("------- Shutting Down --------------------");
    sched.shutdown();
    _log.info("------- Shutdown Complete ----------------");
  }

  public static void main(String[] args) throws Exception {
    boolean clearJobs = false;
    boolean scheduleJobs = true;

    for (String arg : args) {
      if (arg.equalsIgnoreCase("clearJobs")) {
        clearJobs = true;
      } else if (arg.equalsIgnoreCase("dontScheduleJobs")) {
        scheduleJobs = false;
      }
    }

    ClusterExample example = new ClusterExample();
    example.run(clearJobs, scheduleJobs);
  }
}
