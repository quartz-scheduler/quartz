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
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;

public class StartStopStartClient extends ClientBase {
  public static final CyclicBarrier localBarrier = new CyclicBarrier(2);

  public StartStopStartClient(String[] args) {
    super(args);
  }

  @Override
  protected boolean isStartingScheduler() {
    return false;
  }

  @Override
  protected void test(Scheduler sched) throws Throwable {
    Properties props = getSchedulerProps();
    props.setProperty(StdSchedulerFactory.PROP_SCHED_IDLE_WAIT_TIME, "1000");

    sched.start();
    sched.shutdown();

    SchedulerFactory schedFact = new StdSchedulerFactory(props);
    sched = schedFact.getScheduler();
    sched.start();
    sched.shutdown();
  }

}
