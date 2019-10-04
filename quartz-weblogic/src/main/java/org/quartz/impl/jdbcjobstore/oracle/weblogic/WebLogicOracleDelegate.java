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

package org.quartz.impl.jdbcjobstore.oracle.weblogic;

import org.slf4j.Logger;
import org.quartz.impl.jdbcjobstore.oracle.OracleDelegate;
import org.quartz.spi.ClassLoadHelper;

import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handle Blobs correctly when Oracle is being used inside of Weblogic 8.1,
 * as discussed at: http://edocs.bea.com/wls/docs81/jdbc/thirdparty.html#1043705
 *  
 * @see org.quartz.impl.jdbcjobstore.WebLogicDelegate
 * @author James House
 * @author Igor Fedulov <a href="mailto:igor@fedulov.com">igor@fedulov.com</a>
 */
public class WebLogicOracleDelegate extends OracleDelegate {

    /**
     * Check for the Weblogic Blob wrapper, and handle accordingly...
     */
    @Override
    protected Blob writeDataToBlob(ResultSet rs, int column, byte[] data) throws SQLException {
        Blob blob = rs.getBlob(column);
        
        if (blob == null) { 
            throw new SQLException("Driver's Blob representation is null!");
        }
        
        // handle thin driver's blob
        if (blob instanceof weblogic.jdbc.vendor.oracle.OracleThinBlob) { 
            ((weblogic.jdbc.vendor.oracle.OracleThinBlob) blob).putBytes(1, data);
            return blob;
        } else if(blob.getClass().getPackage().getName().startsWith("weblogic.")) {
            // (more slowly) handle blob for wrappers of other variations of drivers...
            try {
                // try to find putBytes method...
                Method m = blob.getClass().getMethod("putBytes", new Class[] {long.class, byte[].class});
                m.invoke(blob, new Object[] {new Long(1), data});
            } catch (Exception e) {
                try {
                    // Added this logic to the original code from OpenSymphony
                    // putBytes method does not exist. Try setBytes
                    Method m = blob.getClass().getMethod("setBytes", new Class[] { long.class, byte[].class });
                    m.invoke(blob, new Object[] { new Long(1), data });
                } catch (Exception e2) {
                    throw new SQLException("Unable to find putBytes(long,byte[]) or setBytes(long,byte[]) methods on blob: " + e2);
                }
            }
            return blob;
        } else {
            return super.writeDataToBlob(rs, column, data);
        }
    }
}
