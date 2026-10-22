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
package org.quartz.impl.jdbcjobstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.quartz.JobPersistenceException;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;
import org.slf4j.LoggerFactory;
import org.quartz.JobDataMap;
import org.quartz.simpl.SimpleClassLoadHelper;



public class StdJDBCDelegateTest  {

    @Test
    void testSerializeJobData() throws IOException, NoSuchDelegateException {
        StdJDBCDelegate delegate = new StdJDBCDelegate();
        delegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false, "");
        
        JobDataMap jdm = new JobDataMap();
        delegate.serializeJobData(jdm).close();

        jdm.clear();
        jdm.put("key", "value");
        jdm.put("key2", null);
        delegate.serializeJobData(jdm).close();

        jdm.clear();
        jdm.put("key1", "value");
        jdm.put("key2", null);
        jdm.put("key3", new Object());
        try {
            delegate.serializeJobData(jdm);
            fail();
        } catch (NotSerializableException e) {
            assertTrue(e.getMessage().indexOf("key3") >= 0);
        }
    }

    @Test
    void testReadObjectFromBinaryStreamWithoutFilterIsBehaviorPreserving() throws Exception {
        StdJDBCDelegate delegate = new StdJDBCDelegate();
        delegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false, "");

        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        data.put("count", 42);

        Object read = delegate.readObjectFromBinaryStream(new ByteArrayInputStream(serialize(data)));
        assertEquals(data, read);
    }

    @Test
    void testReadObjectFromBinaryStreamAppliesConfiguredFilter() throws Exception {
        StdJDBCDelegate delegate = new StdJDBCDelegate();
        // Restrict deserialization to the JDK types a job-data map round-trips, rejecting anything else.
        delegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false,
                "objectInputFilter=java.util.*;java.lang.*;!*");

        // A legitimate, allow-listed payload still deserializes unchanged.
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        assertEquals(data, delegate.readObjectFromBinaryStream(new ByteArrayInputStream(serialize(data))));

        // A class outside the allow-list is rejected by the filter before it is constructed.
        byte[] disallowed = serialize(new UnexpectedType());
        assertThrows(InvalidClassException.class,
                () -> delegate.readObjectFromBinaryStream(new ByteArrayInputStream(disallowed)));
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    private static class UnexpectedType implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void testSelectBlobTriggerWithNoBlobContent() throws JobPersistenceException, SQLException, IOException, ClassNotFoundException {
        StdJDBCDelegate jdbcDelegate = new StdJDBCDelegate();
        jdbcDelegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false, "");

        Connection conn = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First result set has results, second has none
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_BLOB);

        OperableTrigger trigger = jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("test"));
        assertNull(trigger);

    }

    @Test
    void testHandleTriggerBlob() throws SQLException, JobPersistenceException, IOException, ClassNotFoundException {
        StdJDBCDelegate jdbcDelegate = new StdJDBCDelegate();
        jdbcDelegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false, "");

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        
        PreparedStatement pps = mock(PreparedStatement.class);
        ResultSet rss = mock(ResultSet.class);

        // Mock basic trigger data in rs
        when(rs.getString(Constants.COL_JOB_NAME)).thenReturn("jobName");
        when(rs.getString(Constants.COL_JOB_GROUP)).thenReturn("jobGroup");
        when(rs.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_BLOB);

        // Mock SELECT_BLOB_TRIGGER execution
        when(conn.prepareStatement(jdbcDelegate.rtp(StdJDBCConstants.SELECT_BLOB_TRIGGER))).thenReturn(pps);
        when(pps.executeQuery()).thenReturn(rss);
        when(rss.next()).thenReturn(true);
        
        // This is what getObjectFromBlob calls
        // Since we are testing the bug, handleTrigger is calling getObjectFromBlob(rs, COL_BLOB)
        // instead of getObjectFromBlob(rss, COL_BLOB)
        
        // Mock getObjectFromBlob behavior. It calls rs.getBlob(colName) or rs.getBytes(colName)
        // For the purpose of this test, we want to see if it fails when using 'rs'
        // 'rs' won't have the COL_BLOB column.
        
        when(rs.getBlob(Constants.COL_BLOB)).thenThrow(new SQLException("Column 'BLOB_DATA' not found"));

        // Setup the outer selectTrigger mock
        when(conn.prepareStatement(jdbcDelegate.rtp(StdJDBCConstants.SELECT_TRIGGER))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);

        // Mock success case for rss
        // getObjectFromBlob will be called on rss. rss.getBlob(COL_BLOB) should return something safe
        when(rss.getBlob(Constants.COL_BLOB)).thenReturn(null);

        jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("testName", "testGroup"));
        
        // If we reach here, it means it didn't throw SQLException from 'rs'
        // Let's also verify that rss.getBlob was called, and rs.getBlob was NOT called
        verify(rss).getBlob(Constants.COL_BLOB);
        // verify(rs, never()).getBlob(Constants.COL_BLOB); // Actually we already mocked it to throw if called
    }

    @Test
    void testSelectSimpleTriggerWithExceptionWithExtendedProps() throws SQLException, JobPersistenceException, IOException, ClassNotFoundException {
        TriggerPersistenceDelegate persistenceDelegate = mock(TriggerPersistenceDelegate.class);
        IllegalStateException exception = new IllegalStateException();
        when(persistenceDelegate.loadExtendedTriggerProperties(any(Connection.class), any(TriggerKey.class))).thenThrow(exception);

        StdJDBCDelegate jdbcDelegate = new TestStdJDBCDelegate(persistenceDelegate);
        jdbcDelegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false, "");

        Connection conn = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Mock basic trigger data
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_SIMPLE);

        try {
            jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("test"));
            fail("Trigger selection should result in exception");
        } catch (IllegalStateException e) {
            assertSame(exception, e);
        }
        verify(persistenceDelegate).loadExtendedTriggerProperties(any(Connection.class), any(TriggerKey.class));

    }
    @Test
    void testSelectSimpleTriggerWithDeleteBeforeSelectExtendedProps() throws JobPersistenceException, ClassNotFoundException, SQLException, IOException {
        TriggerPersistenceDelegate persistenceDelegate = mock(TriggerPersistenceDelegate.class);
        when(persistenceDelegate.loadExtendedTriggerProperties(any(Connection.class), any(TriggerKey.class))).thenThrow(new IllegalStateException());

        StdJDBCDelegate jdbcDelegate = new TestStdJDBCDelegate(persistenceDelegate);
        jdbcDelegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE", new SimpleClassLoadHelper(), false, "");

        Connection conn = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First result set has results, second has none
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_SIMPLE);

        OperableTrigger trigger = jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("test"));
        assertNull(trigger);
        verify(persistenceDelegate).loadExtendedTriggerProperties(any(Connection.class), any(TriggerKey.class));
    }
    @Test
    void testSelectTriggerToAcquireHonorsMaxCount() throws SQLException {

        StdJDBCDelegate jdbcDelegate = new StdJDBCDelegate();

        Connection conn = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(anyString())).thenReturn("test");

        List<TriggerKey> triggerKeys = jdbcDelegate.selectTriggerToAcquire(conn, Long.MAX_VALUE, Long.MIN_VALUE, 10);

        assertThat(triggerKeys, iterableWithSize(10));
    }

    static class TestStdJDBCDelegate extends StdJDBCDelegate {

        private final TriggerPersistenceDelegate testDelegate;

        public TestStdJDBCDelegate(TriggerPersistenceDelegate testDelegate) {
            this.testDelegate = testDelegate;
        }

        @Override
        public TriggerPersistenceDelegate findTriggerPersistenceDelegate(String discriminator) {
            return testDelegate;
        }
    }

}
