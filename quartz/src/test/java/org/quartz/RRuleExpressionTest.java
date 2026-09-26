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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RRuleExpression}, the RFC 5545 RRULE evaluator.
 */
public class RRuleExpressionTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static Date d(int y, int mo, int day, int h, int mi, int s) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, mo - 1, day, h, mi, s);
        return c.getTime();
    }

    private static RRuleExpression rr(String expr, Date anchor) throws ParseException {
        RRuleExpression e = new RRuleExpression(expr);
        e.setTimeZone(UTC);
        e.setStartTime(anchor);
        return e;
    }

    private static List<Date> firstN(RRuleExpression e, int n) {
        List<Date> out = new ArrayList<>();
        Date cur = new Date(e.getStartTime().getTime() - 1000L);
        for (int i = 0; i < n; i++) {
            Date next = e.getTimeAfter(cur);
            if (next == null) {
                break;
            }
            out.add(next);
            cur = next;
        }
        return out;
    }

    @Test
    void testDailyWithByHour() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=10", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 1, 5, 10, 0, 0), f.get(0));
        assertEquals(d(2026, 1, 6, 10, 0, 0), f.get(1));
        assertEquals(d(2026, 1, 7, 10, 0, 0), f.get(2));
    }

    @Test
    void testWeeklyByDayExcludesStartWhenBeforeAnchor() throws Exception {
        // Anchor is Monday 09:00; the Monday 08:00 occurrence is before it.
        RRuleExpression e = rr("FREQ=WEEKLY;BYDAY=MO,WE,FR;BYHOUR=8", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 1, 7, 8, 0, 0), f.get(0));
        assertEquals(d(2026, 1, 9, 8, 0, 0), f.get(1));
        assertEquals(d(2026, 1, 12, 8, 0, 0), f.get(2));
    }

    @Test
    void testMonthlyByMonthDay() throws Exception {
        RRuleExpression e = rr("FREQ=MONTHLY;BYMONTHDAY=1;BYHOUR=0", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 2, 1, 0, 0, 0), f.get(0));
        assertEquals(d(2026, 3, 1, 0, 0, 0), f.get(1));
        assertEquals(d(2026, 4, 1, 0, 0, 0), f.get(2));
    }

    @Test
    void testMonthlyLastFriday() throws Exception {
        RRuleExpression e = rr("FREQ=MONTHLY;BYDAY=-1FR;BYHOUR=22", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 1, 30, 22, 0, 0), f.get(0));
        assertEquals(d(2026, 2, 27, 22, 0, 0), f.get(1));
        assertEquals(d(2026, 3, 27, 22, 0, 0), f.get(2));
    }

    @Test
    void testCountBounded() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;COUNT=3;BYHOUR=12", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 10);
        assertEquals(3, f.size());
        assertEquals(d(2026, 1, 7, 12, 0, 0), f.get(2));
        assertEquals(d(2026, 1, 7, 12, 0, 0), e.getFinalFireTime());
        assertNull(e.getTimeAfter(d(2026, 1, 7, 12, 0, 0)));
    }

    @Test
    void testUntilBoundedInclusive() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=12;UNTIL=20260107T120000Z", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 10);
        assertEquals(3, f.size());
        assertEquals(d(2026, 1, 7, 12, 0, 0), f.get(2));
        assertEquals(d(2026, 1, 7, 12, 0, 0), e.getFinalFireTime());
    }

    @Test
    void testYearly() throws Exception {
        RRuleExpression e = rr("FREQ=YEARLY;BYMONTH=7;BYMONTHDAY=4;BYHOUR=0", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 2);
        assertEquals(d(2026, 7, 4, 0, 0, 0), f.get(0));
        assertEquals(d(2027, 7, 4, 0, 0, 0), f.get(1));
    }

    @Test
    void testHourlyInterval() throws Exception {
        RRuleExpression e = rr("FREQ=HOURLY;INTERVAL=6", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 4);
        assertEquals(d(2026, 1, 5, 9, 0, 0), f.get(0));
        assertEquals(d(2026, 1, 5, 15, 0, 0), f.get(1));
        assertEquals(d(2026, 1, 5, 21, 0, 0), f.get(2));
        assertEquals(d(2026, 1, 6, 3, 0, 0), f.get(3));
    }

    @Test
    void testBySetPosFirstWeekdayOfMonth() throws Exception {
        RRuleExpression e = rr("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=1;BYHOUR=9", d(2026, 1, 1, 0, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 1, 1, 9, 0, 0), f.get(0));
        assertEquals(d(2026, 2, 2, 9, 0, 0), f.get(1));
        assertEquals(d(2026, 3, 2, 9, 0, 0), f.get(2));
    }

    @Test
    void testWeeklyEveryTwoWeeks() throws Exception {
        RRuleExpression e = rr("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO;BYHOUR=9", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 1, 5, 9, 0, 0), f.get(0));
        assertEquals(d(2026, 1, 19, 9, 0, 0), f.get(1));
        assertEquals(d(2026, 2, 2, 9, 0, 0), f.get(2));
    }

    @Test
    void testSecondly() throws Exception {
        RRuleExpression e = rr("FREQ=SECONDLY;INTERVAL=30", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 3);
        assertEquals(d(2026, 1, 5, 9, 0, 0), f.get(0));
        assertEquals(d(2026, 1, 5, 9, 0, 30), f.get(1));
        assertEquals(d(2026, 1, 5, 9, 1, 0), f.get(2));
    }

    @Test
    void testIsSatisfiedBy() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=10;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        assertTrue(e.isSatisfiedBy(d(2026, 1, 6, 10, 0, 0)));
        assertFalse(e.isSatisfiedBy(d(2026, 1, 6, 10, 0, 1)));
        assertFalse(e.isSatisfiedBy(d(2026, 1, 6, 11, 0, 0)));
    }

    @Test
    void testGetNextInvalidTimeAfter() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=10;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        // At an occurrence, the next invalid time is one second later.
        Date occ = d(2026, 1, 6, 10, 0, 0);
        assertEquals(d(2026, 1, 6, 10, 0, 1), e.getNextInvalidTimeAfter(occ));
    }

    @Test
    void testGetNextValidTimeAfterIsInclusive() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=10;BYMINUTE=0;BYSECOND=0", d(2026, 1, 5, 9, 0, 0));
        Date occ = d(2026, 1, 6, 10, 0, 0);
        assertEquals(occ, e.getNextValidTimeAfter(occ));
    }

    @Test
    void testExpressionAndToString() throws Exception {
        String expr = "FREQ=DAILY;BYHOUR=10";
        RRuleExpression e = new RRuleExpression(expr);
        assertEquals(expr, e.getExpression());
        assertEquals(expr, e.toString());
    }

    @Test
    void testCopyConstructorPreservesState() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=10", d(2026, 1, 5, 9, 0, 0));
        RRuleExpression copy = new RRuleExpression(e);
        assertEquals(e.getExpression(), copy.getExpression());
        assertEquals(e.getTimeZone(), copy.getTimeZone());
        assertEquals(e.getStartTime(), copy.getStartTime());
        assertEquals(e.getTimeAfter(d(2026, 1, 5, 9, 0, 0)), copy.getTimeAfter(d(2026, 1, 5, 9, 0, 0)));
    }

    @Test
    void testDefaultTimeZone() throws Exception {
        RRuleExpression e = new RRuleExpression("FREQ=DAILY");
        assertEquals(TimeZone.getDefault(), e.getTimeZone());
    }

    @Test
    void testValidExpressions() {
        assertTrue(RRuleExpression.isValidExpression("FREQ=DAILY"));
        assertTrue(RRuleExpression.isValidExpression("FREQ=WEEKLY;BYDAY=MO,WE,FR;INTERVAL=2"));
        assertTrue(RRuleExpression.isValidExpression("FREQ=MONTHLY;BYDAY=-1FR;BYSETPOS=1"));
        assertTrue(RRuleExpression.isValidExpression("RRULE:FREQ=YEARLY;BYMONTH=1;BYMONTHDAY=1"));
    }

    @Test
    void testMissingFreqIsInvalid() {
        assertThrows(ParseException.class, () -> new RRuleExpression("INTERVAL=2;BYHOUR=10"));
    }

    @Test
    void testCountAndUntilTogetherIsInvalid() {
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;COUNT=3;UNTIL=20260107T120000Z"));
    }

    @Test
    void testInvalidFreqIsInvalid() {
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=FORTNIGHTLY"));
    }

    @Test
    void testByWeekNoRequiresYearly() {
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=WEEKLY;BYWEEKNO=1"));
    }

    @Test
    void testOrdinalByDayRequiresMonthlyOrYearly() {
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=WEEKLY;BYDAY=2MO"));
    }

    @Test
    void testOutOfRangeValuesAreInvalid() {
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;BYHOUR=24"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;BYMINUTE=60"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=MONTHLY;BYMONTHDAY=0"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=YEARLY;BYMONTH=13"));
    }
}
