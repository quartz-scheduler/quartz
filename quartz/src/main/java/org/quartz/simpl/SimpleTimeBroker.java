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

package org.quartz.simpl;

import java.util.Date;

import org.quartz.SchedulerConfigException;
import org.quartz.spi.TimeBroker;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a mechanism
 * by which the <code>{@link org.quartz.core.QuartzScheduler}</code> can
 * reliably determine the current time.
 * </p>
 * 
 * <p>
 * In general, the default implementation of this interface (<code>{@link org.quartz.simpl.SimpleTimeBroker}</code>-
 * which simply uses <code>System.currentTimeMillis()</code>) is sufficient.
 * Applications may provide a different implementation when scheduling
 * decisions need to use a controllable clock.
 * </p>
 * 
 * @see org.quartz.core.QuartzScheduler
 * 
 * @author James House
 */
public class SimpleTimeBroker implements TimeBroker {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the current time, simply using <code>new Date()</code>.
     * </p>
     */
    public Date getCurrentTime() {
        return new Date();
    }

    public void initialize() throws SchedulerConfigException {
        // do nothing...
    }

    public void shutdown() {
        // do nothing...
    }

}
