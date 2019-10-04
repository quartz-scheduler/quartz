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

import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.util.concurrent.CyclicBarrier;

public class Client1 extends ClientBase {

  public static final CyclicBarrier localBarrier = new CyclicBarrier(2);

  public static void main(String[] args) {
    new Client1(args).run();
  }

  public Client1(String args[]) {
    super(args);
  }

  @Override
  protected boolean isSynchWrite() {
    return true;
  }

  protected void test(Scheduler scheduler) throws Throwable {
    JobDetailImpl jobDetail = new JobDetailImpl("recoveryjob", null, RecoveryTestJob.class);
    jobDetail.setDurability(true);
    jobDetail.setRequestsRecovery(true);

    SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger1", "group");
    trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    trigger.setRepeatInterval(1000L * 60L * 60L * 24L * 7L);
    trigger.setJobName("recoveryjob");
    trigger.getJobDataMap().putAsString(RecoveryTest.class.getName(), true);

    scheduler.addJob(jobDetail, false);
    scheduler.scheduleJob(trigger);

    localBarrier.await();
    pass();
    Runtime.getRuntime().halt(0);
  }
}
