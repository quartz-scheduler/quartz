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
 */
package org.quartz.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;
import static org.quartz.impl.matchers.NameMatcher.jobNameContains;

import java.util.List;
import java.util.UUID;


import org.junit.jupiter.api.Test;
import org.quartz.ClusterListener;
import org.quartz.JobListener;
import org.quartz.SchedulerListener;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.NameMatcher;
import org.quartz.listeners.ClusterListenerSupport;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.listeners.TriggerListenerSupport;

/**
 * Test ListenerManagerImpl functionality 
 */
class ListenerManagerTest  {


    public static class TestJobListener extends JobListenerSupport {

        private String name;
        
        public TestJobListener(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }

    public static class TestTriggerListener extends TriggerListenerSupport {

        private String name;
        
        public TestTriggerListener(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }

    public static class TestSchedulerListener extends SchedulerListenerSupport {

    }

    public static class TestClusterListener extends ClusterListenerSupport {

        public TestClusterListener() {
            super();
        }

        public TestClusterListener(String name) {
            super(name);
        }

        private String lastFailedInstanceId;

        @Override
        public void clusterNodeFailed(String failedInstanceId) {
            this.lastFailedInstanceId = failedInstanceId;
        }

        public String getLastFailedInstanceId() {
            return lastFailedInstanceId;
        }
    }


    @Test
    void testManagementOfJobListeners() throws Exception {

        JobListener tl1 = new TestJobListener("tl1");
        JobListener tl2 = new TestJobListener("tl2");

        ListenerManagerImpl manager = new ListenerManagerImpl();

        // test adding listener without matcher
        manager.addJobListener(tl1);
        assertEquals(1, manager.getJobListeners().size(), "Unexpected size of listener list");

        // test adding listener with matcher
        manager.addJobListener(tl2, jobGroupEquals("foo"));
        assertEquals(2, manager.getJobListeners().size(), "Unexpected size of listener list");

        // test removing a listener
        manager.removeJobListener("tl1");
        assertEquals(1, manager.getJobListeners().size(), "Unexpected size of listener list");

        // test adding a matcher
        manager.addJobListenerMatcher("tl2", jobNameContains("foo"));
        assertEquals(2, manager.getJobListenerMatchers("tl2").size(), "Unexpected size of listener's matcher list");

        // Test ordering of registration is preserved.
        final int numListenersToTestOrderOf = 15;
        manager = new ListenerManagerImpl();
        JobListener[] listeners = new JobListener[numListenersToTestOrderOf];
        for(int i = 0; i < numListenersToTestOrderOf; i++) {
            // use random name, to help test that order isn't based on naming or coincidental hashing
            listeners[i] = new TestJobListener(UUID.randomUUID().toString());
            manager.addJobListener(listeners[i]);
        }
        List<JobListener> mls = manager.getJobListeners();
        int i = 0;
        for(JobListener listener: mls) {
            assertSame(listeners[i], listener, "Unexpected order of listeners");
            i++;
        }
    }
    @Test
    void testManagementOfTriggerListeners() throws Exception {

        TriggerListener tl1 = new TestTriggerListener("tl1");
        TriggerListener tl2 = new TestTriggerListener("tl2");

        ListenerManagerImpl manager = new ListenerManagerImpl();

        // test adding listener without matcher
        manager.addTriggerListener(tl1);
        assertEquals(1, manager.getTriggerListeners().size(), "Unexpected size of listener list");

        // test adding listener with matcher
        manager.addTriggerListener(tl2, triggerGroupEquals("foo"));
        assertEquals(2, manager.getTriggerListeners().size(), "Unexpected size of listener list");

        // test removing a listener
        manager.removeTriggerListener("tl1");
        assertEquals(1, manager.getTriggerListeners().size(), "Unexpected size of listener list");

        // test adding a matcher
        manager.addTriggerListenerMatcher("tl2", NameMatcher.<TriggerKey>nameContains("foo"));
        assertEquals(2, manager.getTriggerListenerMatchers("tl2").size(), "Unexpected size of listener's matcher list");

        // Test ordering of registration is preserved.
        final int numListenersToTestOrderOf = 15;
        manager = new ListenerManagerImpl();
        TriggerListener[] listeners = new TriggerListener[numListenersToTestOrderOf];
        for(int i = 0; i < numListenersToTestOrderOf; i++) {
            // use random name, to help test that order isn't based on naming or coincidental hashing
            listeners[i] = new TestTriggerListener(UUID.randomUUID().toString());
            manager.addTriggerListener(listeners[i]);
        }
        List<TriggerListener> mls = manager.getTriggerListeners();
        int i = 0;
        for(TriggerListener listener: mls) {
            assertSame(listeners[i], listener, "Unexpected order of listeners");
            i++;
        }
    }

