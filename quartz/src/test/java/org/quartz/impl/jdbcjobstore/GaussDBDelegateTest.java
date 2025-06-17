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

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.TriggerKey;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class GaussDBDelegateTest {

    @Test
    void testSelectBlobTriggerWithBlobContent() throws Exception {
        StdJDBCDelegate jdbcDelegate = new GaussDBDelegate();
        jdbcDelegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE",
                new SimpleClassLoadHelper(), false, "");

        Connection conn = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_BLOB);
        when(resultSet.getBytes(Constants.COL_JOB_DATAMAP)).thenReturn(null);

        OperableTrigger trigger = jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("test"));
        assertNull(trigger);

        reset(resultSet);

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_BLOB);
        when(resultSet.getString(Constants.COL_TRIGGER_NAME)).thenReturn("testWithJobData");
        when(resultSet.getString(Constants.COL_TRIGGER_GROUP)).thenReturn("DEFAULT");
        when(resultSet.getString(Constants.COL_JOB_NAME)).thenReturn("testJob");
        when(resultSet.getString(Constants.COL_JOB_GROUP)).thenReturn("DEFAULT");
        when(resultSet.getLong(Constants.COL_NEXT_FIRE_TIME)).thenReturn(System.currentTimeMillis());
        when(resultSet.getLong(Constants.COL_PREV_FIRE_TIME)).thenReturn(System.currentTimeMillis() - 1000);
        when(resultSet.getString(Constants.COL_TRIGGER_STATE)).thenReturn("WAITING");

        JobDataMap jdm = new JobDataMap();
        jdm.put("key1", "value");
        jdm.put("key2", true);
        byte[] jobDataMapBytes = serializeJobDataMap(jdm);
        when(resultSet.getBytes(Constants.COL_JOB_DATAMAP)).thenReturn(jobDataMapBytes);

        trigger = jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("testWithJobData"));
        assertNull(trigger);

        reset(resultSet);

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(Constants.COL_TRIGGER_TYPE)).thenReturn(Constants.TTYPE_BLOB);
        when(resultSet.getString(Constants.COL_TRIGGER_NAME)).thenReturn("testEmptyJobData");
        when(resultSet.getString(Constants.COL_TRIGGER_GROUP)).thenReturn("DEFAULT");
        when(resultSet.getString(Constants.COL_JOB_NAME)).thenReturn("testJob");
        when(resultSet.getString(Constants.COL_JOB_GROUP)).thenReturn("DEFAULT");
        when(resultSet.getBytes(Constants.COL_JOB_DATAMAP)).thenReturn(new byte[0]);

        trigger = jdbcDelegate.selectTrigger(conn, TriggerKey.triggerKey("testEmptyJobData"));
        assertNull(trigger);
    }

    private byte[] serializeJobDataMap(JobDataMap jobDataMap) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(jobDataMap);
        }
        return baos.toByteArray();
    }

}
