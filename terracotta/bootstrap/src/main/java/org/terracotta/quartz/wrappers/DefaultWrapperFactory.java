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

import org.quartz.JobDetail;
import org.quartz.spi.OperableTrigger;

public class DefaultWrapperFactory implements WrapperFactory {

  @Override
  public JobWrapper createJobWrapper(JobDetail jobDetail) {
    return new JobWrapper(jobDetail);
  }

  @Override
  public TriggerWrapper createTriggerWrapper(OperableTrigger trigger, boolean jobDisallowsConcurrence) {
    return new TriggerWrapper(trigger, jobDisallowsConcurrence);
  }

}
