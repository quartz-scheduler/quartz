/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.quartz.tests;

import com.tc.test.config.model.TestConfig;

import org.junit.Assert;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 *
 * @author cdennis
 */
public class ClientCrashAtomicityTest extends AbstractStandaloneTest {

  public ClientCrashAtomicityTest(TestConfig testConfig) {
    super(testConfig, CrashingClient.class, ValidatingClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }
  
  public static class ValidatingClient extends ClientBase {

    public ValidatingClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      Assert.assertFalse(scheduler.checkExists(new JobKey("job-name", "job-group")));
      Assert.assertFalse(scheduler.checkExists(new TriggerKey("trigger-name", "trigger-group")));
      pass();
    }
    
    @Override
    protected boolean isSynchWrite() {
      return true;
    }
  }  
}