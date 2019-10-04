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

package org.terracotta.quartz.wrappers;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import java.io.Serializable;

public class JobWrapper implements Serializable {
  protected JobDetail jobDetail;

  protected JobWrapper(JobDetail jobDetail) {
    this.jobDetail = jobDetail;
  }

  public JobKey getKey() {
    return this.jobDetail.getKey();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JobWrapper) {
      JobWrapper jw = (JobWrapper) obj;
      if (jw.jobDetail.getKey().equals(this.jobDetail.getKey())) { return true; }
    }

    return false;
  }

  @Override
  public int hashCode() {
    return jobDetail.getKey().hashCode();
  }

  @Override
  public String toString() {
    return "[" + jobDetail.toString() + "]";
  }

  public boolean requestsRecovery() {
    return jobDetail.requestsRecovery();
  }

  public boolean isPersistJobDataAfterExecution() {
    return jobDetail.isPersistJobDataAfterExecution();
  }
  
  public boolean isConcurrentExectionDisallowed() {
    return jobDetail.isConcurrentExectionDisallowed();
  }

  public boolean isDurable() {
    return jobDetail.isDurable();
  }

  public JobDetail getJobDetailClone() {
    return (JobDetail) jobDetail.clone();
  }

  public void setJobDataMap(JobDataMap newData, JobFacade jobFacade) {
    jobDetail = jobDetail.getJobBuilder().setJobData(newData).build();
    jobFacade.put(jobDetail.getKey(), this);
  }

  public JobDataMap getJobDataMapClone() {
    return (JobDataMap) jobDetail.getJobDataMap().clone();
  }

  public boolean isInstanceof(Class clazz) {
    return clazz.isInstance(jobDetail);
  }
}