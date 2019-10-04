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
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.quartz.AbstractTerracottaJobStore;
import org.terracotta.quartz.TerracottaJobStore;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;

import java.io.IOException;
import java.util.Properties;

public abstract class ClientBase extends AbstractClientBase {

  private final Properties props = new Properties();
  private Toolkit          toolkit;

  public ClientBase(String args[]) {
    super(args);
  }

  @Override
  public void doTest() throws Throwable {
    Scheduler scheduler = null;
    try {
      scheduler = setupScheduler();
      test(scheduler);
    } finally {
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdown();
      }
    }
  }

  public void addSchedulerProperties(Properties properties) {
    // to be overridden
  }

  public Properties getSchedulerProps() {
    return props;
  }

  public Properties getToolkitProps() {
    return new Properties();
  }

  protected Scheduler setupScheduler() throws IOException, SchedulerException {
    props.load(getClass().getResourceAsStream("/org/quartz/quartz.properties"));
    props.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, TerracottaJobStore.class.getName());
    props.setProperty(AbstractTerracottaJobStore.TC_CONFIGURL_PROP, getTerracottaUrl());
    props.setProperty("org.quartz.jobStore.synchronousWrite", String.valueOf(isSynchWrite()));
    props.setProperty("org.quartz.jobStore.estimatedTimeToReleaseAndAcquireTrigger", "10");
    props.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);

    addSchedulerProperties(props);
    
    System.out.println(props);
    SchedulerFactory schedFact = new StdSchedulerFactory(props);
    Scheduler sched = schedFact.getScheduler();
    if (isStartingScheduler()) {
      sched.start();
    }

    return sched;
  }

  protected boolean isStartingScheduler() {
    return true;
  }

  protected boolean isSynchWrite() {
    return false;
  }

  protected abstract void test(Scheduler scheduler) throws Throwable;

  protected synchronized Toolkit getClusteringToolkit() {
    if (toolkit == null) {
      toolkit = createToolkit();
    }
    return toolkit;
  }

  private Toolkit createToolkit() {
    try {
      return ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl(), getToolkitProps());
    } catch (ToolkitInstantiationException e) {
      throw new RuntimeException(e);
    }
  }
}
