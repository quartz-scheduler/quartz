/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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
 */
package org.quartz.core.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.TimeZone;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.OperableTrigger;

/**
 * Unit test for {@link CronTriggerSupport}.
 */
public class CronTriggerSupportTest {

    @Test
    void testCompositeDataRoundTrip() throws Exception {
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setName("cron");
        trigger.setGroup("group");
        trigger.setJobName("job");
        trigger.setJobGroup("jobGroup");
        trigger.setCronExpression("0 0 10 ? * MON-FRI");
        trigger.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));

        CompositeData data = CronTriggerSupport.toCompositeData(trigger);
        assertEquals("0 0 10 ? * MON-FRI", data.get("cronExpression"));
        assertEquals("Europe/Prague", data.get("timeZone"));
        assertEquals("cron", data.get("name"));

        OperableTrigger copy = CronTriggerSupport.newTrigger(data);
        assertEquals("0 0 10 ? * MON-FRI", ((CronTrigger) copy).getCronExpression());
        assertEquals(TimeZone.getTimeZone("Europe/Prague"), ((CronTrigger) copy).getTimeZone());
        assertEquals(trigger.getKey(), copy.getKey());
        // TriggerSupport dispatches on the "cronExpression" item.
        assertTrue(TriggerSupport.newTrigger(data) instanceof CronTrigger);

        TabularData table = CronTriggerSupport.toTabularData(Collections.singletonList(trigger));
        assertEquals(1, table.size());
    }
}
