/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.integrations.tests;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test is rather a "smoke test"
 * 
 * It will configure the scheduler with a DummyClassLoadHelper (that extends
 * CascadeClassLoadHelper) and using the XMLSchedulingDataProcessorPlugin
 * 
 * The job class configured in quartz_data.xml does not exist.
 * 
 * This test passes only if the plugin uses, like the SchedulerFactory does, the
 * DummyClassLoadHelper which is capable to load this imaginary job class
 * (previous behavior was : always instantiates the CacadeClassLoadHlper , not
 * considering the SchedulerFactory classLoadHelper)
 * 
 * @author adahanne
 * 
 */
public class QTZ225_SchedulerClassLoadHelperForPlugins_Test {

    private Scheduler sched;
    Logger log = LoggerFactory.getLogger(QTZ225_SchedulerClassLoadHelperForPlugins_Test.class);

    @Before
    public void setUp() throws SchedulerException {

        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory("org/quartz/tests/QTZ225/quartz.properties");
        sched = sf.getScheduler();

        log.info("------- Initialization Complete -----------");

        log.info("------- (Not Scheduling any Jobs - relying on XML definitions --");

        log.info("------- Starting Scheduler ----------------");

        // start the schedule
        sched.start();
    }

    @Test
    public void dummyClassLoadHelperSuccessfullyLoadedImagninaryJobClassTest() throws SchedulerException {
        if (!sched.checkExists(new JobKey("ImaginaryJob"))) {
            fail("The dummy job was not added to the scheduler, certainly because the dummy classloadhelper was not used by the plugin");
        }
    }

    @After
    public void tearDown() throws SchedulerException {
        log.info("------- Shutting down Scheduler ----------------");
        sched.shutdown();

    }
}
