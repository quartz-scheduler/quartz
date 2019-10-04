/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.integrations.tests;

import org.junit.Test;
import org.quartz.DateBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class QTZ283_IgnoreMisfirePolicyJdbcStore_Test extends QuartzDatabaseTestSupport {

  private static final long DURATION_OF_FIRST_SCHEDULING = 10L;
  private static final Logger LOG = LoggerFactory.getLogger(QTZ283_IgnoreMisfirePolicyJdbcStore_Test.class);
  private static final int INTERVAL_IN_SECONDS = 5;

  @Override
  protected Properties createSchedulerProperties() {
    Properties properties = new Properties();
    properties.put("org.quartz.scheduler.instanceName", "TestScheduler");
    properties.put("org.quartz.scheduler.instanceId", "AUTO");
    properties.put("org.quartz.scheduler.skipUpdateCheck", "true");
    properties.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.put("org.quartz.threadPool.threadCount", "12");
    properties.put("org.quartz.threadPool.threadPriority", "5");
    properties.put("org.quartz.jobStore.misfireThreshold", "10000");
    properties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
    properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
    properties.put("org.quartz.jobStore.useProperties", "true");
    properties.put("org.quartz.jobStore.dataSource", "myDS");
    properties.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
    properties.put("org.quartz.jobStore.isClustered", "false");
    properties.put("org.quartz.dataSource.myDS.driver", "org.apache.derby.jdbc.ClientDriver");
    properties.put("org.quartz.dataSource.myDS.URL", JdbcQuartzDerbyUtilities.DATABASE_CONNECTION_PREFIX);
    properties.put("org.quartz.dataSource.myDS.user", "quartz");
    properties.put("org.quartz.dataSource.myDS.password", "quartz");
    properties.put("org.quartz.dataSource.myDS.maxConnections", "5");
    return properties;
  }

  @Override
 	protected void afterSchedulerInit() throws Exception {


    LOG.info("------- Scheduling Job  -------------------");

    // define the jobs and tie them to our HelloJob class
    JobDetail job1 = newJob(HelloJob.class).withIdentity("job1", "group1").build();

    // trigger should have started the even minute before now
    // due to its ignore policy, it will be triggered
    Date startTime1 = DateBuilder.evenMinuteDateBefore(null);
    SimpleTrigger oldtriggerMisfirePolicyIgnore = newTrigger()
        .withIdentity("trigger1", "group1")
        .startAt(startTime1)
        .withSchedule(
            simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
                .repeatForever()
                .withMisfireHandlingInstructionIgnoreMisfires())
        .build();

    if (scheduler.checkExists(job1.getKey())) {
      // the job already exists in jdbcjobstore; let's reschedule it
      scheduler.rescheduleJob(oldtriggerMisfirePolicyIgnore.getKey(), oldtriggerMisfirePolicyIgnore);
    } else {
      scheduler.scheduleJob(job1, oldtriggerMisfirePolicyIgnore);
    }

    // Start up the scheduler (nothing can actually run until the
    // scheduler has been started)
    scheduler.start();

    LOG.info("------- Scheduler Started -----------------");

    // wait long enough so that the scheduler as an opportunity to
    // run the job!
    try {
      Thread.sleep(DURATION_OF_FIRST_SCHEDULING * 1000L);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void checkOldTriggerGetsFired() throws SQLException {
    BigDecimal misfirePolicyIgnoreTimesTriggered = JdbcQuartzDerbyUtilities.timesTriggered("trigger1", "group1");
    assertThat("The old trigger has never been fired, even if the policy is ignore", misfirePolicyIgnoreTimesTriggered,
        not(equalTo(BigDecimal.ZERO)));
  }
}
