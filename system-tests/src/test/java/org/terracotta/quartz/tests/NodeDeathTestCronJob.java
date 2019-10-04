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
import org.quartz.impl.JobDetailImpl;

public class NodeDeathTestCronJob implements Job {

  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobDetailImpl jobDetail = ((JobDetailImpl) context.getJobDetail());
    System.err.println("running " + jobDetail.getFullName() + "...");
    try {
      Thread.sleep(3000L);
    } catch (InterruptedException ie) {
      throw new JobExecutionException(ie);
    }
    System.err.println("--> done running " + jobDetail.getFullName());

    NodeDeathTestClient1.run.add(jobDetail.getName());
  }
}
