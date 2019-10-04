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
 
package org.quartz.examples.example5;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import java.util.Date;

/**
 * <p>
 * A dumb implementation of Job, for unit testing purposes.
 * </p>
 * 
 * @author James House
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class StatefulDumbJob implements Job {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public static final String NUM_EXECUTIONS  = "NumExecutions";

  public static final String EXECUTION_DELAY = "ExecutionDelay";

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public StatefulDumbJob() {
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Called by the <code>{@link org.quartz.Scheduler}</code> when a <code>{@link org.quartz.Trigger}</code> fires that
   * is associated with the <code>Job</code>.
   * </p>
   * 
   * @throws JobExecutionException if there is an exception while executing the job.
   */
  public void execute(JobExecutionContext context) throws JobExecutionException {
    System.err.println("---" + context.getJobDetail().getKey() + " executing.[" + new Date() + "]");

    JobDataMap map = context.getJobDetail().getJobDataMap();

    int executeCount = 0;
    if (map.containsKey(NUM_EXECUTIONS)) {
      executeCount = map.getInt(NUM_EXECUTIONS);
    }

    executeCount++;

    map.put(NUM_EXECUTIONS, executeCount);

    long delay = 5000l;
    if (map.containsKey(EXECUTION_DELAY)) {
      delay = map.getLong(EXECUTION_DELAY);
    }

    try {
      Thread.sleep(delay);
    } catch (Exception ignore) {
      //
    }

    System.err.println("  -" + context.getJobDetail().getKey() + " complete (" + executeCount + ").");

  }

}
