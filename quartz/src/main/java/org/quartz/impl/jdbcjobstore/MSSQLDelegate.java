/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

/**
 * <p>
 * This is a driver delegate for the MSSQL JDBC driver.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 */
public class MSSQLDelegate extends StdJDBCDelegate {

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
        InputStream binaryInput = rs.getBinaryStream(colName);

        if(binaryInput == null || binaryInput.available() == 0) {
            return null;
        }

        Object obj = null;

        try (ObjectInputStream in = new ObjectInputStream(binaryInput)) {
            obj = in.readObject();
        }

        return obj;
    }

    @Override           
    protected Object getJobDataFromBlob(ResultSet rs, String colName)
        throws ClassNotFoundException, IOException, SQLException {
        if (canUseProperties()) {
            return rs.getBinaryStream(colName);
        }
        return getObjectFromBlob(rs, colName);
    }
    
    @Override
    public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan, int maxCount)
            throws SQLException {
        // Set max rows to retrieve
        if (maxCount < 1)
            maxCount = 1; // we want at least one trigger back.
        String selectTriggerToAcquire = "SELECT TOP " + maxCount + " " + SELECT_NEXT_TRIGGER_TO_ACQUIRE.substring(6);
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<TriggerKey> nextTriggers = new LinkedList<>();
        try {
            ps = conn.prepareStatement(rtp(selectTriggerToAcquire));
            
            ps.setMaxRows(maxCount);
            
            // Try to give jdbc driver a hint to hopefully not pull over more than the few rows we actually need.
            // Note: in some jdbc drivers, such as MySQL, you must set maxRows before fetchSize, or you get exception!
            ps.setFetchSize(maxCount);
            
            ps.setString(1, STATE_WAITING);
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(noLaterThan)));
            ps.setBigDecimal(3, new BigDecimal(String.valueOf(noEarlierThan)));
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
