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
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.impl.triggers.SimpleTriggerImpl;

public class BasicClient1 extends ClientBase {

  public BasicClient1(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new BasicClient1(args).run();
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    JobDetailImpl jobDetail = new JobDetailImpl("testjob", null, SimpleJob.class);
    jobDetail.setDurability(true);

    SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger1", "group");
    trigger.setRepeatInterval(30000);
    trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    trigger.setJobName("testjob");
    trigger.setCalendarName("mycal");

    // This calendar doesn't do anything really, just testing that calendars work
    scheduler.addCalendar("mycal", new BaseCalendar(), false, true);
    scheduler.addJob(jobDetail, false);
    scheduler.scheduleJob(trigger);

    SimpleJob.localBarrier.await();
  }
}
