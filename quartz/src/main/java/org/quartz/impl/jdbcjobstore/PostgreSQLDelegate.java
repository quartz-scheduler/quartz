/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
 * 
 */

package org.quartz.impl.jdbcjobstore;

import static org.quartz.TriggerKey.triggerKey;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.LinkedList;
import java.util.List;
import org.quartz.TriggerKey;
import org.quartz.spi.ClassLoadHelper;
import org.slf4j.Logger;

/**
 * <p>
 * This is a driver delegate for the PostgreSQL JDBC driver.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 */
public class PostgreSQLDelegate extends StdJDBCDelegate {

    // The difference from the StdJDBC one is the extra LIMIT clause at the end.
    private static final String SELECT_NEXT_TRIGGER_TO_ACQUIRE = "SELECT "
        + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", "
        + COL_NEXT_FIRE_TIME + ", " + COL_PRIORITY + " FROM "
        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_TRIGGER_STATE + " = ? AND " + COL_NEXT_FIRE_TIME + " <= ? "
        + "AND (" + COL_MISFIRE_INSTRUCTION + " = -1 OR (" +COL_MISFIRE_INSTRUCTION+ " <> -1 AND "+ COL_NEXT_FIRE_TIME + " >= ?)) "
        + "ORDER BY "+ COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC "
        + "LIMIT ?";

    //---------------------------------------------------------------------------
    // protected methods that can be overridden by subclasses
    //---------------------------------------------------------------------------

    /**
     * <p>
     * This method should be overridden by any delegate subclasses that need
     * special handling for BLOBs. The default implementation uses standard
     * JDBC <code>java.sql.Blob</code> operations.
     * </p>
     * 
     * @param rs
     *          the result set, already queued to the correct row
     * @param colName
     *          the column name for the BLOB
     * @return the deserialized Object from the ResultSet BLOB
     * @throws ClassNotFoundException
     *           if a class found during deserialization cannot be found
     * @throws IOException
     *           if deserialization causes an error
     */
    @Override           
    protected Object getObjectFromBlob(ResultSet rs, String colName)
        throws ClassNotFoundException, IOException, SQLException {
        InputStream binaryInput = null;
        byte[] bytes = rs.getBytes(colName);
        
        Object obj = null;
        
        if(bytes != null && bytes.length != 0) {
            binaryInput = new ByteArrayInputStream(bytes);
        
            ObjectInputStream in = new ObjectInputStream(binaryInput);
            try {
                obj = in.readObject();
            } finally {
                in.close();
            }

        }
        
        return obj;
    }

    @Override           
    protected Object getJobDataFromBlob(ResultSet rs, String colName)
        throws ClassNotFoundException, IOException, SQLException {
        if (canUseProperties()) {
            InputStream binaryInput = null;
            byte[] bytes = rs.getBytes(colName);
            if(bytes == null || bytes.length == 0) {
                return null;
            }
            binaryInput = new ByteArrayInputStream(bytes);
            return binaryInput;
        }
        return getObjectFromBlob(rs, colName);
    }

    @Override
    public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan, int maxCount)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<TriggerKey> nextTriggers = new LinkedList<TriggerKey>();
        try {
            ps = conn.prepareStatement(rtp(SELECT_NEXT_TRIGGER_TO_ACQUIRE));

            // Set max rows to retrieve
            if (maxCount < 1)
                maxCount = 1; // we want at least one trigger back.
            ps.setMaxRows(maxCount);

            // Try to give jdbc driver a hint to hopefully not pull over more than the few rows we actually need.
            // Note: in some jdbc drivers, such as MySQL, you must set maxRows before fetchSize, or you get exception!
            ps.setFetchSize(maxCount);

            ps.setString(1, STATE_WAITING);
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(noLaterThan)));
            ps.setBigDecimal(3, new BigDecimal(String.valueOf(noEarlierThan)));
            ps.setInt(4, maxCount);
            rs = ps.executeQuery();

            while (rs.next() && nextTriggers.size() < maxCount) {
                nextTriggers.add(triggerKey(
                    rs.getString(COL_TRIGGER_NAME),
                    rs.getString(COL_TRIGGER_GROUP)));
            }

            return nextTriggers;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }
}

// EOF
