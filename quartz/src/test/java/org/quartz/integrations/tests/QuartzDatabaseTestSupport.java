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
package org.quartz.integrations.tests;

import org.apache.derby.drda.NetworkServerControl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.quartz.*;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A base class to support database (DERBY) scheduler integration testing. Each test will have a fresh
 * scheduler created and started, and it will auto shutdown upon each test run. The database will
 * be created with schema before class and destroy after class test.
 *
 * @author Zemian Deng
 */
public class QuartzDatabaseTestSupport extends QuartzMemoryTestSupport {
    protected static final Logger LOG = LoggerFactory.getLogger(QuartzDatabaseTestSupport.class);
    protected static NetworkServerControl derbyServer;

    @BeforeClass
    public static void initialize() throws Exception {
        LOG.info("Starting DERBY database.");
        InetAddress localhost = InetAddress.getByName("localhost");
        int portNum = Integer.parseInt(JdbcQuartzDerbyUtilities.DATABASE_PORT);
        derbyServer = new NetworkServerControl(localhost, portNum);
        derbyServer.start(new PrintWriter(System.out));
        int tries = 0;
        while (tries < 5) {
            try {
                Thread.sleep(500);
                derbyServer.ping();
                break;
            } catch (Exception e) {
                tries++;
            }
        }
        if (tries == 5) {
            throw new Exception("Failed to start Derby!");
        }
        LOG.info("Database started");
        try {
            LOG.info("Creating Database tables for Quartz.");
            JdbcQuartzDerbyUtilities.createDatabase();
            LOG.info("Database tables created.");
        } catch (SQLException e) {
            throw new Exception("Failed to create Quartz tables.", e);
        }
    }

    @AfterClass
    public static void shutdownDb() throws Exception {
        try {
            LOG.info("Destroying Database.");
            JdbcQuartzDerbyUtilities.destroyDatabase();
            LOG.info("Database destroyed.");
        } catch (SQLException e) {
            e.printStackTrace();
            e.getNextException().printStackTrace();
            throw new AssertionError(e);
        }

        derbyServer.shutdown();
        LOG.info("Database shutdown.");
    }

    protected Properties createSchedulerProperties() {
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName","TestScheduler");
        properties.put("org.quartz.scheduler.instanceId","AUTO");
        properties.put("org.quartz.scheduler.skipUpdateCheck","true");
        properties.put("org.quartz.threadPool.class","org.quartz.simpl.SimpleThreadPool");
        properties.put("org.quartz.threadPool.threadCount","12");
        properties.put("org.quartz.threadPool.threadPriority","5");
        properties.put("org.quartz.jobStore.misfireThreshold","10000");
        properties.put("org.quartz.jobStore.class","org.quartz.impl.jdbcjobstore.JobStoreTX");
        properties.put("org.quartz.jobStore.driverDelegateClass","org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        properties.put("org.quartz.jobStore.useProperties","true");
        properties.put("org.quartz.jobStore.dataSource","myDS");
        properties.put("org.quartz.jobStore.tablePrefix","QRTZ_");
        properties.put("org.quartz.jobStore.isClustered", "false");
        properties.put("org.quartz.dataSource.myDS.driver", "org.apache.derby.jdbc.ClientDriver");
        properties.put("org.quartz.dataSource.myDS.URL",JdbcQuartzDerbyUtilities.DATABASE_CONNECTION_PREFIX);
        properties.put("org.quartz.dataSource.myDS.user","quartz");
        properties.put("org.quartz.dataSource.myDS.password","quartz");
        properties.put("org.quartz.dataSource.myDS.maxConnections","5");
        return properties;
    }
}
