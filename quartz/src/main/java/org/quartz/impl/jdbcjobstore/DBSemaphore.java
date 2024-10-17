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

import java.sql.Connection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for database based lock handlers for providing thread/resource locking 
 * in order to protect resources from being altered by multiple threads at the 
 * same time.
 */
public abstract class DBSemaphore implements Semaphore, Constants,
    StdJDBCConstants, TablePrefixAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    final ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<>();

    private String sql;
    private String insertSql;

    private String tablePrefix;
    
    private String schedName;

    private String expandedSQL;
    private String expandedInsertSQL;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public DBSemaphore(String tablePrefix, String schedName, String defaultSQL, String defaultInsertSQL) {
        this.tablePrefix = tablePrefix;
        this.schedName = schedName;
        setSQL(defaultSQL);
        setInsertSQL(defaultInsertSQL);
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

    private HashSet<String> getThreadLocks() {
        HashSet<String> threadLocks = lockOwners.get();
        if (threadLocks == null) {
            threadLocks = new HashSet<>();
            lockOwners.set(threadLocks);
        }
        return threadLocks;
    }

    /**
     * Execute the SQL that will lock the proper database row.
     */
    protected abstract void executeSQL(Connection conn, String lockName, String theExpandedSQL, String theExpandedInsertSQL) 
        throws LockException;
    
    /**
     * Grants a lock on the identified resource to the calling thread (blocking
     * until it is available).
     * 
     * @return true if the lock was obtained.
     */
    public boolean obtainLock(Connection conn, String lockName)
        throws LockException {

        if(log.isDebugEnabled()) {
            log.debug("Lock '{}' is desired by: {}", lockName, Thread.currentThread().getName());
        }
        if (!isLockOwner(lockName)) {

            executeSQL(conn, lockName, expandedSQL, expandedInsertSQL);
            
            if(log.isDebugEnabled()) {
                log.debug("Lock '{}' given to: {}", lockName, Thread.currentThread().getName());
            }
            getThreadLocks().add(lockName);
            //getThreadLocksObtainer().put(lockName, new
            // Exception("Obtainer..."));
        } else if(log.isDebugEnabled()) {
            log.debug("Lock '{}' Is already owned by: {}", lockName, Thread.currentThread().getName());
        }

        return true;
    }

       
    /**
     * Release the lock on the identified resource if it is held by the calling
     * thread.
     */
    public void releaseLock(String lockName) {

        if (isLockOwner(lockName)) {
            if(getLog().isDebugEnabled()) {
                getLog().debug("Lock '{}' returned by: {}", lockName, Thread.currentThread().getName());
            }
            getThreadLocks().remove(lockName);
            //getThreadLocksObtainer().remove(lockName);
        } else if (getLog().isDebugEnabled()) {
            getLog().warn("Lock '{}' attempt to return by: {} -- but not owner!", lockName, Thread.currentThread().getName(), new Exception("stack-trace of wrongful returner"));
        }
    }

    /**
     * Determine whether the calling thread owns a lock on the identified
     * resource.
     */
    public boolean isLockOwner(String lockName) {
        return getThreadLocks().contains(lockName);
    }

    /**
     * This Semaphore implementation does use the database.
     */
    public boolean requiresConnection() {
        return true;
    }

    protected String getSQL() {
        return sql;
    }

    protected void setSQL(String sql) {
        if ((sql != null) && (!sql.trim().isEmpty())) {
            this.sql = sql.trim();
        }
        
        setExpandedSQL();
    }

    protected void setInsertSQL(String insertSql) {
        if ((insertSql != null) && (!insertSql.trim().isEmpty())) {
            this.insertSql = insertSql.trim();
        }
        
        setExpandedSQL();
    }

    private void setExpandedSQL() {
        if (getTablePrefix() != null && getSchedName() != null && sql != null && insertSql != null) {
            expandedSQL = Util.rtp(this.sql, getTablePrefix(), getSchedulerNameLiteral());
            expandedInsertSQL = Util.rtp(this.insertSql, getTablePrefix(), getSchedulerNameLiteral());
        }
    }
    
    private String schedNameLiteral = null;
    protected String getSchedulerNameLiteral() {
        if(schedNameLiteral == null)
            schedNameLiteral = "'" + schedName + "'";
        return schedNameLiteral;
    }

    public String getSchedName() {
        return schedName;
    }

    public void setSchedName(String schedName) {
        this.schedName = schedName;
        
        setExpandedSQL();
    }
    
    protected String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        
        setExpandedSQL();
    }
}
