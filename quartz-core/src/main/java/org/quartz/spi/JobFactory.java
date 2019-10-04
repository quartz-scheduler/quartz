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
package org.quartz.spi;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * <p>
 * A JobFactory is responsible for producing instances of <code>Job</code>
 * classes.
 * </p>
 * 
 * <p>
 * This interface may be of use to those wishing to have their application
 * produce <code>Job</code> instances via some special mechanism, such as to
 * give the opertunity for dependency injection.
 * </p>
 * 
 * @see org.quartz.Scheduler#setJobFactory(JobFactory)
 * @see org.quartz.simpl.SimpleJobFactory
 * @see org.quartz.simpl.PropertySettingJobFactory
 * 
 * @author James House
 */
public interface JobFactory {

    /**
     * Called by the scheduler at the time of the trigger firing, in order to
     * produce a <code>Job</code> instance on which to call execute.
     * 
     * <p>
     * It should be extremely rare for this method to throw an exception -
     * basically only the the case where there is no way at all to instantiate
     * and prepare the Job for execution.  When the exception is thrown, the
     * Scheduler will move all triggers associated with the Job into the
     * <code>Trigger.STATE_ERROR</code> state, which will require human
     * intervention (e.g. an application restart after fixing whatever 
     * configuration problem led to the issue wih instantiating the Job. 
     * </p>
     * 
     * @param bundle
     *            The TriggerFiredBundle from which the <code>JobDetail</code>
     *            and other info relating to the trigger firing can be obtained.
     * @param scheduler a handle to the scheduler that is about to execute the job.
     * @throws SchedulerException if there is a problem instantiating the Job.
     * @return the newly instantiated Job
     */
    Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException;

}
