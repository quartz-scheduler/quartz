
/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025, 2026
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
 * The interface to be implemented by classes that want to be informed when a
 * cluster node fails in a clustered Quartz environment.
 *
 * <p>
 * In cluster mode, when a node fails (detected by missing heartbeats in the
 * database), registered ClusterListeners will be notified with the ID of the
 * failed instance.
 * </p>
 *
 * @see ListenerManager#addClusterListener(ClusterListener)
 * @see ListenerManager#removeClusterListener(String)
 */
public interface ClusterListener {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the name of the <code>ClusterListener</code>.
     * </p>
     */
    String getName();

    /**
     * <p>
     * Called by the <code>{@link Scheduler}</code> when a cluster node has been
     * detected as failed. This method is only invoked in clustered environments
     * when the ClusterManager detects that another scheduler instance in the
     * cluster has missed its check-in deadline.
     * </p>
     *
     * <p>
     * The <code>failedInstanceId</code> parameter contains the scheduler instance
     * ID of the node that has failed. This can be used to identify which node
     * in the cluster is no longer active.
     * </p>
     *
     * @param failedInstanceId the scheduler instance ID of the failed cluster node
     */
    void clusterNodeFailed(String failedInstanceId);

}
