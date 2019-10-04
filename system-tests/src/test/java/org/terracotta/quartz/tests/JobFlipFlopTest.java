/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.terracotta.quartz.tests;

import com.tc.test.config.model.TestConfig;


import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

public class JobFlipFlopTest extends AbstractStandaloneTest {

  public JobFlipFlopTest(TestConfig testConfig) {
    super(testConfig, InstallClient.class, RemoveClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class InstallClient extends ClientBase {

    public InstallClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new InstallClient(args).run();
    }

    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      JobDetail job = JobBuilder.newJob().ofType(NullJob.class)
              .withIdentity("ConstantJobFlipFlopTest", "job").build();
      Trigger trigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0))
              .withIdentity("ConstantJobFlipFlopTest", "trigger").build();
      
      Assert.assertThat(scheduler.scheduleJob(job, trigger), IsNull.notNullValue());
      while (scheduler.checkExists(job.getKey())) {
        Thread.sleep(50);
      }
      Assert.assertThat(scheduler.scheduleJob(job, trigger), IsNull.notNullValue());
    }
  }

  public static class RemoveClient extends ClientBase {

    public RemoveClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new RemoveClient(args).run();
    }

    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      TriggerKey triggerKey = new TriggerKey("ConstantJobFlipFlopTest", "trigger");
      while (!scheduler.checkExists(triggerKey)) {
        Thread.sleep(50);
      }
      Assert.assertTrue(scheduler.unscheduleJob(triggerKey));
    }
  }
}
