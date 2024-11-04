/* 
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

import java.sql.SQLException;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.quartz.impl.jdbcjobstore.JdbcQuartzMSSQLUtilities;
import org.quartz.impl.jdbcjobstore.MSSQLDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A base class to support database (MSSQL) scheduler integration testing. Each
 * test will have a fresh scheduler created and started, and it will auto
 * shutdown upon each test run. The database will be created with schema before
 * class and destroy after class test.
 *
 * @author Arnaud Mergey
 */
public class QuartzMSSQLTestSupport extends QuartzMemoryTestSupport {
    protected static final Logger LOG = LoggerFactory.getLogger(QuartzMSSQLTestSupport.class);
    static MSSQLServerContainer<?> mssqlserver = new MSSQLServerContainer(
            DockerImageName.parse(MSSQLServerContainer.IMAGE).withTag("latest")).acceptLicense();

    @BeforeClass
    public static void initialize() throws Exception {
        LOG.info("Starting MSSQL database.");
        mssqlserver.start();
        LOG.info("Database started.");
        try {
            LOG.info("Creating Database tables for Quartz.");
            JdbcQuartzMSSQLUtilities.createDatabase(mssqlserver);
            LOG.info("Database tables created.");
        } catch (SQLException e) {
            throw new Exception("Failed to create Quartz tables.", e);
        }
    }

    @AfterClass
    public static void shutdownDb() throws Exception {
        mssqlserver.stop();
        LOG.info("Database shutdown.");
    }

    protected Properties createSchedulerProperties() {
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "TestScheduler");
        properties.put("org.quartz.scheduler.instanceId", "AUTO");
        properties.put("org.quartz.scheduler.skipUpdateCheck", "true");
        properties.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        properties.put("org.quartz.threadPool.threadCount", "12");
        properties.put("org.quartz.threadPool.threadPriority", "5");
        properties.put("org.quartz.jobStore.misfireThreshold", "10000");
        properties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        properties.put("org.quartz.jobStore.driverDelegateClass", MSSQLDelegate.class.getName());
        properties.put("org.quartz.jobStore.useProperties", "true");
        properties.put("org.quartz.jobStore.dataSource", "myDS");
        properties.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        properties.put("org.quartz.jobStore.isClustered", "false");
        properties.put("org.quartz.dataSource.myDS.driver", mssqlserver.getDriverClassName());
        properties.put("org.quartz.dataSource.myDS.URL", mssqlserver.getJdbcUrl());
        properties.put("org.quartz.dataSource.myDS.user", mssqlserver.getUsername());
        properties.put("org.quartz.dataSource.myDS.password", mssqlserver.getPassword());
        properties.put("org.quartz.dataSource.myDS.maxConnections", "5");
        properties.put("org.quartz.dataSource.myDS.provider", "hikaricp");
        return properties;
    }
}