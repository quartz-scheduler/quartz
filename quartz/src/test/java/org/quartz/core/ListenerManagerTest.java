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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;
import static org.quartz.impl.matchers.NameMatcher.jobNameContains;

import java.util.List;
import java.util.UUID;


import org.junit.jupiter.api.Test;
import org.quartz.JobListener;
import org.quartz.SchedulerListener;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.NameMatcher;
/**
 * Test ListenerManagerImpl functionality 
 */
class ListenerManagerTest  {


    public static class TestJobListener implements JobListener {

        private final String name;
        
        public TestJobListener(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }

    public static class TestTriggerListener implements TriggerListener {

        private final String name;
        
        public TestTriggerListener(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
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

        SchedulerListener tl1 = new SchedulerListener() {
        };
        SchedulerListener tl2 = new SchedulerListener(){
        };

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
            listeners[i] = new SchedulerListener(){};
            manager.addSchedulerListener(listeners[i]);
        }
        List<SchedulerListener> mls = manager.getSchedulerListeners();
        int i = 0;
        for (SchedulerListener listener : mls) {
            assertSame(listeners[i], listener, "Unexpected order of listeners");
            i++;
        }
    }

}
