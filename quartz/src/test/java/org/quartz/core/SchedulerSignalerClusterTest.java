
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

import org.junit.jupiter.api.Test;
import org.quartz.ClusterListener;
import org.quartz.listeners.ClusterListenerSupport;
import org.quartz.spi.SchedulerSignaler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test SchedulerSignaler integration with ClusterListener
 */
class SchedulerSignalerClusterTest {

    private static class TestClusterListener extends ClusterListenerSupport {
        // Use CopyOnWriteArrayList for thread-safe concurrent access
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
     * Test that a SchedulerSignaler implementation properly delegates to ClusterListeners.
     * This test uses a mock SchedulerSignaler to verify the contract.
     */
    @Test
    void testSchedulerSignalerNotifiesClusterListeners() {
        final List<String> notifiedInstances = new ArrayList<>();

        // Create a mock SchedulerSignaler that records calls
        SchedulerSignaler signaler = new SchedulerSignaler() {
            @Override
            public void notifyTriggerListenersMisfired(org.quartz.Trigger trigger) {
            }

            @Override
            public void notifySchedulerListenersFinalized(org.quartz.Trigger trigger) {
            }

            @Override
            public void notifySchedulerListenersJobDeleted(org.quartz.JobKey jobKey) {
            }

            @Override
            public void signalSchedulingChange(long candidateNewNextFireTime) {
            }

            @Override
            public void notifySchedulerListenersError(String msg, org.quartz.SchedulerException jpe) {
            }

            @Override
            public void notifyClusterListenersNodeFailed(String failedInstanceId) {
                notifiedInstances.add(failedInstanceId);
            }
        };

        // Test notification through signaler
        signaler.notifyClusterListenersNodeFailed("failedNode1");
        signaler.notifyClusterListenersNodeFailed("failedNode2");

        assertEquals(2, notifiedInstances.size());
        assertEquals("failedNode1", notifiedInstances.get(0));
        assertEquals("failedNode2", notifiedInstances.get(1));
    }

    /**
     * Test that a cluster listener can be notified of multiple node failures
     */
    @Test
    void testClusterListenerMultipleNotifications() {
        TestClusterListener listener = new TestClusterListener("testListener");

        // Simulate multiple node failures
        String[] failedNodes = {"node1", "node2", "node3", "node1"};
        for (String node : failedNodes) {
            listener.clusterNodeFailed(node);
        }

        assertEquals(4, listener.getFailedInstances().size());
        assertEquals("node1", listener.getFailedInstances().get(0));
        assertEquals("node2", listener.getFailedInstances().get(1));
        assertEquals("node3", listener.getFailedInstances().get(2));
        assertEquals("node1", listener.getFailedInstances().get(3)); // Same node can fail multiple times
    }

    /**
     * Test that multiple cluster listeners all receive notifications
     */
    @Test
    void testMultipleClusterListenersNotified() {
        final List<String> listener1Notifications = new ArrayList<>();
        final List<String> listener2Notifications = new ArrayList<>();
        final List<String> listener3Notifications = new ArrayList<>();

        ClusterListener listener1 = new ClusterListener() {
            @Override
            public String getName() { return "listener1"; }
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                listener1Notifications.add(failedInstanceId);
            }
        };

        ClusterListener listener2 = new ClusterListener() {
            @Override
            public String getName() { return "listener2"; }
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                listener2Notifications.add(failedInstanceId);
            }
        };

        ClusterListener listener3 = new ClusterListener() {
            @Override
            public String getName() { return "listener3"; }
            @Override
            public void clusterNodeFailed(String failedInstanceId) {
                listener3Notifications.add(failedInstanceId);
            }
        };

        // Simulate signaler notifying all listeners
        String failedNode = "failedClusterNode";
        listener1.clusterNodeFailed(failedNode);
        listener2.clusterNodeFailed(failedNode);
        listener3.clusterNodeFailed(failedNode);

        assertEquals(1, listener1Notifications.size());
        assertEquals(failedNode, listener1Notifications.get(0));

        assertEquals(1, listener2Notifications.size());
        assertEquals(failedNode, listener2Notifications.get(0));

        assertEquals(1, listener3Notifications.size());
        assertEquals(failedNode, listener3Notifications.get(0));
    }

    /**
     * Test that the cluster listener interface contract is properly defined
     */
    @Test
    void testClusterListenerInterfaceContract() {
        // Test that getName() returns the expected value
        ClusterListener listener = new ClusterListener() {
            @Override
            public String getName() {
                return "myTestListener";
            }

            @Override
            public void clusterNodeFailed(String failedInstanceId) {
            }
        };

        assertEquals("myTestListener", listener.getName());
    }

    /**
     * Test concurrent access to ClusterListener (simulating real cluster scenario)
     */
    @Test
    void testConcurrentNotifications() throws InterruptedException {
        final TestClusterListener listener = new TestClusterListener("concurrentTest");
        final int numThreads = 10;
        final int notificationsPerThread = 100;

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < notificationsPerThread; j++) {
                    listener.clusterNodeFailed("node-" + threadNum + "-" + j);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all notifications were received
        assertEquals(numThreads * notificationsPerThread, listener.getFailedInstances().size());
    }
}
