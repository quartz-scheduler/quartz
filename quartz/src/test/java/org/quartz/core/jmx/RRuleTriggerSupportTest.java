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
 *
 */

package org.quartz.core.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.junit.jupiter.api.Test;
import org.quartz.RRuleTrigger;
import org.quartz.impl.triggers.RRuleTriggerImpl;
import org.quartz.spi.OperableTrigger;

/**
 * Unit test for {@link RRuleTriggerSupport}.
 */
public class RRuleTriggerSupportTest {

    @Test
    void testCompositeDataRoundTrip() throws Exception {
        RRuleTriggerImpl trigger = new RRuleTriggerImpl();
        trigger.setName("rrule");
        trigger.setGroup("group");
        trigger.setJobName("job");
        trigger.setJobGroup("jobGroup");
        trigger.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));
        trigger.setStartTime(new Date(1_800_000_000_000L));
        trigger.setRRuleExpression("FREQ=WEEKLY;BYDAY=MO;BYHOUR=9");

        CompositeData data = RRuleTriggerSupport.toCompositeData(trigger);
        assertEquals("FREQ=WEEKLY;BYDAY=MO;BYHOUR=9", data.get("rruleExpression"));
        assertEquals("Europe/Prague", data.get("timeZone"));
        assertEquals("rrule", data.get("name"));

        OperableTrigger copy = RRuleTriggerSupport.newTrigger(data);
        assertEquals("FREQ=WEEKLY;BYDAY=MO;BYHOUR=9", ((RRuleTrigger) copy).getRRuleExpression());
        assertEquals(TimeZone.getTimeZone("Europe/Prague"), ((RRuleTrigger) copy).getTimeZone());
        assertEquals(trigger.getKey(), copy.getKey());

        TabularData table = RRuleTriggerSupport.toTabularData(Collections.singletonList(trigger));
        assertEquals(1, table.size());
    }
}
