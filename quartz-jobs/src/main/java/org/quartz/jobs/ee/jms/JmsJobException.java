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

package org.quartz.jobs.ee.jms;

import org.quartz.SchedulerException;

/**
 * The JmsJobException is used to indicate an error during sending of a
 * <code>javax.jms.Message</code>.
 * 
 * @author Fernando Ribeiro
 * @author Weston M. Price
 */
public final class JmsJobException extends SchedulerException {
    private static final long serialVersionUID = 3045647075496522093L;

    public JmsJobException(final String message) {
        super(message);
    }

    public JmsJobException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JmsJobException(final Throwable cause) {
        super(cause);
    }

}
