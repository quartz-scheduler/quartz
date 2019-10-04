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
 
package org.quartz.examples.example13;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * <p>
 * A dumb implementation of Job, for unit testing purposes.
 * </p>
 * 
 * @author James House
 */
public class SimpleRecoveryJob implements Job {

  private static Logger       _log  = LoggerFactory.getLogger(SimpleRecoveryJob.class);

  private static final String COUNT = "count";

  /**
   * Quartz requires a public empty constructor so that the scheduler can instantiate the class whenever it needs.
   */
  public SimpleRecoveryJob() {
  }

  /**
   * <p>
   * Called by the <code>{@link org.quartz.Scheduler}</code> when a <code>{@link org.quartz.Trigger}</code> fires that
   * is associated with the <code>Job</code>.
   * </p>
   * 
   * @throws JobExecutionException if there is an exception while executing the job.
   */
  public void execute(JobExecutionContext context) throws JobExecutionException {

    JobKey jobKey = context.getJobDetail().getKey();

    // if the job is recovering print a message
    if (context.isRecovering()) {
      _log.info("SimpleRecoveryJob: " + jobKey + " RECOVERING at " + new Date());
    } else {
      _log.info("SimpleRecoveryJob: " + jobKey + " starting at " + new Date());
    }

    // delay for ten seconds
    long delay = 10L * 1000L;
    try {
      Thread.sleep(delay);
    } catch (Exception e) {
      //
    }

    JobDataMap data = context.getJobDetail().getJobDataMap();
    int count;
    if (data.containsKey(COUNT)) {
      count = data.getInt(COUNT);
    } else {
      count = 0;
    }
    count++;
    data.put(COUNT, count);

    _log.info("SimpleRecoveryJob: " + jobKey + " done at " + new Date() + "\n Execution #" + count);

  }

}
