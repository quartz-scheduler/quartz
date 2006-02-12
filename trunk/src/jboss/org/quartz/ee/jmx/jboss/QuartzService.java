
/* 
 * Copyright 2004-2005 OpenSymphony 
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

/*
 * Previously Copyright (c) 2001-2004 James House
 */
package org.quartz.ee.jmx.jboss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;

import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.impl.StdSchedulerFactory;

import org.jboss.naming.NonSerializableFactory;
import org.jboss.system.ServiceMBeanSupport;

/**
 * 
 * See org/quartz/ee/jmx/jboss/doc-files/quartz-service.xml for an example
 * service mbean deployment descriptor.
 * 
 * @author Andrew Collins
 */
public class QuartzService extends ServiceMBeanSupport implements
        QuartzServiceMBean {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private Properties properties;

    private StdSchedulerFactory schedulerFactory;

    private String jndiName;

    private String propertiesFile;

    private boolean error;

    private boolean useProperties;

    private boolean usePropertiesFile;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public QuartzService() {
        // flag initialization errors
        error = false;

        // use PropertiesFile attribute
        usePropertiesFile = false;
        propertiesFile = "";

        // use Properties attribute
        useProperties = false;
        properties = new Properties();

        // default JNDI name for Scheduler
        jndiName = "Quartz";
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public void setJndiName(String jndiName) throws Exception {
        String oldName = this.jndiName;
        this.jndiName = jndiName;

        if (super.getState() == STARTED) {
            unbind(oldName);

            try {
                rebind();
            } catch (NamingException ne) {
                log.error(captureStackTrace(ne));

                throw new SchedulerConfigException(
                        "Failed to rebind Scheduler - ", ne);
            }
        }
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getName() {
        return "QuartzService(" + jndiName + ")";
    }

    public void setProperties(String properties) {
        if (usePropertiesFile) {
            log
                    .error("Must specify only one of 'Properties' or 'PropertiesFile'");

            error = true;

            return;
        }

        useProperties = true;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(properties
                    .getBytes());
            this.properties = new Properties();
            this.properties.load(bais);
        } catch (IOException ioe) {
            // should not happen
        }
    }

    public String getProperties() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            properties.store(baos, "");

            return new String(baos.toByteArray());
        } catch (IOException ioe) {
            // should not happen
            return "";
        }
    }

    public void setPropertiesFile(String propertiesFile) {
        if (useProperties) {
            log
                    .error("Must specify only one of 'Properties' or 'PropertiesFile'");

            error = true;

            return;
        }

        usePropertiesFile = true;

        this.propertiesFile = propertiesFile;
    }

    public String getPropertiesFile() {
        return propertiesFile;
    }

    public void createService() throws Exception {
        log.info("Create QuartzService(" + jndiName + ")...");

        if (error) {
            log
                    .error("Must specify only one of 'Properties' or 'PropertiesFile'");

            throw new Exception(
                    "Must specify only one of 'Properties' or 'PropertiesFile'");
        }

        schedulerFactory = new StdSchedulerFactory();

        try {
            if (useProperties) {
                schedulerFactory.initialize(properties);
            }

            if (usePropertiesFile) {
                schedulerFactory.initialize(propertiesFile);
            }
        } catch (Exception e) {
            log.error(captureStackTrace(e));

            throw new SchedulerConfigException(
                    "Failed to initialize Scheduler - ", e);
        }

        log.info("QuartzService(" + jndiName + ") created.");
    }

    public void destroyService() throws Exception {
        log.info("Destroy QuartzService(" + jndiName + ")...");

        schedulerFactory = null;

        log.info("QuartzService(" + jndiName + ") destroyed.");
    }

    public void startService() throws Exception {
        log.info("Start QuartzService(" + jndiName + ")...");

        try {
            rebind();
        } catch (NamingException ne) {
            log.error(captureStackTrace(ne));

            throw new SchedulerConfigException("Failed to rebind Scheduler - ",
                    ne);
        }

        try {
            Scheduler scheduler = schedulerFactory.getScheduler();

            scheduler.start();
        } catch (Exception e) {
            log.error(captureStackTrace(e));

            throw new SchedulerConfigException("Failed to start Scheduler - ",
                    e);
        }

        log.info("QuartzService(" + jndiName + ") started.");
    }

    public void stopService() throws Exception {
        log.info("Stop QuartzService(" + jndiName + ")...");

        try {
            Scheduler scheduler = schedulerFactory.getScheduler();

            scheduler.shutdown();
        } catch (Exception e) {
            log.error(captureStackTrace(e));

            throw new SchedulerConfigException(
                    "Failed to shutdown Scheduler - ");
        }

        unbind(jndiName);

        log.info("QuartzService(" + jndiName + ") stopped.");
    }

    private String captureStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw, true));

        return sw.toString();
    }

     private void rebind() throws Exception
     {
         InitialContext rootCtx = new InitialContext();
         Name fullName = rootCtx.getNameParser("").parse(jndiName);
         Scheduler scheduler = schedulerFactory.getScheduler();
         NonSerializableFactory.rebind(fullName, scheduler, true);
     }
    
     private void unbind(String jndiName)
     {
         try
         {
             InitialContext rootCtx = new InitialContext();
             rootCtx.unbind(jndiName);
             NonSerializableFactory.unbind(jndiName);
         }
         catch (NamingException e)
         {
             e.printStackTrace();
         }
     }
}
