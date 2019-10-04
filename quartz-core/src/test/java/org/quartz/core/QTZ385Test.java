/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JdbcQuartzTestUtilities;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.integrations.tests.HelloJob;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;

/**
 *
 * @author cdennis
 */
public class QTZ385Test {
  
  private static final Method TRIGGERS_FIRED;
  static {
    try {
      TRIGGERS_FIRED = JobStore.class.getDeclaredMethod("triggersFired", new Class[] {List.class});
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testShutdownOrdering() throws SchedulerException, SQLException, InterruptedException, BrokenBarrierException {
    JdbcQuartzTestUtilities.createDatabase("testShutdownOrdering");
    try {
      final CyclicBarrier barrier = new CyclicBarrier(2);
      final JobStoreTX realJobStore = new JobStoreTX();
      realJobStore.setDataSource("testShutdownOrdering");
      realJobStore.setInstanceId("SINGLE_NODE_TEST");
      realJobStore.setInstanceName("testShutdownOrdering");

      JobStore evilJobStore = (JobStore) Proxy.newProxyInstance(JobStore.class.getClassLoader(), new Class[] {JobStore.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          if (TRIGGERS_FIRED.equals(method)) {
            Object result = method.invoke(realJobStore, args);
            barrier.await();
            try {
              barrier.await(1, TimeUnit.SECONDS);
            } catch (Exception e) {
              //ignore
            }
            return result;
          } else {
            return method.invoke(realJobStore, args);
          }
        }
      });

      DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
      factory.createScheduler(new SimpleThreadPool(1, Thread.NORM_PRIORITY), evilJobStore);
      Scheduler scheduler = factory.getScheduler();
      try {
        scheduler.scheduleJob(JobBuilder.newJob(HelloJob.class).withIdentity("test").requestRecovery().build(), 
                TriggerBuilder.newTrigger().withIdentity("test").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(1)).build());
        scheduler.start();
        barrier.await();
      } finally {
        scheduler.shutdown(true);
      }

      try {
        barrier.await(1, TimeUnit.SECONDS);
      } catch (Exception e) {
        //ignore
      }
      
      
      final AtomicBoolean recoveredJob = new AtomicBoolean(false);
      factory.createScheduler(new SimpleThreadPool(1, Thread.NORM_PRIORITY), realJobStore);
      Scheduler recovery = factory.getScheduler();
      try {
        recovery.getListenerManager().addJobListener(new JobListenerSupport() {

          @Override
          public String getName() {
            return QTZ385Test.class.getSimpleName();
          }

          @Override
          public void jobToBeExecuted(JobExecutionContext context) {
            if (context.isRecovering()) {
              recoveredJob.set(true);
            }
          }
        });
        recovery.start();
        Thread.sleep(1000);
        Assert.assertFalse(recoveredJob.get());
      } finally {
        recovery.shutdown(true);
      }
    } finally {
      JdbcQuartzTestUtilities.destroyDatabase("testShutdownOrdering");
    }
  }
  
  
}
