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

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;

import java.util.List;

public class PausedTriggerClient2 extends ClientBase {

  public PausedTriggerClient2(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new PausedTriggerClient2(args).run();
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(new JobKey("job1", "group"));
    if (triggers.size() != 1) { throw new AssertionError(triggers); }
  }
}
