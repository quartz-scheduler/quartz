/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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

import java.util.Date;

import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a mechanism
 * by which Quartz can reliably determine the current scheduling time.
 * </p>
 * 
 * <p>
 * In general, the default implementation of this interface (<code>{@link org.quartz.simpl.SimpleTimeBroker}</code>-
 * which simply uses <code>System.currentTimeMillis()</code>) is
 * sufficient. However situations may exist where this default scheme is
 * lacking in its robustness, or where tests need a controllable scheduling
 * clock. Infrastructure timing such as cluster check-ins, retry delays and
 * thread-pool waits continues to use elapsed wall-clock time.
 * </p>
 *
 * <p>
 * Implementations that change time discontinuously should do so while the
 * scheduler is in standby and resume it after the change. All nodes in a
 * cluster must observe the same scheduling time before they are resumed.
 * Implementations must be thread-safe because scheduler and job-store threads
 * can query them concurrently.
 * </p>
 * 
 * @see org.quartz.core.QuartzScheduler
 * @author James House
 */
public interface TimeBroker {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the current time, as known by the <code>TimeBroker</code>.
     * </p>
     * 
     * @throws SchedulerException
     *           if the current scheduling time cannot be obtained
     */
    Date getCurrentTime() throws SchedulerException;

    /**
     * <p>
     * Called by the QuartzScheduler before the <code>TimeBroker</code> is
     * used, in order to give the it a chance to initialize.
     * </p>
     */
    void initialize() throws SchedulerConfigException;

    /**
     * <p>
     * Called by the QuartzScheduler to inform the <code>TimeBroker</code>
     * that it should free up all of it's resources because the scheduler is
     * shutting down.
     * </p>
     */
    void shutdown();

}
