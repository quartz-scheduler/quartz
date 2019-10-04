
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
 * The interface to be implemented by <code>{@link Job}s</code> that provide a 
 * mechanism for having their execution interrupted.  It is NOT a requirement
 * for jobs to implement this interface - in fact, for most people, none of
 * their jobs will.
 * 
 * <p>Interrupting a <code>Job</code> is very analogous in concept and 
 * challenge to normal interruption of a <code>Thread</code> in Java. 
 * 
 * <p>
 * The means of actually interrupting the Job must be implemented within the
 * <code>Job</code> itself (the <code>interrupt()</code> method of this 
 * interface is simply a means for the scheduler to inform the <code>Job</code>
 * that a request has been made for it to be interrupted). The mechanism that
 * your jobs use to interrupt themselves might vary between implementations.
 * However the principle idea in any implementation should be to have the
 * body of the job's <code>execute(..)</code> periodically check some flag to
 * see if an interruption has been requested, and if the flag is set, somehow
 * abort the performance of the rest of the job's work.  An example of 
 * interrupting a job can be found in the java source for the  class 
 * <code>org.quartz.examples.DumbInterruptableJob</code>.  It is legal to use
 * some combination of <code>wait()</code> and <code>notify()</code> 
 * synchronization within <code>interrupt()</code> and <code>execute(..)</code>
 * in order to have the <code>interrupt()</code> method block until the
 * <code>execute(..)</code> signals that it has noticed the set flag.
 * </p>
 * 
 * <p>
 * If the Job performs some form of blocking I/O or similar functions, you may
 * want to consider having the <code>Job.execute(..)</code> method store a
 * reference to the calling <code>Thread</code> as a member variable.  Then the
 * Implementation of this interfaces <code>interrupt()</code> method can call 
 * <code>interrupt()</code> on that Thread.   Before attempting this, make
 * sure that you fully understand what <code>java.lang.Thread.interrupt()</code> 
 * does and doesn't do.  Also make sure that you clear the Job's member 
 * reference to the Thread when the execute(..) method exits (preferably in a
 * <code>finally</code> block.
 * </p>
 * 
 * <p>
 * See Example 7 (org.quartz.examples.example7.DumbInterruptableJob) for a simple
 * implementation demonstration.
 * </p>
 * @see Job
 * @see StatefulJob
 * @see Scheduler#interrupt(JobKey)
 * @see Scheduler#interrupt(String)
 * 
 * @author James House
 */
public interface InterruptableJob extends Job {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Called by the <code>{@link Scheduler}</code> when a user
     * interrupts the <code>Job</code>.
     * </p>
     * 
     * @throws UnableToInterruptJobException
     *           if there is an exception while interrupting the job.
     */
    void interrupt()
        throws UnableToInterruptJobException;
}
