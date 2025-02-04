/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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
package org.quartz.xml;

import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class XMLSchedulingDataProcessorPluginTest  implements TriggerListener {

    CountDownLatch latch = new CountDownLatch(1);
    boolean jobRan = false;

    @Test
    void testPluginSchedulesFromSimpleXMLFile() throws Exception {
        Scheduler scheduler = null;
        try {
            StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-xml-plugin-test.properties");
            scheduler = factory.getScheduler();

            scheduler.getListenerManager().addTriggerListener(this);
            scheduler.start();
            latch.await(1, TimeUnit.MINUTES);

            assert(jobRan);
        } finally {
            if (scheduler != null)
                scheduler.shutdown();
        }
    }

    @Override
    public String getName() {
        return "XMLSchedulingDataProcessorPluginTestListener";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        jobRan = true;
        latch.countDown();
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {

    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {

    }

}
