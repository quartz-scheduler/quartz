
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

package org.quartz;

/**
 * An exception that is thrown to indicate that a call to 
 * InterruptableJob.interrupt() failed without interrupting the Job.
 * 
 * @see org.quartz.InterruptableJob#interrupt()
 * 
 * @author James House
 */
public class UnableToInterruptJobException extends SchedulerException {
  
    private static final long serialVersionUID = -490863760696463776L;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a <code>UnableToInterruptJobException</code> with the given message.
     * </p>
     */
    public UnableToInterruptJobException(String msg) {
        super(msg);
    }
    
    /**
     * <p>
     * Create a <code>UnableToInterruptJobException</code> with the given cause.
     * </p>
     */
    public UnableToInterruptJobException(Throwable cause) {
        super(cause);
    }
    
}
