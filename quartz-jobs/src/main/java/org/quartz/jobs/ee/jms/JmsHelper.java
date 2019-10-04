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

import java.lang.reflect.Method;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.quartz.JobDataMap;

/**
 * Utility class that aids in the processing of JMS based jobs and sending of
 * <code>javax.jms.Message</code>
 * 
 * @author Fernando Ribeiro
 * @author Weston M. Price
 */
public final class JmsHelper {
    public static final String CREDENTIALS = "java.naming.security.credentials";

    public static final String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";

    public static final String JMS_ACK_MODE = "jms.acknowledge";

    public static final String JMS_CONNECTION_FACTORY_JNDI = "jms.connection.factory";

    public static final String JMS_DESTINATION_JNDI = "jms.destination";

    public static final String JMS_MSG_FACTORY_CLASS_NAME = "jms.message.factory.class.name";

    public static final String JMS_PASSWORD = "jms.password";

    public static final String JMS_USE_TXN = "jms.use.transaction";

    public static final String JMS_USER = "jms.user";

    public static final String PRINCIPAL = "java.naming.security.principal";

    public static final String PROVIDER_URL = "java.naming.provider.url";

    public static void closeResource(final Object resource) {

        if (resource == null)
            return;

        try {
            final Method m = resource.getClass().getMethod("close",
                    new Class[0]);

            m.invoke(resource, new Object[0]);
        } catch (final Exception e) {
        }

    }

    public static InitialContext getInitialContext(final JobDataMap dataMap)
            throws NamingException {
        final Hashtable<String, String> params = new Hashtable<String, String>(4);

        final String initialContextFactory = dataMap
                .getString(INITIAL_CONTEXT_FACTORY);

        if (initialContextFactory != null)
            params.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);

        final String providerUrl = dataMap.getString(PROVIDER_URL);

        if (providerUrl != null)
            params.put(Context.PROVIDER_URL, providerUrl);

        final String principal = dataMap.getString(PRINCIPAL);

        if (principal != null)
            params.put(Context.SECURITY_PRINCIPAL, principal);

        final String credentials = dataMap.getString(CREDENTIALS);

        if (credentials != null)
            params.put(Context.SECURITY_CREDENTIALS, credentials);

        if (params.size() == 0)
            return new InitialContext();
        else
            return new InitialContext(params);

    }

    public static JmsMessageFactory getMessageFactory(final String name)
            throws JmsJobException {

        try {
            final Class<?> cls = Class.forName(name);

            final JmsMessageFactory factory = (JmsMessageFactory) cls
                    .newInstance();

            return factory;
        } catch (final Exception e) {
            throw new JmsJobException(e.getMessage(), e);
        }

    }

    public static boolean isDestinationSecure(final JobDataMap dataMap) {
        return ((dataMap.getString(JmsHelper.JMS_USER) != null) && (dataMap
                .getString(JmsHelper.JMS_PASSWORD) != null));
    }

    public static boolean useTransaction(final JobDataMap dataMap) {
        return dataMap.getBoolean(JMS_USE_TXN);
    }

    private JmsHelper() {
    }

}
