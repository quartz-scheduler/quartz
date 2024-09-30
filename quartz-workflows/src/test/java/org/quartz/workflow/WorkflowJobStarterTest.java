/*
 * Copyright 2023 Dimitry Polivaev, Unite
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quartz.workflow;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.EverythingMatcher;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowJobStarterTest {
    @Mock private Scheduler scheduler;
    @Mock private ListenerManager listenerManager;
    final WorkflowJobStarter uut = WorkflowJobStarter.INSTANCE;
    
    @Before
    public void setup() throws Exception {
        when(scheduler.getSchedulerName()).thenReturn("scheduler");
        when(scheduler.getListenerManager()).thenReturn(listenerManager);
    }
    
    @After
    public void reset() {
        uut.removeAllSchedulers();
    }
    
    @Test
    public void installsJobStarterAsListener() throws Exception {
        uut.addScheduler(scheduler);
        verify(listenerManager).addJobListener(uut, EverythingMatcher.allJobs());
    }

    @Test
    public void installsJobStarterAsListenerIdempotent() throws Exception {
        uut.addScheduler(scheduler);
        uut.addScheduler(scheduler);
        verify(listenerManager, times(1)).addJobListener(uut, EverythingMatcher.allJobs());
    }

    @Test
    public void uninstallsJobStarterAsListener() throws Exception {
        final InOrder inOrder = inOrder(listenerManager);
        uut.addScheduler(scheduler);
        uut.removeScheduler(scheduler);
        uut.addScheduler(scheduler);
        inOrder.verify(listenerManager).addJobListener(uut, EverythingMatcher.allJobs());
        inOrder.verify(listenerManager).removeJobListener(uut.getName());
        inOrder.verify(listenerManager).addJobListener(uut, EverythingMatcher.allJobs());
    }

    @Test
    public void uninstallsJobStarterAsListenerIdempotent() throws Exception {
        uut.addScheduler(scheduler);
        uut.removeScheduler(scheduler);
        uut.removeScheduler(scheduler);
        verify(listenerManager, times(1)).removeJobListener(uut.getName());
    }
}
