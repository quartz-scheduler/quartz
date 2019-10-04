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

package org.quartz.utils;

import javax.sql.DataSource;

/**
 * <p>
 * <code>ConnectionProvider</code>s supporting pooling of connections.
 * </p>
 *
 * <p>
 * Implementations must pool connections.
 * </p>
 *
 * @see DBConnectionManager
 * @see ConnectionProvider
 * @author Ludovic Orban
 */
public interface PoolingConnectionProvider extends ConnectionProvider {

    /** The pooling provider. */
    String POOLING_PROVIDER = "provider";

    /** The c3p0 pooling provider. */
    String POOLING_PROVIDER_C3P0 = "c3p0";

    /** The Hikari pooling provider. */
    String POOLING_PROVIDER_HIKARICP = "hikaricp";

    /** The JDBC database driver. */
    String DB_DRIVER = "driver";

    /** The JDBC database URL. */
    String DB_URL = "URL";

    /** The database user name. */
    String DB_USER = "user";

    /** The database user password. */
    String DB_PASSWORD = "password";

    /** The maximum number of database connections to have in the pool.  Default is 10. */
    String DB_MAX_CONNECTIONS = "maxConnections";

    /**
     * The database sql query to execute every time a connection is returned
     * to the pool to ensure that it is still valid.
     */
    String DB_VALIDATION_QUERY = "validationQuery";

    /** Default maximum number of database connections in the pool. */
    int DEFAULT_DB_MAX_CONNECTIONS = 10;


    DataSource getDataSource();

}
