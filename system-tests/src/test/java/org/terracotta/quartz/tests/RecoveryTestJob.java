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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RecoveryTestJob implements Job {

  public void execute(JobExecutionContext context) throws JobExecutionException {
    System.err.println("Hi There");

    if (context.getMergedJobDataMap().getString(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME) == null) {
      // If not recovering, just exit the VM
      try {
        Client1.localBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
      try {
        Thread.currentThread().join();
      } catch (InterruptedException ex) {
        throw new JobExecutionException(ex);
      }
    } else {
      assertThat(context.getMergedJobDataMap().getBooleanValue(RecoveryTest.class.getName()), is(true));
      try {
        Client2.localBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
    }
  }

}
