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
 
package org.quartz.examples.example7;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;


/**
 * <p>
 * A dumb implementation of an InterruptableJob, for unit testing purposes.
 * </p>
 * 
 * @author <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 * @author Bill Kratzer
 */
public class DumbInterruptableJob implements InterruptableJob {
    
    // logging services
    private static Logger _log = LoggerFactory.getLogger(DumbInterruptableJob.class);
    
    // has the job been interrupted?
    private boolean _interrupted = false;

    // job name 
    private JobKey _jobKey = null;
    
    /**
     * <p>
     * Empty constructor for job initialization
     * </p>
     */
    public DumbInterruptableJob() {
    }


    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a <code>{@link org.quartz.Trigger}</code>
     * fires that is associated with the <code>Job</code>.
     * </p>
     * 
     * @throws JobExecutionException
     *           if there is an exception while executing the job.
     */
    public void execute(JobExecutionContext context)
        throws JobExecutionException {

        _jobKey = context.getJobDetail().getKey();
        _log.info("---- " + _jobKey + " executing at " + new Date());

        try {
            // main job loop... see the JavaDOC for InterruptableJob for discussion...
            // do some work... in this example we are 'simulating' work by sleeping... :)

            for (int i = 0; i < 4; i++) {
                try {
                    Thread.sleep(1000L);
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
                
                // periodically check if we've been interrupted...
                if(_interrupted) {
                    _log.info("--- " + _jobKey + "  -- Interrupted... bailing out!");
                    return; // could also choose to throw a JobExecutionException 
                             // if that made for sense based on the particular  
                             // job's responsibilities/behaviors
                }
            }
            
        } finally {
            _log.info("---- " + _jobKey + " completed at " + new Date());
        }
    }
    
    /**
     * <p>
     * Called by the <code>{@link Scheduler}</code> when a user
     * interrupts the <code>Job</code>.
     * </p>
     * 
     * @return void (nothing) if job interrupt is successful.
     * @throws JobExecutionException
     *           if there is an exception while interrupting the job.
     */
    public void interrupt() throws UnableToInterruptJobException {
        _log.info("---" + _jobKey + "  -- INTERRUPTING --");
        _interrupted = true;
    }

}
