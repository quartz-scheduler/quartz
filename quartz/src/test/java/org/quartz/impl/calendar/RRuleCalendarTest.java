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
package org.quartz.impl.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RRuleCalendar}.
 */
public class RRuleCalendarTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static Date d(int y, int mo, int day, int h, int mi, int s) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, mo - 1, day, h, mi, s);
        return c.getTime();
    }

    @Test
    void testExcludesMatchingTimes() throws Exception {
        // Exclude every Sunday at midnight.
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=WEEKLY;BYDAY=SU;BYHOUR=0;BYMINUTE=0;BYSECOND=0", UTC);

        // 2026-01-04 is a Sunday.
        assertFalse(cal.isTimeIncluded(d(2026, 1, 4, 0, 0, 0).getTime()), "Sunday midnight should be excluded");
        assertTrue(cal.isTimeIncluded(d(2026, 1, 4, 0, 0, 1).getTime()), "one second later should be included");
        assertTrue(cal.isTimeIncluded(d(2026, 1, 5, 0, 0, 0).getTime()), "Monday midnight should be included");
    }

    @Test
    void testGetNextIncludedTimeSkipsExcludedInstant() throws Exception {
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=WEEKLY;BYDAY=SU;BYHOUR=0;BYMINUTE=0;BYSECOND=0", UTC);

        long excluded = d(2026, 1, 4, 0, 0, 0).getTime();
        long next = cal.getNextIncludedTime(excluded - 1);
        assertEquals(d(2026, 1, 4, 0, 0, 1).getTime(), next);
    }

    @Test
    void testCloneIsIndependent() throws Exception {
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=WEEKLY;BYDAY=SU;BYHOUR=0", UTC);
        RRuleCalendar clone = (RRuleCalendar) cal.clone();
        assertEquals(cal.getRRuleExpression().getExpression(), clone.getRRuleExpression().getExpression());
        assertFalse(clone.isTimeIncluded(d(2026, 1, 4, 0, 0, 0).getTime()));
    }

    @Test
    void testExcludesMonthlyRule() throws Exception {
        // With the old period estimate, a MONTHLY rule anchored in 1970 excluded nothing.
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=MONTHLY;BYMONTHDAY=1;BYHOUR=0;BYMINUTE=0;BYSECOND=0", UTC);
        assertFalse(cal.isTimeIncluded(d(2026, 10, 1, 0, 0, 0).getTime()));
        assertFalse(cal.isTimeIncluded(d(2026, 11, 1, 0, 0, 0).getTime()));
        assertTrue(cal.isTimeIncluded(d(2026, 10, 2, 0, 0, 0).getTime()));
    }

    @Test
    void testExcludesWholeDaysWithSubDailyRule() throws Exception {
        // Exclude every second of every Sunday (2026-01-04 is a Sunday).
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=SECONDLY;BYDAY=SU", UTC);
        assertFalse(cal.isTimeIncluded(d(2026, 1, 4, 0, 0, 0).getTime()));
        assertFalse(cal.isTimeIncluded(d(2026, 1, 4, 13, 37, 42).getTime()));
        assertFalse(cal.isTimeIncluded(d(2026, 1, 4, 23, 59, 59).getTime()));
        assertTrue(cal.isTimeIncluded(d(2026, 1, 3, 23, 59, 59).getTime()));
        assertTrue(cal.isTimeIncluded(d(2026, 1, 5, 0, 0, 0).getTime()));
        assertEquals(d(2026, 1, 5, 0, 0, 0).getTime(), cal.getNextIncludedTime(d(2026, 1, 4, 8, 0, 0).getTime()));
    }

    @Test
    void testDefaultAnchorIsLocalMidnight() throws Exception {
        TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=DAILY;BYHOUR=9", kolkata);
        Calendar c = Calendar.getInstance(kolkata);
        c.clear();
        c.set(2026, Calendar.OCTOBER, 22, 9, 0, 0);
        assertFalse(cal.isTimeIncluded(c.getTimeInMillis()), "09:00:00 local should be excluded");
        c.set(Calendar.MINUTE, 30);
        assertTrue(cal.isTimeIncluded(c.getTimeInMillis()), "09:30:00 local should be included");
    }

    @Test
    void testSetRRuleExpressionKeepsTimeZone() throws Exception {
        // Not a likely JVM default, so a dropped time zone cannot pass by accident.
        TimeZone chatham = TimeZone.getTimeZone("Pacific/Chatham");
        RRuleCalendar cal = new RRuleCalendar(null, "FREQ=DAILY;BYHOUR=0", chatham);
        cal.setRRuleExpression("FREQ=DAILY;BYHOUR=1");
        assertEquals(chatham, cal.getTimeZone());
        assertEquals(chatham, cal.getRRuleExpression().getTimeZone());
    }
}
