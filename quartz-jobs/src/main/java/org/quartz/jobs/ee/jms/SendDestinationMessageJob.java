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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * <p>
 * A <code>Job</code> that sends a <code>javax.jms.Message</code> to a
 * <code>javax.jms.Destination</code>. Note, this class can only be used in a
 * JMS-based environment.
 * 
 * <p>
 * The following properties are expected to be provided in the
 * <code>JobDataMap</code>:
 * 
 * <ul>
 * <li><code>JMS_CONNECTION_FACTORY_JNDI</code> - The JNDI name of the JMS
 * Connection Factory.</li>
 * <li><code>JMS_DESTINATION_JNDI</code> - The JNDI name of the JMS
 * destination.</li>
 * <li><code>JMS_USE_TXN</code> - Whether or not to use a transacted
 * <code>javax.jms.Session</code>.</li>
 * <li><code>JMS_ACK_MODE</code> - The acknowledgement mode for the
 * <code>javax.jms.Session</code>.</li>
 * <li><code>JMS_MSG_FACTORY_CLASS_NAME</code> - The implementation class
 * name for the <code>JmsMessageFactory</code>.</li>
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
 * 
 * <ul>
 * <li><code>INITIAL_CONTEXT_FACTORY</code> - The java.naming.factory.initial
 * setting for JNDI.
 * <li><code>PROVIDER_URL</code> - The java.naming.provider.url for JNDI.
 * </ul>
 * 
 * @see JmsMessageFactory
 * 
 * @author Fernando Ribeiro
 * @author Weston M. Price
 * @author Frank Van Uffelen
 */
public final class SendDestinationMessageJob implements Job {

    public void execute(final JobExecutionContext jobCtx)
            throws JobExecutionException {
        Connection conn = null;

        Session sess = null;

        MessageProducer producer = null;

        try {
            final JobDataMap dataMap = jobCtx.getMergedJobDataMap();

            final Context namingCtx = JmsHelper.getInitialContext(dataMap);

            final ConnectionFactory connFactory = (ConnectionFactory) namingCtx
                    .lookup(dataMap
                            .getString(JmsHelper.JMS_CONNECTION_FACTORY_JNDI));

            if (!JmsHelper.isDestinationSecure(dataMap)) {
                conn = connFactory.createConnection();
            } else {
                final String user = dataMap.getString(JmsHelper.JMS_USER);

                final String password = dataMap
                        .getString(JmsHelper.JMS_PASSWORD);

                conn = connFactory.createConnection(user, password);
            }

            final boolean useTransaction = JmsHelper.useTransaction(dataMap);

            final int ackMode = dataMap.getInt(JmsHelper.JMS_ACK_MODE);

            sess = conn.createSession(useTransaction, ackMode);

            final Destination destination = (Destination) namingCtx
                    .lookup(dataMap.getString(JmsHelper.JMS_DESTINATION_JNDI));

            producer = sess.createProducer(destination);

            final JmsMessageFactory messageFactory = JmsHelper
                    .getMessageFactory(dataMap
                            .getString(JmsHelper.JMS_MSG_FACTORY_CLASS_NAME));

            final Message msg = messageFactory.createMessage(dataMap, sess);

            producer.send(msg);
        } catch (final Exception e) {
            throw new JobExecutionException(e);
        } finally {
            JmsHelper.closeResource(producer);

            JmsHelper.closeResource(sess);

            JmsHelper.closeResource(conn);
        }

    }

}
