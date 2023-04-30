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

import jakarta.jms.Message;
import jakarta.jms.Session;

import org.quartz.JobDataMap;

/**
 * The JmsMessageFactory interface allows for the creation of a
 * <code>jakarta.jms.Message</code>. This interface is used in constructing a
 * <code>jakarta.jms.Message</code> that is to be sent upon execution of a JMS
 * enabled job.
 * 
 * @see SendDestinationMessageJob
 * @see SendQueueMessageJob
 * @see SendTopicMessageJob
 * 
 * @author Weston M. Price
 */
public interface JmsMessageFactory {

    /**
     * Creates a <code>jakarta.jms.Message</code>.
     * 
     * @param jobDataMap
     *            the <code>JobDataMap</code>
     * @param session
     *            the <code>jakarta.jms.Session</code>
     * 
     * @return the <code>jakarta.jms.Message</code>
     */
    Message createMessage(JobDataMap jobDataMap, Session session);
}
