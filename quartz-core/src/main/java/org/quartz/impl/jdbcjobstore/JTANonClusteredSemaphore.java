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

import java.sql.Connection;
import java.util.HashSet;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides in memory thread/resource locking that is JTA 
 * <code>{@link javax.transaction.Transaction}</code> aware.  
 * It is most appropriate for use when using 
 * <code>{@link org.quartz.impl.jdbcjobstore.JobStoreCMT}</code> without clustering.
 * 
 * <p>
 * This <code>Semaphore</code> implementation is <b>not</b> Quartz cluster safe.  
 * </p>
 *  
 * <p>
 * When a lock is obtained/released but there is no active JTA 
 * <code>{@link javax.transaction.Transaction}</code>, then this <code>Semaphore</code> operates
 * just like <code>{@link org.quartz.impl.jdbcjobstore.SimpleSemaphore}</code>. 
 * </p>
 * 
 * <p>
 * By default, this class looks for the <code>{@link javax.transaction.TransactionManager}</code>
 * in JNDI under name "java:TransactionManager".  If this is not where your Application Server 
 * registers it, you can modify the JNDI lookup location using the 
 * "transactionManagerJNDIName" property.
 * </p>
 *
 * <p>
 * <b>IMPORTANT:</b>  This Semaphore implementation is currently experimental.  
 * It has been tested a limited amount on JBoss 4.0.3SP1.  If you do choose to 
 * use it, any feedback would be most appreciated! 
 * </p>
 * 
 * @see org.quartz.impl.jdbcjobstore.SimpleSemaphore
 */
