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

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;

import org.junit.Assert;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class NonDurableJobRemovalTest extends AbstractStandaloneTest {

  public NonDurableJobRemovalTest(TestConfig testConfig) {
    super(testConfig, NonDurableJobClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  public static class NonDurableJobClient extends ClientBase {

    public NonDurableJobClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new NonDurableJobClient(args).run();
    }

    @Override
    public void addSchedulerProperties(Properties properties) {
      super.addSchedulerProperties(properties);
      properties.setProperty("org.quartz.threadPool.threadCount", "1");
    }

    
    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      
      scheduler.setJobFactory(new JobFactory() {

        @Override
        public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
          JobDetail jobDetail = bundle.getJobDetail();
          Class<? extends Job> jobClass = jobDetail.getJobClass();
          try {
            return jobClass.getConstructor(CyclicBarrier.class).newInstance(localBarrier);
          } catch (Exception e) {
            throw new SchedulerException(e);
          }
        }
      });
      
      JobDetail jobDetail = newJob(TestJob.class).withIdentity("job").build();
      Trigger trigger1 = newTrigger().withIdentity("trigger").build();
      scheduler.scheduleJob(jobDetail, trigger1);
    
      scheduler.start();
      localBarrier.await();
      while (scheduler.checkExists(trigger1.getKey())) {
        Thread.sleep(50);
      }

      Assert.assertFalse(scheduler.checkExists(jobDetail.getKey()));
      scheduler.shutdown(true);
    }
  }
  
  public static class TestJob implements Job {

    private final CyclicBarrier jobBarrier;
    
    public TestJob(CyclicBarrier jobBarrier) {
      this.jobBarrier = jobBarrier;
    }
    
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        jobBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
    }
  }
}
