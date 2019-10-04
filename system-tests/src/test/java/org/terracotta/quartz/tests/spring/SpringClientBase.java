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

import org.quartz.Scheduler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.terracotta.quartz.tests.ClientBase;

public abstract class SpringClientBase extends ClientBase {

  private final String         contextFile;
  protected ApplicationContext context;

  public SpringClientBase(String contextFile, String[] args) {
    super(args);
    this.contextFile = contextFile;
  }

  @Override
  protected final Scheduler setupScheduler() {
    // Set this to get it substituted in the context file
    System.setProperty("tcConfigUrl", getTerracottaUrl());

    // using an application context so that it will be set in the JobDetailBean instances
    context = new ClassPathXmlApplicationContext(contextFile, getClass());

    return (Scheduler) context.getBean("scheduler");

  }

  @Override
  protected final void test(Scheduler scheduler) throws Throwable {

    try {
      test0(scheduler);
    } finally {
      if (context.containsBean("taskExecutor")) {
        DisposableBean exec = (DisposableBean) context.getBean("taskExecutor");
        exec.destroy();
      }
    }
  }

  protected abstract void test0(Scheduler scheduler) throws Throwable;

}
