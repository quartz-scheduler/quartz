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

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.Context;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
* <p>
* A <code>Job</code> that sends a <code>javax.jms.Message</code> to a 
* <code>javax.jms.Queue</code>. This class is for older JMS. If you are using
 * JMS 1.1, you should use {@link SendDestinationMessageJob} instead.
* 
* <p>
* The following properties are expected to be provided in the <code>JobDataMap</code>:
* 
* <ul>
* <li><code>JMS_CONNECTION_FACTORY_JNDI</code> - The JNDI name of the JMS Connection Factory.</li>
* <li><code>JMS_DESTINATION_JNDI</code> - The JNDI name of the JMS destination.</li>
* <li><code>JMS_USE_TXN</code> - Whether or not to use a transacted <code>javax.jms.Session</code>.</li>
* <li><code>JMS_ACK_MODE</code> - The acknowledgement mode for the <code>javax.jms.Session</code>.</li>
* <li><code>JMS_MSG_FACTORY_CLASS_NAME</code> - The implementation class name for the <code>JmsMessageFactory</code>.</li>
* </ul>
* 
* <p>
* The following properties are optional
* 
* <ul>
* <li><code>JMS_USER</code> - The JMS user for secure destinations.
* <li><code>JMS_PASSWORD</code> - The JMS password for secure destinations.
* </ul>
* 
* <p>
* The following properties can be used for JNDI support:
* <ul>
* <li><code>INITIAL_CONTEXT_FACTORY</code> - The java.naming.factory.initial setting for JNDI.
* <li><code>PROVIDER_URL</code> - The java.naming.provider.url for JNDI.
* </ul>
* 
* 
* @see JmsMessageFactory
* 
* @author Weston M. Price (little fixes v. in 1.6.0 by Toni Alatalo) 
* 
*
*/
public final class SendQueueMessageJob implements Job {

    public void execute(final JobExecutionContext jobCtx)
            throws JobExecutionException {
        QueueConnection conn = null;

        QueueSession sess = null;

        QueueSender sender = null;

        try {
            final JobDataMap dataMap = jobCtx.getMergedJobDataMap();

            final Context namingCtx = JmsHelper.getInitialContext(dataMap);

            final QueueConnectionFactory connFactory = (QueueConnectionFactory) namingCtx
                    .lookup(dataMap
                            .getString(JmsHelper.JMS_CONNECTION_FACTORY_JNDI));

            if (!JmsHelper.isDestinationSecure(dataMap)) {
                conn = connFactory.createQueueConnection();
            } else {
                final String user = dataMap.getString(JmsHelper.JMS_USER);

                final String password = dataMap
                        .getString(JmsHelper.JMS_PASSWORD);

                conn = connFactory.createQueueConnection(user, password);
            }

            final boolean useTransactions = JmsHelper.useTransaction(dataMap);

            final int ackMode = dataMap.getInt(JmsHelper.JMS_ACK_MODE);

            sess = conn.createQueueSession(useTransactions, ackMode);

            final Queue queue = (Queue) namingCtx.lookup(dataMap
                    .getString(JmsHelper.JMS_DESTINATION_JNDI));

            sender = sess.createSender(queue);

            final JmsMessageFactory msgFactory = JmsHelper
                    .getMessageFactory(dataMap
                            .getString(JmsHelper.JMS_MSG_FACTORY_CLASS_NAME));

            final Message msg = msgFactory.createMessage(dataMap, sess);

            sender.send(msg);
        } catch (final Exception e) {
            throw new JobExecutionException(e.getMessage());
        } finally {
            JmsHelper.closeResource(sender);

            JmsHelper.closeResource(sess);

            JmsHelper.closeResource(conn);
        }

    }

}