public class JTANonClusteredSemaphore implements Semaphore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public static final String DEFAULT_TRANSACTION_MANANGER_LOCATION = "java:TransactionManager";

    ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<HashSet<String>>();

    HashSet<String> locks = new HashSet<String>();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String transactionManagerJNDIName = DEFAULT_TRANSACTION_MANANGER_LOCATION;
    
    
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

    public void setTransactionManagerJNDIName(String transactionManagerJNDIName) {
        this.transactionManagerJNDIName = transactionManagerJNDIName;
    }
    
    private HashSet<String> getThreadLocks() {
        HashSet<String> threadLocks = lockOwners.get();
        if (threadLocks == null) {
            threadLocks = new HashSet<String>();
            lockOwners.set(threadLocks);
        }
        return threadLocks;
    }

    /**
     * Grants a lock on the identified resource to the calling thread (blocking
     * until it is available).
     * 
     * @return true if the lock was obtained.
     */
    public synchronized boolean obtainLock(Connection conn, String lockName) throws LockException {

        lockName = lockName.intern();

        if(log.isDebugEnabled()) {
            log.debug(
                "Lock '" + lockName + "' is desired by: "
                        + Thread.currentThread().getName());
        }

        if (!isLockOwner(conn, lockName)) {
            if(log.isDebugEnabled()) {
                log.debug(
                    "Lock '" + lockName + "' is being obtained: "
                            + Thread.currentThread().getName());
            }
            
            while (locks.contains(lockName)) {
                try {
                    this.wait();
                } catch (InterruptedException ie) {
                    if(log.isDebugEnabled()) {
                        log.debug(
                            "Lock '" + lockName + "' was not obtained by: "
                                    + Thread.currentThread().getName());
                    }
                }
            }

            // If we are in a transaction, register a callback to actually release
            // the lock when the transaction completes
            Transaction t = getTransaction();
            if (t != null) {
                try {
                    t.registerSynchronization(new SemaphoreSynchronization(lockName));
                } catch (Exception e) {
                    throw new LockException("Failed to register semaphore with Transaction.", e);
                }
            }
            
            if(log.isDebugEnabled()) {
                log.debug(
                    "Lock '" + lockName + "' given to: "
                            + Thread.currentThread().getName());
            }
            
            
            getThreadLocks().add(lockName);
            locks.add(lockName);
        } else if(log.isDebugEnabled()) {
            log.debug(
                "Lock '" + lockName + "' already owned by: "
                        + Thread.currentThread().getName()
                        + " -- but not owner!",
                new Exception("stack-trace of wrongful returner"));
        }

        return true;
    }

    /**
     * Helper method to get the current <code>{@link javax.transaction.Transaction}</code>
     * from the <code>{@link javax.transaction.TransactionManager}</code> in JNDI.
     * 
     * @return The current <code>{@link javax.transaction.Transaction}</code>, null if
     * not currently in a transaction.
     */
    protected Transaction getTransaction() throws LockException{
        InitialContext ic = null; 
        try {
            ic = new InitialContext(); 
            TransactionManager tm = (TransactionManager)ic.lookup(transactionManagerJNDIName);
            
            return tm.getTransaction();
        } catch (SystemException e) {
            throw new LockException("Failed to get Transaction from TransactionManager", e);
        } catch (NamingException e) {
            throw new LockException("Failed to find TransactionManager in JNDI under name: " + transactionManagerJNDIName, e);
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (NamingException ignored) {
                }
            }
        }
    }
    
    /**
     * Release the lock on the identified resource if it is held by the calling
     * thread, unless currently in a JTA transaction.
     */
    public synchronized void releaseLock(String lockName) throws LockException {
        releaseLock(lockName, false);
    }
    
    /**
     * Release the lock on the identified resource if it is held by the calling
     * thread, unless currently in a JTA transaction.
     * 
     * @param fromSynchronization True if this method is being invoked from
     *      <code>{@link Synchronization}</code> notified of the enclosing 
     *      transaction having completed.
     * 
     * @throws LockException Thrown if there was a problem accessing the JTA 
     *      <code>Transaction</code>.  Only relevant if <code>fromSynchronization</code>
     *      is false.
     */
    protected synchronized void releaseLock(
        String lockName, boolean fromSynchronization) throws LockException {
        lockName = lockName.intern();

        if (isLockOwner(null, lockName)) {
            
            if (fromSynchronization == false) {
                Transaction t = getTransaction();
                if (t != null) {
                    if(getLog().isDebugEnabled()) {
                        getLog().debug(
                            "Lock '" + lockName + "' is in a JTA transaction.  " + 
                            "Return deferred by: " + Thread.currentThread().getName());
                    }
                    
                    // If we are still in a transaction, then we don't want to 
                    // actually release the lock.
                    return;
                }
            }
            
            if(getLog().isDebugEnabled()) {
                getLog().debug(
                    "Lock '" + lockName + "' returned by: "
                            + Thread.currentThread().getName());
            }
            getThreadLocks().remove(lockName);
            locks.remove(lockName);
            this.notify();
        } else if (getLog().isDebugEnabled()) {
            getLog().debug(
                "Lock '" + lockName + "' attempt to return by: "
                        + Thread.currentThread().getName()
                        + " -- but not owner!",
                new Exception("stack-trace of wrongful returner"));
        }
    }

    /**
     * Determine whether the calling thread owns a lock on the identified
     * resource.
     */
    public synchronized boolean isLockOwner(Connection conn, String lockName) {
        lockName = lockName.intern();

        return getThreadLocks().contains(lockName);
    }

    /**
     * This Semaphore implementation does not use the database.
     */
    public boolean requiresConnection() {
        return false;
    }

    /**
     * Helper class that is registered with the active 
     * <code>{@link javax.transaction.Transaction}</code> so that the lock
     * will be released when the transaction completes.
     */
    private class SemaphoreSynchronization implements Synchronization {
        private String lockName;
        
        public SemaphoreSynchronization(String lockName) {
            this.lockName = lockName;
        }
        
        public void beforeCompletion() {
            // nothing to do...
        }
    
        public void afterCompletion(int status) {
            try {
                releaseLock(lockName, true);
            } catch (LockException e) {
                // Ignore as can't be thrown with fromSynchronization set to true
            }
        }
    }
}
