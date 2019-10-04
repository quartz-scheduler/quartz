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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;

public class PausedTriggerClient1 extends ClientBase {

  public PausedTriggerClient1(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new PausedTriggerClient1(args).run();
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    String pausedGroup = "group";

    scheduler.pauseTriggers(GroupMatcher.triggerGroupEquals(pausedGroup));

    JobDetailImpl jobDetail = new JobDetailImpl("job1", "group", MyJob.class);
    jobDetail.setDurability(true);

    SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger1", pausedGroup);
    trigger.setRepeatInterval(30000);
    trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    trigger.setJobName("job1");
    trigger.setJobGroup("group");

    scheduler.addJob(jobDetail, false);
    scheduler.scheduleJob(trigger);
  }
  
  /** An empty job for testing purpose. */
  public static class MyJob implements Job {
      public void execute(JobExecutionContext context) throws JobExecutionException {
          //
      }
  }
}

