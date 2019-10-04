package org.quartz.management;

import org.quartz.core.QuartzScheduler;


/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


/**
 * Interface implemented by management servers.
 *
 * @author Ludovic Orban
 * @author brandony
 */
public interface ManagementServer {

    /**
     * Start the management server
     */
    public void start();

    /**
     * Stop the management server
     */
    public void stop();

    /**
     * Puts the submitted resource under the purview of this {@code ManagementServer}.
     *
     * @param managedResource the resource to be managed
     */
    public void register(QuartzScheduler managedResource);

    /**
     * Removes the submitted resource under the purview of this {@code ManagementServer}.
     *
     * @param managedResource the resource to be managed
     */
    public void unregister(QuartzScheduler managedResource);

    /**
     * Returns true if this {@code ManagementServer} has any resources registered.
     *
     * @return true if actively managing resources, false if not.
     */
    public boolean hasRegistered();

}
