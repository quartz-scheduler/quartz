
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

package org.quartz.listeners;

import org.quartz.ClusterListener;

/**
 * A helpful abstract base class for implementors of
 * <code>{@link ClusterListener}</code>.
 *
 * <p>
 * The implementations of this class only override methods they are interested
 * in receiving, while adhering to the contract of
 * <code>{@link ClusterListener}</code>.
 * </p>
 *
 * @see ClusterListener
 */
public abstract class ClusterListenerSupport implements ClusterListener {

    private final String name;

    /**
     * Constructor with a default name.
     */
    protected ClusterListenerSupport() {
        this.name = getClass().getName() + "_" + System.currentTimeMillis();
    }

    /**
     * Constructor with a specified name.
     *
     * @param name the name of the listener
     */
    protected ClusterListenerSupport(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Listener name cannot be null or empty");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void clusterNodeFailed(String failedInstanceId) {
        // do nothing by default
    }
}
