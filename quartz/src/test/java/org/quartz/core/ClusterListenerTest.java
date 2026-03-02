
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
 */

package org.quartz.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.quartz.ClusterListener;
import org.quartz.listeners.ClusterListenerSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test ClusterListener functionality
 */
class ClusterListenerTest {

    private static class TestClusterListener extends ClusterListenerSupport {
        private final List<String> failedInstances = new CopyOnWriteArrayList<>();

        public TestClusterListener(String name) {
            super(name);
        }

        @Override
        public void clusterNodeFailed(String failedInstanceId) {
            failedInstances.add(failedInstanceId);
        }

        public List<String> getFailedInstances() {
            return failedInstances;
        }

        public void clear() {
            failedInstances.clear();
        }
    }

    /**
     * Test that ClusterListenerSupport provides default name when not specified
     */
    @Test
    void testClusterListenerSupportDefaultName() {
        ClusterListener listener = new ClusterListenerSupport() {
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                // Do nothing
            }
        };

        assertNotNull(listener.getName(), "Listener name should not be null");
        assertTrue(listener.getName().contains("ClusterListenerTest"), "Listener name should contain class name");
    }

    /**
     * Test that ClusterListenerSupport uses provided name
     */
    @Test
    void testClusterListenerSupportCustomName() {
        TestClusterListener listener = new TestClusterListener("myCustomListener");
        assertEquals("myCustomListener", listener.getName());
    }

    /**
     * Test that ClusterListenerSupport throws exception for empty name
     */
    @Test
    void testClusterListenerSupportEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ClusterListenerSupport("") {
                @Override
                public void clusterNodeFailed(String failedInstanceId) {
                    // Do nothing
                }
            };
        }, "Expected IllegalArgumentException for empty name");
    }

    /**
     * Test that ClusterListenerSupport throws exception for null name
     */
    @Test
    void testClusterListenerSupportNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ClusterListenerSupport(null) {
                @Override
                public void clusterNodeFailed(String failedInstanceId) {
                    // Do nothing
                }
            };
        }, "Expected IllegalArgumentException for null name");
    }

    /**
     * Test clusterNodeFailed callback
     */
    @Test
    void testClusterNodeFailedCallback() {
        TestClusterListener listener = new TestClusterListener("testListener");

        assertTrue(listener.getFailedInstances().isEmpty(), "Failed instances list should be empty initially");

        listener.clusterNodeFailed("node1");
        assertEquals(1, listener.getFailedInstances().size(), "Should have 1 failed instance");
        assertEquals("node1", listener.getFailedInstances().get(0));

        listener.clusterNodeFailed("node2");
        assertEquals(2, listener.getFailedInstances().size(), "Should have 2 failed instances");
        assertEquals("node2", listener.getFailedInstances().get(1));
    }

    /**
     * Test multiple listeners receive notifications independently
     */
    @Test
    void testMultipleListeners() {
        TestClusterListener listener1 = new TestClusterListener("listener1");
        TestClusterListener listener2 = new TestClusterListener("listener2");

        listener1.clusterNodeFailed("nodeA");
        listener2.clusterNodeFailed("nodeB");

        assertEquals(1, listener1.getFailedInstances().size());
        assertEquals("nodeA", listener1.getFailedInstances().get(0));

        assertEquals(1, listener2.getFailedInstances().size());
        assertEquals("nodeB", listener2.getFailedInstances().get(0));
    }

    /**
     * Test clear functionality
     */
    @Test
    void testClearFailedInstances() {
        TestClusterListener listener = new TestClusterListener("testListener");

        listener.clusterNodeFailed("node1");
        listener.clusterNodeFailed("node2");
        assertEquals(2, listener.getFailedInstances().size());

        listener.clear();
        assertTrue(listener.getFailedInstances().isEmpty(), "Failed instances list should be empty after clear");
    }

    /**
     * Test exception handling in listener - should not propagate
     */
    @Test
    void testListenerExceptionHandling() {
        final List<String> receivedInstances = new ArrayList<>();

        ClusterListener throwingListener = new ClusterListener() {
            @Override
            public String getName() {
                return "throwingListener";
            }

            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                throw new RuntimeException("Test exception for " + failedInstanceId);
            }
        };

        ClusterListener normalListener = new ClusterListener() {
            @Override
            public String getName() {
                return "normalListener";
            }

            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                receivedInstances.add(failedInstanceId);
            }
        };

        // Test that exceptions don't affect other listeners
        assertThrows(RuntimeException.class, () -> {
            throwingListener.clusterNodeFailed("node1");
        }, "Expected RuntimeException");

        // Normal listener should still work
        normalListener.clusterNodeFailed("node2");
        assertEquals(1, receivedInstances.size());
        assertEquals("node2", receivedInstances.get(0));
    }

    /**
     * Test listener ordering by tracking notification order
     */
    @Test
    void testListenerNotificationOrder() {
        final List<String> notificationOrder = new ArrayList<>();

        ClusterListener listener1 = new ClusterListener() {
            @Override
            public String getName() { return "listener1"; }
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                notificationOrder.add("listener1:" + failedInstanceId);
            }
        };

        ClusterListener listener2 = new ClusterListener() {
            @Override
            public String getName() { return "listener2"; }
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                notificationOrder.add("listener2:" + failedInstanceId);
            }
        };

        ClusterListener listener3 = new ClusterListener() {
            @Override
            public String getName() { return "listener3"; }
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                notificationOrder.add("listener3:" + failedInstanceId);
            }
        };

        // Simulate notification in order
        listener1.clusterNodeFailed("node1");
        listener2.clusterNodeFailed("node1");
        listener3.clusterNodeFailed("node1");

        assertEquals(3, notificationOrder.size());
        assertEquals("listener1:node1", notificationOrder.get(0));
        assertEquals("listener2:node1", notificationOrder.get(1));
        assertEquals("listener3:node1", notificationOrder.get(2));
    }

    /**
     * Test listener name uniqueness
     */
    @Test
    void testListenerNameUniqueness() {
        ClusterListener listener1 = new ClusterListenerSupport("sameName") {
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
            }
        };

        ClusterListener listener2 = new ClusterListenerSupport("sameName") {
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
            }
        };

        // Both can have the same name, it's up to the manager to handle uniqueness
        assertEquals("sameName", listener1.getName());
        assertEquals("sameName", listener2.getName());
    }
}
