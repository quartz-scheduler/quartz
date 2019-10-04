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
package org.quartz.integrations.tests;

import org.junit.After;
import org.junit.Before;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * A base class to support in-memory scheduler integration testing. Each test will have a fresh
 * scheduler created and started, and it will auto shutdown upon each test run.
 *
 * @author Zemian Deng
 */
public class QuartzMemoryTestSupport {
    protected static final Logger LOG = LoggerFactory.getLogger(QuartzMemoryTestSupport.class);
    protected Scheduler scheduler;

    @Before
    public void initSchedulerBeforeTest() throws Exception {
        Properties properties = createSchedulerProperties();
        SchedulerFactory sf = new StdSchedulerFactory(properties);
        scheduler = sf.getScheduler();
        afterSchedulerInit();
    }

    protected void afterSchedulerInit() throws Exception {
        LOG.info("Scheduler starting.");
        scheduler.start();
        LOG.info("Scheduler started.");
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
        return properties;
    }

    @After
    public void initSchedulerAfterTest() throws Exception {
        LOG.info("Scheduler shutting down.");
        scheduler.shutdown(true);
        LOG.info("Scheduler shutdown complete.");
    }
}
