
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

package org.quartz.ee.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShell;
import org.quartz.spi.TriggerFiredBundle;

/**
 * <p>
 * An extension of <code>{@link org.quartz.core.JobRunShell}</code> that
 * begins an XA transaction before executing the Job, and commits (or
 * rolls-back) the transaction after execution completes.
 * </p>
 * 
 * @see org.quartz.core.JobRunShell
 * 
 * @author James House
 */
public class JTAJobRunShell extends JobRunShell {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private final Integer transactionTimeout;

    private UserTransaction ut;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a JTAJobRunShell instance with the given settings.
     * </p>
     */
    public JTAJobRunShell(Scheduler scheduler, TriggerFiredBundle bndle) {
        super(scheduler, bndle);
        this.transactionTimeout = null;
    }

    /**
     * <p>
     * Create a JTAJobRunShell instance with the given settings.
     * </p>
     */
    public JTAJobRunShell(Scheduler scheduler, TriggerFiredBundle bndle, int timeout) {
        super(scheduler, bndle);
        this.transactionTimeout = timeout;
    }
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    @Override
    protected void begin() throws SchedulerException {
        // Don't get a new UserTransaction w/o making sure we cleaned up the old 
        // one.  This is necessary because there are paths through JobRunShell.run()
        // where begin() can be called multiple times w/o complete being called in
        // between.
        cleanupUserTransaction();
        
        boolean beganSuccessfully = false;
        try {
            getLog().debug("Looking up UserTransaction.");
            ut = UserTransactionHelper.lookupUserTransaction();
            if (transactionTimeout != null) {
                ut.setTransactionTimeout(transactionTimeout);
            }

            getLog().debug("Beginning UserTransaction.");
            ut.begin();
            
            beganSuccessfully = true;
        } catch (SchedulerException se) {
            throw se;
        } catch (Exception nse) {

            throw new SchedulerException(
                    "JTAJobRunShell could not start UserTransaction.", nse);
        } finally {
            if (beganSuccessfully == false) {
                cleanupUserTransaction();
            }
        }
    }

    @Override
    protected void complete(boolean successfulExecution)
        throws SchedulerException {
        if (ut == null) {
            return;
        }

        try {
            try {
                if (ut.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                    getLog().debug("UserTransaction marked for rollback only.");
                    successfulExecution = false;
                }
            } catch (SystemException e) {
                throw new SchedulerException(
                        "JTAJobRunShell could not read UserTransaction status.", e);
            }
    
            if (successfulExecution) {
                try {
                    getLog().debug("Committing UserTransaction.");
                    ut.commit();
                } catch (Exception nse) {
                    throw new SchedulerException(
                            "JTAJobRunShell could not commit UserTransaction.", nse);
                }
            } else {
                try {
                    getLog().debug("Rolling-back UserTransaction.");
                    ut.rollback();
                } catch (Exception nse) {
                    throw new SchedulerException(
                            "JTAJobRunShell could not rollback UserTransaction.",
                            nse);
                }
            }
        } finally {
            cleanupUserTransaction();
        }
    }

    /**
     * Override passivate() to ensure we always cleanup the UserTransaction. 
     */
    @Override
    public void passivate() {
        cleanupUserTransaction();
        super.passivate();
    }
    
    private void cleanupUserTransaction() {
        if (ut != null) {
            UserTransactionHelper.returnUserTransaction(ut);
            ut = null;
        }
    }
}