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

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.quartz.ClusterListener;
import org.quartz.SchedulerException;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.ThreadPool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuartzSchedulerClusterListenerTest {

    @Test
    void testListenerExceptionDoesNotPreventLaterNotifications() throws SchedulerException {
        QuartzSchedulerResources resources = new QuartzSchedulerResources();
        resources.setName("QuartzSchedulerClusterListenerTest");
        resources.setInstanceId("local");
        resources.setJobStore(mock(JobStore.class));
        resources.setThreadPool(mock(ThreadPool.class));
        // Notification dispatch is synchronous, so no scheduler thread needs to run.
        resources.setThreadExecutor(mock(ThreadExecutor.class));

        QuartzScheduler scheduler = new QuartzScheduler(resources, 0L, 0L);
        try {
            ClusterListener throwingListener = listenerNamed("throwingListener");
            ClusterListener normalListener = listenerNamed("normalListener");
            ClusterListener internalListener = listenerNamed("internalListener");
            String failedInstanceId = "failedNode";
            doThrow(new IllegalStateException("Listener failed"))
                    .when(throwingListener).clusterNodeFailed(failedInstanceId);

            scheduler.getListenerManager().addClusterListener(throwingListener);
            scheduler.getListenerManager().addClusterListener(normalListener);
            scheduler.addInternalClusterListener(internalListener);

            assertDoesNotThrow(() -> scheduler.notifyClusterListenersNodeFailed(failedInstanceId));

            InOrder order = inOrder(throwingListener, normalListener, internalListener);
            order.verify(throwingListener).clusterNodeFailed(failedInstanceId);
            order.verify(normalListener).clusterNodeFailed(failedInstanceId);
            order.verify(internalListener).clusterNodeFailed(failedInstanceId);
            order.verifyNoMoreInteractions();
        } finally {
            scheduler.shutdown();
        }
    }

    private ClusterListener listenerNamed(String name) {
        ClusterListener listener = mock(ClusterListener.class);
        when(listener.getName()).thenReturn(name);
        return listener;
    }
}
