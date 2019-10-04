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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A <code>ConnectionProvider</code> that provides connections from a <code>DataSource</code>
 * that is managed by an application server, and made available via JNDI.
 * </p>
 * 
 * @see DBConnectionManager
 * @see ConnectionProvider
 * @see PoolingConnectionProvider
 * 
 * @author James House
 * @author Sharada Jambula
 * @author Mohammad Rezaei
 * @author Patrick Lightbody
 * @author Srinivas Venkatarangaiah
 */
public class JNDIConnectionProvider implements ConnectionProvider {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private String url;

    private Properties props;

    private Object datasource;

    private boolean alwaysLookup = false;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Constructor
     * 
     * @param jndiUrl
     *          The url for the datasource
     */
    public JNDIConnectionProvider(String jndiUrl, boolean alwaysLookup) {
        this.url = jndiUrl;
        this.alwaysLookup = alwaysLookup;
        init();
    }

    /**
     * Constructor
     * 
     * @param jndiUrl
     *          The URL for the DataSource
     * @param jndiProps
     *          The JNDI properties to use when establishing the InitialContext
     *          for the lookup of the given URL.
     */
    public JNDIConnectionProvider(String jndiUrl, Properties jndiProps,
            boolean alwaysLookup) {
        this.url = jndiUrl;
        this.props = jndiProps;
        this.alwaysLookup = alwaysLookup;
        init();
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected Logger getLog() {
        return log;
    }

    private void init() {

        if (!isAlwaysLookup()) {
            Context ctx = null;
            try {
                ctx = (props != null) ? new InitialContext(props) : new InitialContext(); 

                datasource = (DataSource) ctx.lookup(url);
            } catch (Exception e) {
                getLog().error(
                        "Error looking up datasource: " + e.getMessage(), e);
            } finally {
                if (ctx != null) {
                    try { ctx.close(); } catch(Exception ignore) {}
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        Context ctx = null;
        try {
            Object ds = this.datasource;

            if (ds == null || isAlwaysLookup()) {
                ctx = (props != null) ? new InitialContext(props): new InitialContext(); 

                ds = ctx.lookup(url);
                if (!isAlwaysLookup()) {
                    this.datasource = ds;
                }
            }

            if (ds == null) {
                throw new SQLException( "There is no object at the JNDI URL '" + url + "'");
            }

            if (ds instanceof XADataSource) {
                return (((XADataSource) ds).getXAConnection().getConnection());
            } else if (ds instanceof DataSource) { 
                return ((DataSource) ds).getConnection();
            } else {
                throw new SQLException("Object at JNDI URL '" + url + "' is not a DataSource.");
            }
        } catch (Exception e) {
            this.datasource = null;
            throw new SQLException(
                    "Could not retrieve datasource via JNDI url '" + url + "' "
                            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            if (ctx != null) {
                try { ctx.close(); } catch(Exception ignore) {}
            }
        }
    }

    public boolean isAlwaysLookup() {
        return alwaysLookup;
    }

    public void setAlwaysLookup(boolean b) {
        alwaysLookup = b;
    }

    /* 
     * @see org.quartz.utils.ConnectionProvider#shutdown()
     */
    public void shutdown() throws SQLException {
        // do nothing
    }

    public void initialize() throws SQLException {
        // do nothing, already initialized during constructor call
    }
}
