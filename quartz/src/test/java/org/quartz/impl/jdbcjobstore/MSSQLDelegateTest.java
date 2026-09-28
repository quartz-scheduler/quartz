/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2026
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.slf4j.LoggerFactory;

public class MSSQLDelegateTest {

    @Test
    void updateSchedulerStateBindsInstanceNameAsNString() throws Exception {
        MSSQLDelegate delegate = new MSSQLDelegate();
        delegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "TESTSCHED", "INSTANCE",
                new SimpleClassLoadHelper(), false, "");

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        int rows = delegate.updateSchedulerState(conn, "node-1", 123L);

        assertEquals(1, rows);
        verify(ps).setLong(1, 123L);
        verify(ps).setNString(2, "node-1");
        verify(ps, never()).setString(2, "node-1");
    }
}