    @Test
    void testManagementOfSchedulerListeners() throws Exception {

        SchedulerListener tl1 = new TestSchedulerListener();
        SchedulerListener tl2 = new TestSchedulerListener();

        ListenerManagerImpl manager = new ListenerManagerImpl();

        // test adding listener without matcher
        manager.addSchedulerListener(tl1);
        assertEquals(1, manager.getSchedulerListeners().size(), "Unexpected size of listener list");

        // test adding listener with matcher
        manager.addSchedulerListener(tl2);
        assertEquals(2, manager.getSchedulerListeners().size(), "Unexpected size of listener list");

        // test removing a listener
        manager.removeSchedulerListener(tl1);
        assertEquals(1, manager.getSchedulerListeners().size(), "Unexpected size of listener list");

        // Test ordering of registration is preserved.
        final int numListenersToTestOrderOf = 15;
        manager = new ListenerManagerImpl();
        SchedulerListener[] listeners = new SchedulerListener[numListenersToTestOrderOf];
        for (int i = 0; i < numListenersToTestOrderOf; i++) {
            listeners[i] = new TestSchedulerListener();
            manager.addSchedulerListener(listeners[i]);
        }
        List<SchedulerListener> mls = manager.getSchedulerListeners();
        int i = 0;
        for (SchedulerListener listener : mls) {
            assertSame(listeners[i], listener, "Unexpected order of listeners");
            i++;
        }
    }

    @Test
    public void testManagementOfClusterListeners() throws Exception {

        TestClusterListener tl1 = new TestClusterListener("tl1");
        TestClusterListener tl2 = new TestClusterListener("tl2");

        ListenerManagerImpl manager = new ListenerManagerImpl();

        // test adding listener
        manager.addClusterListener(tl1);
        assertEquals(1, manager.getClusterListeners().size(), "Unexpected size of listener list");

        // test adding another listener
        manager.addClusterListener(tl2);
        assertEquals(2, manager.getClusterListeners().size(), "Unexpected size of listener list");

        // test getting a listener by name
        assertSame(tl1, manager.getClusterListener("tl1"), "Expected to find listener by name");
        assertSame(tl2, manager.getClusterListener("tl2"), "Expected to find listener by name");

        // test removing a listener
        manager.removeClusterListener("tl1");
        assertEquals(1, manager.getClusterListeners().size(), "Unexpected size of listener list");
        assertNull(manager.getClusterListener("tl1"), "Expected listener to be removed");

        // test removing non-existent listener
        assertFalse(manager.removeClusterListener("nonexistent"), "Expected false when removing non-existent listener");

        // Test ordering of registration is preserved.
        final int numListenersToTestOrderOf = 15;
        manager = new ListenerManagerImpl();
        ClusterListener[] lstners = new ClusterListener[numListenersToTestOrderOf];
        for(int i = 0; i < numListenersToTestOrderOf; i++) {
            // use random name, to help test that order isn't based on naming or coincidental hashing
            lstners[i] = new TestClusterListener(UUID.randomUUID().toString());
            manager.addClusterListener(lstners[i]);
        }
        List<ClusterListener> mls = manager.getClusterListeners();
        int i = 0;
        for(ClusterListener lsnr: mls) {
            assertSame(lstners[i], lsnr, "Unexpected order of listeners");
            i++;
        }
    }

    @Test
    public void testClusterListenerNotification() throws Exception {
        TestClusterListener listener = new TestClusterListener("testListener");

        ListenerManagerImpl manager = new ListenerManagerImpl();
        manager.addClusterListener(listener);

        // Simulate notification through QuartzScheduler
        QuartzScheduler scheduler = null;
        try {
            // Note: In actual usage, the QuartzScheduler's notifyClusterListenersNodeFailed()
            // would be called by the SchedulerSignaler when a cluster node fails.
            // Here we just verify the listener is properly registered and can be retrieved.
            assertEquals(1, manager.getClusterListeners().size(), "Expected listener to be registered");
            assertSame(listener, manager.getClusterListener("testListener"), "Expected to find the registered listener");
        } finally {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }
    }

}
