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
package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.quartz.JobPersistenceException;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.quartz.impl.jdbcjobstore.JobStoreSupport.LOCK_STATE_ACCESS;
import static org.quartz.impl.jdbcjobstore.JobStoreSupport.LOCK_TRIGGER_ACCESS;

class ClusterNotificationTest {
    private final Connection connection = mock(Connection.class);
    private final DriverDelegate delegate = mock(DriverDelegate.class);
    private final Semaphore locks = mock(Semaphore.class);
    private final SchedulerSignaler signaler = mock(SchedulerSignaler.class);
    private final CheckinStore store = new CheckinStore();

    @BeforeEach
    void setUp() throws Exception {
        store.setInstanceId("local");
        store.setLockHandler(locks);
        store.setDataSource("unused");
        store.initialize(new SimpleClassLoadHelper(), signaler);
        when(locks.obtainLock(eq(connection), anyString())).thenReturn(true);
        when(delegate.updateSchedulerState(eq(connection), eq("local"), anyLong())).thenReturn(1);
        when(delegate.selectInstancesFiredTriggerRecords(eq(connection), anyString()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void notifiesLockedRecheckOnlyAfterRecoveryCommitAndCleanup() throws Exception {
        store.firstCheckIn = false;
        store.detections.add(records("stale"));
        store.detections.add(records("local", "failed"));

        assertTrue(store.doCheckin());

        InOrder order = inOrder(connection, locks, delegate, signaler);
        order.verify(connection).commit();
        order.verify(locks).obtainLock(connection, LOCK_STATE_ACCESS);
        order.verify(locks).obtainLock(connection, LOCK_TRIGGER_ACCESS);
        order.verify(delegate).deleteSchedulerState(connection, "failed");
        order.verify(connection).commit();
        order.verify(locks).releaseLock(LOCK_TRIGGER_ACCESS);
        order.verify(locks).releaseLock(LOCK_STATE_ACCESS);
        order.verify(connection).close();
        order.verify(signaler).notifyClusterListenersNodeFailed("failed");
        verify(signaler, never()).notifyClusterListenersNodeFailed("local");
        verify(signaler, never()).notifyClusterListenersNodeFailed("stale");
    }

    @Test
    void finalCommitFailureDoesNotNotifyEvenAfterPreliminaryCommitSucceeded() throws Exception {
        store.firstCheckIn = false;
        store.detections.add(records("failed"));
        store.detections.add(records("failed"));
        doNothing().doThrow(new SQLException("commit outcome unknown")).when(connection).commit();

        assertThrows(JobPersistenceException.class, store::doCheckin);

        verify(connection).rollback();
        verify(locks).releaseLock(LOCK_TRIGGER_ACCESS);
        verify(locks).releaseLock(LOCK_STATE_ACCESS);
        verify(connection).close();
        verifyNoInteractions(signaler);
    }

    @Test
    void customSignalerFailureDoesNotUndoRecoveryOrSkipLaterNodes() throws Exception {
        store.detections.add(records("first", "second"));
        doThrow(new IllegalStateException("notification failed"))
                .when(signaler).notifyClusterListenersNodeFailed("first");

        assertTrue(store.doCheckin());

        assertFalse(store.firstCheckIn);
        verify(connection, never()).rollback();
        InOrder order = inOrder(signaler);
        order.verify(signaler).notifyClusterListenersNodeFailed("first");
        order.verify(signaler).notifyClusterListenersNodeFailed("second");
    }

    private static List<SchedulerStateRecord> records(String... ids) {
        List<SchedulerStateRecord> records = new ArrayList<>();
        Arrays.stream(ids).forEach(id -> {
            SchedulerStateRecord record = new SchedulerStateRecord();
            record.setSchedulerInstanceId(id);
            records.add(record);
        });
        return records;
    }

    private class CheckinStore extends JobStoreTX {
        private final Deque<List<SchedulerStateRecord>> detections = new ArrayDeque<>();

        @Override
        protected Connection getNonManagedTXConnection() {
            return connection;
        }

        @Override
        protected DriverDelegate getDelegate() {
            return delegate;
        }

        @Override
        protected List<SchedulerStateRecord> findFailedInstances(Connection conn) {
            return detections.removeFirst();
        }
    }
}
