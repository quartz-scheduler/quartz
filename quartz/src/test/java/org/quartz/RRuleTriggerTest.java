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
package org.quartz;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.quartz.impl.triggers.RRuleTriggerImpl;

/**
 * Unit test for {@link RRuleTrigger} / {@link RRuleTriggerImpl}.
 */
public class RRuleTriggerTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static Date d(int y, int mo, int day, int h, int mi, int s) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, mo - 1, day, h, mi, s);
        return c.getTime();
    }

    private static RRuleTriggerImpl trigger(String expr, Date start) throws ParseException {
        RRuleTriggerImpl t = new RRuleTriggerImpl();
        t.setName("test");
        t.setGroup("testGroup");
        t.setTimeZone(UTC);
        t.setStartTime(start);
        t.setRRuleExpression(expr);
        return t;
    }

    @Test
    void testComputeFirstFireTime() throws Exception {
        RRuleTriggerImpl t = trigger("FREQ=DAILY;BYHOUR=10;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        assertEquals(d(2026, 1, 5, 10, 0, 0), t.computeFirstFireTime(null));
    }

    @Test
    void testGetFireTimeAfterSequence() throws Exception {
        RRuleTriggerImpl t = trigger("FREQ=WEEKLY;BYDAY=MO,WE,FR;BYHOUR=8;BYMINUTE=0;BYSECOND=0",
                d(2026, 1, 5, 9, 0, 0));
        Date f1 = t.getFireTimeAfter(new Date(t.getStartTime().getTime() - 1000L));
        assertEquals(d(2026, 1, 7, 8, 0, 0), f1);
        assertEquals(d(2026, 1, 9, 8, 0, 0), t.getFireTimeAfter(f1));
        assertEquals(d(2026, 1, 12, 8, 0, 0), t.getFireTimeAfter(d(2026, 1, 9, 8, 0, 0)));
    }

    @Test
    void testCountBoundedFinalFireTime() throws Exception {
        RRuleTriggerImpl t = trigger("FREQ=DAILY;COUNT=3;BYHOUR=12;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        assertEquals(d(2026, 1, 7, 12, 0, 0), t.getFinalFireTime());
        assertNull(t.getFireTimeAfter(d(2026, 1, 7, 12, 0, 0)));
    }

    @Test
    void testEndTimeBoundsFiring() throws Exception {
        RRuleTriggerImpl t = trigger("FREQ=DAILY;BYHOUR=12;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        t.setEndTime(d(2026, 1, 7, 12, 0, 0));
        assertEquals(d(2026, 1, 7, 12, 0, 0), t.getFinalFireTime());
        // Nothing fires at or after the end time.
        assertNull(t.getFireTimeAfter(d(2026, 1, 7, 12, 0, 0)));
    }

    @Test
    void testStartTimeAnchorsIntervalCounting() throws Exception {
        RRuleTriggerImpl t = trigger("FREQ=DAILY;INTERVAL=2;BYHOUR=9;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        Date f1 = t.getFireTimeAfter(new Date(t.getStartTime().getTime() - 1000L));
        assertEquals(d(2026, 1, 5, 9, 0, 0), f1);
        assertEquals(d(2026, 1, 7, 9, 0, 0), t.getFireTimeAfter(f1));
        assertEquals(d(2026, 1, 9, 9, 0, 0), t.getFireTimeAfter(d(2026, 1, 7, 9, 0, 0)));
    }

    @Test
    void testClone() throws ParseException {
        RRuleTriggerImpl trigger = trigger("FREQ=DAILY;BYHOUR=12", d(2026, 1, 5, 9, 0, 0));
        RRuleTrigger trigger2 = (RRuleTrigger) trigger.clone();

        assertEquals(trigger, trigger2, "Cloning failed");
        assertEquals("FREQ=DAILY;BYHOUR=12", trigger2.getRRuleExpression(), "Cloning failed for the RRULE expression");
    }

    @Test
    void testMisfireInstructionValidity() throws ParseException {
        RRuleTriggerImpl trigger = new RRuleTriggerImpl();

        try {
            trigger.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
            trigger.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY);
            trigger.setMisfireInstruction(RRuleTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
            trigger.setMisfireInstruction(RRuleTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        } catch (Exception e) {
            fail("Unexpected exception while setting misfire instruction.");
        }

        try {
            trigger.setMisfireInstruction(RRuleTrigger.MISFIRE_INSTRUCTION_DO_NOTHING + 1);
            fail("Expected exception while setting invalid misfire instruction but did not get it.");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    void testMisfireInstructionInDerivedBuilder() throws ParseException {
        for (int policy : asList(
                Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                Trigger.MISFIRE_INSTRUCTION_SMART_POLICY,
                RRuleTrigger.MISFIRE_INSTRUCTION_DO_NOTHING,
                RRuleTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW)) {
            RRuleTriggerImpl trigger = new RRuleTriggerImpl();
            trigger.setRRuleExpression("FREQ=DAILY;BYHOUR=12");
            trigger.setMisfireInstruction(policy);
            assertEquals(policy, trigger.getMisfireInstruction());

            RRuleTrigger copy = trigger.getTriggerBuilder().build();
            assertEquals(policy, copy.getMisfireInstruction());
        }
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        RRuleTriggerImpl t = trigger("FREQ=WEEKLY;BYDAY=MO,WE,FR;BYHOUR=8;BYMINUTE=0;BYSECOND=0",
                d(2026, 1, 5, 9, 0, 0));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(t);
        }
        RRuleTriggerImpl copy;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            copy = (RRuleTriggerImpl) ois.readObject();
        }

        assertEquals(t.getRRuleExpression(), copy.getRRuleExpression());
        assertEquals(t.getTimeZone(), copy.getTimeZone());
        assertEquals(d(2026, 1, 7, 8, 0, 0),
                copy.getFireTimeAfter(new Date(copy.getStartTime().getTime() - 1000L)));
    }
}
