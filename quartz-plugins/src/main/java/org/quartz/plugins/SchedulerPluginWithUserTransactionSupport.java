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
 */
package org.quartz.plugins;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.ee.jta.UserTransactionHelper;
import org.quartz.spi.SchedulerPlugin;

/**
 * Base class for plugins that wish to support having their start and
 * shutdown methods run within a <code>UserTransaction</code>.  This is 
 * often necessary if using the JobStoreCMT and the plugin interacts with
 * jobs/triggers. 
 * 
 * <p>
 * The subclass should implement start(UserTransaction) and 
 * shutdown(UserTransaction).  The <code>UserTransaction</code> will be 
 * non-null if property <em>wrapInUserTransaction</em> is set to true.
 * </p>
 * <p>
 * For convenience, this base class also provides an initialize() implementation
 * which saves the scheduler and plugin name, as well as getLog() for logging.
 * </p>
 */
public abstract class SchedulerPluginWithUserTransactionSupport implements
    SchedulerPlugin {

    private String name;
    private Scheduler scheduler;
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Properties
    
    private boolean wrapInUserTransaction = false;

    /**
     * <p>
     * Called when the associated <code>Scheduler</code> is started, in order
     * to let the plug-in know it can now make calls into the scheduler if it
     * needs to.
     * </p>
     * 
     * <p>
     * If UserTransaction is not null, the plugin can call setRollbackOnly()
     * on it to signal that the wrapped transaction should rollback.  
     * </p>
     * 
     * @param userTransaction The UserTranaction object used to provide a 
     * transaction around the start() operation.  It will be null if 
     * <em>wrapInUserTransaction</em> is false or if the transaction failed
     * to be started.
     */
    protected void start(UserTransaction userTransaction) {
    }

    /**
     * <p>
     * Called in order to inform the <code>SchedulerPlugin</code> that it
     * should free up all of it's resources because the scheduler is shutting
     * down.
     * </p>
     * 
     * <p>
     * If UserTransaction is not null, the plugin can call setRollbackOnly()
     * on it to signal that the wrapped transaction should rollback.  
     * </p>
     * 
     * @param userTransaction The UserTranaction object used to provide a 
     * transaction around the shutdown() operation.  It will be null if 
     * <em>wrapInUserTransaction</em> is false or if the transaction failed
     * to be started.
     */
    protected void shutdown(UserTransaction userTransaction) {
    }

    /**
     * Get the commons Logger for this class.
     */
    protected Logger getLog() {
        return log;
    }

    /**
     * Get the name of this plugin.  Set as part of initialize().
     */
    protected String getName() {
        return name;
    }

    /**
     * Get this plugin's <code>Scheduler</code>.  Set as part of initialize().
     */
    protected Scheduler getScheduler() {
        return scheduler;
    }

    public void initialize(String pname, Scheduler sched) throws SchedulerException {
        this.name = pname;
        this.scheduler = sched;
    }
    
    /**
     * Wrap the start() and shutdown() methods in a UserTransaction.  This is necessary
     * for some plugins if using the JobStoreCMT.
     */
    public boolean getWrapInUserTransaction() {
        return wrapInUserTransaction;
    }

    /**
     * Wrap the start() and shutdown() methods in a UserTransaction.  This is necessary
     * for some plugins if using the JobStoreCMT.
     */
    public void setWrapInUserTransaction(boolean wrapInUserTransaction) {
        this.wrapInUserTransaction = wrapInUserTransaction;
    }

    /**
     * Based on the value of <em>wrapInUserTransaction</em>, wraps the
     * call to start(UserTransaction) in a UserTransaction.
     */
    public void start() {
        UserTransaction userTransaction = startUserTransaction();
        try {
            start(userTransaction);
        } finally {
            resolveUserTransaction(userTransaction);
        }
    }

    /**
     * Based on the value of <em>wrapInUserTransaction</em>, wraps the
     * call to shutdown(UserTransaction) in a UserTransaction.
     */
    public void shutdown() {
        UserTransaction userTransaction = startUserTransaction();
        try {
            shutdown(userTransaction);
        } finally {
            resolveUserTransaction(userTransaction);
        }
    }
        
    /**
     * If <em>wrapInUserTransaction</em> is true, starts a new UserTransaction 
     * and returns it. Otherwise, or if establishing the transaction fail, it 
     * will return null.
     */
    private UserTransaction startUserTransaction() {
        if (wrapInUserTransaction == false) {
            return null;
        }
        
        UserTransaction userTransaction = null;
        try {
            userTransaction = UserTransactionHelper.lookupUserTransaction();
            userTransaction.begin();
        } catch (Throwable t) {
            UserTransactionHelper.returnUserTransaction(userTransaction);
            userTransaction = null;
            getLog().error("Failed to start UserTransaction for plugin: " + getName(), t);
        }
        
        return userTransaction;
    }
    
    /**
     * If the given UserTransaction is not null, it is committed/rolledback,
     * and then returned to the UserTransactionHelper.
     */
    private void resolveUserTransaction(UserTransaction userTransaction) {
        if (userTransaction != null) {
            try {
                if (userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                    userTransaction.rollback();
                } else {
                    userTransaction.commit();
                } 
            } catch (Throwable t) {
                getLog().error("Failed to resolve UserTransaction for plugin: " + getName(), t);
            } finally {
                UserTransactionHelper.returnUserTransaction(userTransaction);
            }
        }
    }
}
