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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.quartz.spi.ClassLoadHelper;
import org.slf4j.Logger;

/**
 * Quartz JDBC delegate for DB2 v8 databases.
 * <p>
 * This differs from the <code>StdJDBCDelegate</code> in that it stores 
 * <code>boolean</code> values in an <code>integer</code> column.
 * </p>
 * 
 * @author Blair Jensen
 */
public class DB2v8Delegate extends StdJDBCDelegate {

    /**
     * Sets the designated parameter to the given Java <code>boolean</code> value.
     * This translates the boolean to 1/0 for true/false.
     */
    @Override           
    protected void setBoolean(PreparedStatement ps, int index, boolean val) throws SQLException {
        ps.setInt(index, ((val) ? 1 : 0));
    }
}
