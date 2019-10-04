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
 package org.terracotta.quartz.tests.spring;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SimpleSpringClient2 extends SpringClientBase {

  public SimpleSpringClient2(String[] args) {
    super("simple-spring-client2.xml", args);
  }

  public static void main(String[] args) {
    new SimpleSpringClient2(args).run();
  }

  @Override
  protected void test0(Scheduler scheduler) throws Throwable {
    // assert some things about the triggers and jobs that exist
    List<String> triggerGroupNames = scheduler.getTriggerGroupNames();
    if (triggerGroupNames.size() != 1) { throw new AssertionError("wrong number of trigger groups: "
                                                                  + Arrays.asList(triggerGroupNames).toString()); }

    Set<TriggerKey> triggerNames = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroupNames.get(0)));
    if (triggerNames.size() != 2) { throw new AssertionError("wrong number of triggers: " + Arrays.asList(triggerNames)); }

    assertTrigger(scheduler, "simpleTrigger", triggerGroupNames.get(0));
    assertTrigger(scheduler, "cronTrigger", triggerGroupNames.get(0));

    List<String> jobGroupNames = scheduler.getJobGroupNames();
    if (jobGroupNames.size() != 1) { throw new AssertionError("wrong number of job groups: "
                                                              + Arrays.asList(jobGroupNames).toString()); }

    assertJob(scheduler, "exampleJob", jobGroupNames.get(0));

    SimpleSpringClient1.localBarrier.await();
  }

  private void assertJob(Scheduler scheduler, String jobName, String group) throws SchedulerException {
    JobDetail jobDetail = scheduler.getJobDetail(new JobKey(jobName, group));
    if (jobDetail == null) { throw new AssertionError("missing job: " + jobName); }
  }

  private void assertTrigger(Scheduler scheduler, String triggerName, String group) throws SchedulerException {
    Trigger trigger = scheduler.getTrigger(new TriggerKey(triggerName, group));
    if (trigger == null) { throw new AssertionError("missing trigger: " + triggerName); }
  }
}
