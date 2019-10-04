/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 *
 * @author cdennis
 */
public class UpdateLockRowSemaphoreTest {
  
  private static final PreparedStatement GOOD_STATEMENT = mock(PreparedStatement.class);
  private static final PreparedStatement FAIL_STATEMENT = mock(PreparedStatement.class);
  private static final PreparedStatement BAD_STATEMENT = mock(PreparedStatement.class);

  static {
    try {
      when(GOOD_STATEMENT.executeUpdate()).thenReturn(1);
      when(FAIL_STATEMENT.executeUpdate()).thenReturn(0);
      when(BAD_STATEMENT.executeUpdate()).thenThrow(SQLException.class);
    } catch (SQLException e) {
      throw new AssertionError(e);
    }
  }
  
  @Test
  public void testSingleSuccessUsingUpdate() throws LockException, SQLException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(GOOD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    Assert.assertTrue(semaphore.obtainLock(mockConnection, "test"));
  }
  
  @Test
  public void testSingleFailureFollowedBySuccessUsingUpdate() throws LockException, SQLException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(BAD_STATEMENT)
            .thenReturn(GOOD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    Assert.assertTrue(semaphore.obtainLock(mockConnection, "test"));
  }

  @Test
  public void testDoubleFailureFollowedBySuccessUsingUpdate() throws LockException, SQLException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(BAD_STATEMENT, BAD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    try {
      semaphore.obtainLock(mockConnection, "test");
      Assert.fail();
    } catch (LockException e) {
      //expected
    }
  }
  
  @Test
  public void testFallThroughToInsert() throws SQLException, LockException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(FAIL_STATEMENT)
            .thenThrow(AssertionError.class);
    when(mockConnection.prepareStatement(startsWith("INSERT")))
            .thenReturn(GOOD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    Assert.assertTrue(semaphore.obtainLock(mockConnection, "test"));
  }
}
