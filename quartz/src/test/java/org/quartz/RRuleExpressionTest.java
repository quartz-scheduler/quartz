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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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

    // ---- review regressions: exact period arithmetic for distant queries ----

    @Test
    void testMonthlyWithDistantAnchor() throws Exception {
        // A 28-day month estimate used to overshoot and skip months for old anchors.
        RRuleExpression e = rr("FREQ=MONTHLY;BYMONTHDAY=1;BYHOUR=0", d(2023, 9, 5, 0, 0, 0));
        assertEquals(d(2026, 10, 1, 0, 0, 0), e.getTimeAfter(d(2026, 9, 10, 0, 0, 0)));
        e = rr("FREQ=MONTHLY;BYMONTHDAY=1;BYHOUR=0", d(2000, 1, 15, 0, 0, 0));
        assertEquals(d(2026, 7, 1, 0, 0, 0), e.getTimeAfter(d(2026, 6, 15, 0, 0, 0)));
        e = rr("FREQ=MONTHLY;INTERVAL=5;BYMONTHDAY=-1;BYHOUR=0", d(1990, 1, 15, 0, 0, 0));
        // 1990-01 + 5n months: 2026-04 is n = 87.
        assertEquals(d(2026, 4, 30, 0, 0, 0), e.getTimeAfter(d(2026, 1, 1, 0, 0, 0)));
    }

    @Test
    void testEveryFrequencyAgreesForNearAndDistantAnchors() throws Exception {
        // Shifting the anchor back by whole periods must not change the result.
        String[][] rules = {
            {"FREQ=SECONDLY;INTERVAL=7", "7"},
            {"FREQ=MINUTELY;INTERVAL=13", "780"},
            {"FREQ=HOURLY;INTERVAL=5", "18000"},
        };
        Date near = d(2026, 10, 1, 0, 0, 0);
        for (String[] r : rules) {
            long period = Long.parseLong(r[1]) * 1000L;
            long periods = (40L * 365 * 86400 * 1000) / period;
            RRuleExpression distant = rr(r[0], new Date(near.getTime() - periods * period));
            RRuleExpression recent = rr(r[0], near);
            Date query = d(2026, 10, 22, 13, 37, 11);
            assertEquals(recent.getTimeAfter(query), distant.getTimeAfter(query), r[0]);
        }
    }

    @Test
    void testSecondlyFarFromAnchor() throws Exception {
        // A cap of 10,000,000 periods used to stop SECONDLY rules after ~116 days.
        RRuleExpression e = rr("FREQ=SECONDLY", d(2020, 1, 1, 0, 0, 0));
        assertEquals(d(2026, 10, 22, 0, 0, 1), e.getTimeAfter(d(2026, 10, 22, 0, 0, 0)));
        RRuleExpression m = rr("FREQ=MINUTELY;INTERVAL=15", new Date(0L));
        assertEquals(d(2026, 10, 22, 0, 15, 0), m.getTimeAfter(d(2026, 10, 22, 0, 0, 0)));
    }

    @Test
    void testSecondlyFinalFireTimeWithDistantUntil() throws Exception {
        RRuleExpression e = rr("FREQ=SECONDLY;INTERVAL=10;UNTIL=20261231T235959Z", d(2026, 1, 1, 0, 0, 0));
        assertEquals(d(2026, 12, 31, 23, 59, 50), e.getFinalFireTime());
        assertEquals(d(2026, 6, 30, 23, 59, 50), e.getTimeBefore(d(2026, 7, 1, 0, 0, 0)));
    }

    @Test
    void testSparseSubDailyRuleIsFound() throws Exception {
        // Leap day that is a Monday: 2044-02-29 is the next one after 2024.
        RRuleExpression e = rr("FREQ=SECONDLY;BYMONTH=2;BYMONTHDAY=29;BYDAY=MO;BYHOUR=10;BYMINUTE=5;BYSECOND=3",
                d(2025, 1, 1, 0, 0, 0));
        assertEquals(d(2044, 2, 29, 10, 5, 3), e.getTimeAfter(d(2025, 1, 1, 0, 0, 0)));
    }

    @Test
    void testRuleThatNeverMatchesTerminates() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            assertNull(rr("FREQ=SECONDLY;INTERVAL=2;BYSECOND=5", d(2026, 1, 1, 0, 0, 0))
                    .getTimeAfter(d(2026, 1, 1, 0, 0, 0)));
            assertNull(rr("FREQ=MINUTELY;BYDAY=FR;BYSETPOS=2", d(2026, 1, 1, 0, 0, 0))
                    .getTimeAfter(d(2026, 1, 1, 0, 0, 0)));
            assertNull(rr("FREQ=SECONDLY;BYMONTH=2;BYMONTHDAY=30", d(2026, 1, 1, 0, 0, 0))
                    .getTimeAfter(d(2026, 1, 1, 0, 0, 0)));
        });
    }

    // ---- review regressions: COUNT is not re-enumerated on every call ----

    @Test
    void testCountRuleSequentialCallsAreLinear() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            RRuleExpression e = rr("FREQ=SECONDLY;COUNT=100000", d(2026, 1, 1, 0, 0, 0));
            Date cur = new Date(e.getStartTime().getTime() - 1000L);
            int n = 0;
            for (Date next = e.getTimeAfter(cur); next != null; next = e.getTimeAfter(next)) {
                n++;
                cur = next;
            }
            assertEquals(100000, n);
            assertEquals(new Date(d(2026, 1, 1, 0, 0, 0).getTime() + 99999 * 1000L), cur);
            assertEquals(cur, e.getFinalFireTime());
        });
    }

    @Test
    void testCountRuleOutOfOrderQueries() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;COUNT=5;BYHOUR=12", d(2026, 1, 1, 0, 0, 0));
        assertEquals(d(2026, 1, 4, 12, 0, 0), e.getTimeAfter(d(2026, 1, 3, 12, 0, 0)));
        assertEquals(d(2026, 1, 2, 12, 0, 0), e.getTimeAfter(d(2026, 1, 1, 12, 0, 0)));
        assertEquals(d(2026, 1, 2, 12, 0, 0), e.getTimeAfter(d(2026, 1, 1, 13, 0, 0)));
        assertEquals(d(2026, 1, 5, 12, 0, 0), e.getTimeAfter(d(2026, 1, 4, 12, 0, 0)));
        assertNull(e.getTimeAfter(d(2026, 1, 5, 12, 0, 0)));
        assertEquals(d(2026, 1, 1, 12, 0, 0), e.getTimeAfter(d(2025, 1, 1, 0, 0, 0)));
        // Changing the anchor invalidates the remembered position.
        e.setStartTime(d(2026, 2, 1, 0, 0, 0));
        assertEquals(d(2026, 2, 5, 12, 0, 0), e.getFinalFireTime());
        assertEquals(d(2026, 2, 1, 12, 0, 0), e.getTimeAfter(d(2026, 1, 1, 0, 0, 0)));
    }

    @Test
    void testTimeBeforeHonoursCountAndUntil() throws Exception {
        RRuleExpression count = rr("FREQ=DAILY;COUNT=3;BYHOUR=12", d(2026, 1, 1, 0, 0, 0));
        assertEquals(d(2026, 1, 3, 12, 0, 0), count.getTimeBefore(d(2027, 1, 1, 0, 0, 0)));
        assertEquals(d(2026, 1, 2, 12, 0, 0), count.getTimeBefore(d(2026, 1, 3, 12, 0, 0)));
        assertNull(count.getTimeBefore(d(2026, 1, 1, 12, 0, 0)));
        RRuleExpression until = rr("FREQ=DAILY;BYHOUR=12;UNTIL=20260103T120000Z", d(2026, 1, 1, 0, 0, 0));
        assertEquals(d(2026, 1, 3, 12, 0, 0), until.getTimeBefore(d(2027, 1, 1, 0, 0, 0)));
    }

    // ---- review regressions: RFC 5545 N/A combinations are rejected ----

    @Test
    void testNotApplicableCombinationsAreInvalid() {
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=WEEKLY;BYMONTHDAY=13"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=MONTHLY;BYYEARDAY=100"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=WEEKLY;BYYEARDAY=100"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;BYYEARDAY=100"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=YEARLY;BYWEEKNO=20;BYDAY=1MO"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=MONTHLY;BYSETPOS=1"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;BYHOUR=1;BYHOUR=2"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=MINUTELY;BYSECOND=60"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;UNTIL=20260230"));
        assertThrows(ParseException.class, () -> new RRuleExpression("FREQ=DAILY;UNTIL=20260101Z"));
        // Limits that RFC 5545 does allow.
        assertTrue(RRuleExpression.isValidExpression("FREQ=HOURLY;BYYEARDAY=100"));
        assertTrue(RRuleExpression.isValidExpression("FREQ=DAILY;BYMONTHDAY=13"));
        assertTrue(RRuleExpression.isValidExpression("FREQ=YEARLY;BYWEEKNO=20;BYDAY=MO"));
    }

    // ---- UNTIL ----

    @Test
    void testDateOnlyUntilIncludesTheWholeDay() throws Exception {
        RRuleExpression e = rr("FREQ=DAILY;BYHOUR=12;UNTIL=20260107", d(2026, 1, 5, 9, 0, 0));
        List<Date> f = firstN(e, 10);
        assertEquals(3, f.size());
        assertEquals(d(2026, 1, 7, 12, 0, 0), f.get(2));
        assertEquals(d(2026, 1, 7, 12, 0, 0), e.getFinalFireTime());
    }

    @Test
    void testLocalUntilFollowsTheTimeZone() throws Exception {
        RRuleExpression e = new RRuleExpression("FREQ=DAILY;BYHOUR=12;UNTIL=20260107T120000");
        e.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));
        e.setStartTime(d(2026, 1, 5, 0, 0, 0));
        assertEquals(d(2026, 1, 7, 11, 0, 0), e.getFinalFireTime()); // 12:00 CET
    }

    // ---- RFC 5545 semantics (expected values cross-checked with python-dateutil) ----

    @Test
    void testRfcWkstExamples() throws Exception {
        // RFC 5545 §3.8.5.3: the effect of WKST on a bi-weekly rule.
        RRuleExpression mo = rr("FREQ=WEEKLY;INTERVAL=2;COUNT=4;BYDAY=TU,SU;WKST=MO", d(1997, 8, 5, 9, 0, 0));
        assertEquals(Arrays.asList(d(1997, 8, 5, 9, 0, 0), d(1997, 8, 10, 9, 0, 0),
                d(1997, 8, 19, 9, 0, 0), d(1997, 8, 24, 9, 0, 0)), firstN(mo, 10));
        RRuleExpression su = rr("FREQ=WEEKLY;INTERVAL=2;COUNT=4;BYDAY=TU,SU;WKST=SU", d(1997, 8, 5, 9, 0, 0));
        assertEquals(Arrays.asList(d(1997, 8, 5, 9, 0, 0), d(1997, 8, 17, 9, 0, 0),
                d(1997, 8, 19, 9, 0, 0), d(1997, 8, 31, 9, 0, 0)), firstN(su, 10));
    }

    @Test
    void testByWeekNoIncludesDaysOutsideTheCalendarYear() throws Exception {
        // ISO week 1 of 2025 starts on Monday 2024-12-30.
        RRuleExpression e = rr("FREQ=YEARLY;BYWEEKNO=1;BYDAY=MO", d(2024, 1, 1, 0, 0, 0));
        assertEquals(Arrays.asList(d(2024, 1, 1, 0, 0, 0), d(2024, 12, 30, 0, 0, 0), d(2025, 12, 29, 0, 0, 0)),
                firstN(e, 3));
        // Week 53 only exists in some years.
        RRuleExpression w53 = rr("FREQ=YEARLY;BYWEEKNO=53;BYDAY=WE", d(2017, 3, 22, 0, 0, 0));
        assertEquals(Arrays.asList(d(2020, 12, 30, 0, 0, 0), d(2026, 12, 30, 0, 0, 0)), firstN(w53, 2));
        // Without BYDAY, BYWEEKNO expands to the whole week.
        assertEquals(7, firstN(rr("FREQ=YEARLY;BYWEEKNO=21;COUNT=7", d(2025, 5, 12, 0, 0, 0)), 10).size());
    }

    @Test
    void testByYearDayCountsFromTheEndInLeapYears() throws Exception {
        RRuleExpression e = rr("FREQ=YEARLY;BYYEARDAY=-332", d(1993, 6, 14, 0, 0, 0));
        assertEquals(Arrays.asList(d(1994, 2, 3, 0, 0, 0), d(1995, 2, 3, 0, 0, 0), d(1996, 2, 4, 0, 0, 0)),
                firstN(e, 3));
        RRuleExpression hourly = rr("FREQ=HOURLY;INTERVAL=31;BYYEARDAY=-332", d(1993, 6, 14, 7, 17, 38));
        assertEquals(d(1994, 2, 3, 2, 17, 38), firstN(hourly, 1).get(0));
    }

    @Test
    void testBySetPosNegativeAndMultiple() throws Exception {
        // Last weekday of the month, and the 2nd and 2nd-to-last weekdays.
        RRuleExpression last = rr("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1;BYHOUR=17", d(2026, 1, 1, 0, 0, 0));
        assertEquals(Arrays.asList(d(2026, 1, 30, 17, 0, 0), d(2026, 2, 27, 17, 0, 0), d(2026, 3, 31, 17, 0, 0)),
                firstN(last, 3));
        RRuleExpression multi = rr("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=2,-2;BYHOUR=17", d(2026, 1, 1, 0, 0, 0));
        assertEquals(Arrays.asList(d(2026, 1, 2, 17, 0, 0), d(2026, 1, 29, 17, 0, 0)), firstN(multi, 2));
        // BYSETPOS selects from the whole period, before instants earlier than the anchor are dropped.
        RRuleExpression straddle = rr("FREQ=YEARLY;BYMONTH=3,7,12;BYMONTHDAY=28;BYSETPOS=2", d(2024, 4, 22, 0, 0, 0));
        assertEquals(d(2024, 7, 28, 0, 0, 0), firstN(straddle, 1).get(0));
    }

    @Test
    void testOrdinalByDayLimitedByMonthDay() throws Exception {
        // BYMONTHDAY and an ordinal BYDAY intersect; the ordinal is not ignored.
        RRuleExpression e = rr("FREQ=MONTHLY;BYDAY=1MO,2MO;BYMONTHDAY=8,9,10,11,12,13,14", d(2026, 1, 1, 0, 0, 0));
        assertEquals(Arrays.asList(d(2026, 1, 12, 0, 0, 0), d(2026, 2, 9, 0, 0, 0)), firstN(e, 2));
    }

    @Test
    void testYearlyByMonthDayExpandsOverAllMonths() throws Exception {
        RRuleExpression e = rr("FREQ=YEARLY;BYMONTHDAY=10", d(2006, 1, 8, 22, 0, 15));
        assertEquals(Arrays.asList(d(2006, 1, 10, 22, 0, 15), d(2006, 2, 10, 22, 0, 15)), firstN(e, 2));
    }

    // ---- daylight-saving time ----

    private static Date local(String zone, int y, int mo, int day, int h, int mi) {
        return Date.from(LocalDateTime.of(y, mo, day, h, mi).atZone(ZoneId.of(zone)).toInstant());
    }

    @Test
    void testDailyAcrossDaylightSavingTransitions() throws Exception {
        String zone = "America/New_York";
        RRuleExpression e = new RRuleExpression("FREQ=DAILY;BYHOUR=2;BYMINUTE=30;BYSECOND=0");
        e.setTimeZone(TimeZone.getTimeZone(zone));
        e.setStartTime(local(zone, 2026, 3, 7, 0, 0));
        List<Date> spring = firstN(e, 3);
        assertEquals(local(zone, 2026, 3, 7, 2, 30), spring.get(0));
        // 02:30 does not exist on 2026-03-08; it is shifted forward to 03:30 EDT.
        assertEquals(local(zone, 2026, 3, 8, 3, 30), spring.get(1));
        assertEquals(local(zone, 2026, 3, 9, 2, 30), spring.get(2));

        RRuleExpression fall = new RRuleExpression("FREQ=DAILY;BYHOUR=1;BYMINUTE=30;BYSECOND=0");
        fall.setTimeZone(TimeZone.getTimeZone(zone));
        fall.setStartTime(local(zone, 2026, 10, 31, 0, 0));
        List<Date> f = firstN(fall, 3);
        // 01:30 occurs twice on 2026-11-01; the first (EDT) occurrence is used.
        assertEquals(Date.from(java.time.Instant.parse("2026-11-01T05:30:00Z")), f.get(1));
        assertEquals(Date.from(java.time.Instant.parse("2026-11-02T06:30:00Z")), f.get(2));
    }

    @Test
    void testHourlyUsesElapsedTimeAcrossDaylightSaving() throws Exception {
        String zone = "America/New_York";
        RRuleExpression e = new RRuleExpression("FREQ=HOURLY;BYMINUTE=0;BYSECOND=0");
        e.setTimeZone(TimeZone.getTimeZone(zone));
        e.setStartTime(local(zone, 2026, 11, 1, 0, 0));
        List<Date> f = firstN(e, 4);
        for (int i = 1; i < f.size(); i++) {
            assertEquals(3_600_000L, f.get(i).getTime() - f.get(i - 1).getTime(), "instance " + i);
        }
    }

    @Test
    void testDefaultAnchorIsLocalMidnight() throws Exception {
        RRuleExpression e = new RRuleExpression("FREQ=DAILY;BYHOUR=9");
        e.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        assertEquals(local("Asia/Kolkata", 1970, 1, 1, 0, 0), e.getStartTime());
        assertTrue(e.isSatisfiedBy(local("Asia/Kolkata", 2026, 10, 22, 9, 0)));
        assertFalse(e.isSatisfiedBy(local("Asia/Kolkata", 2026, 10, 22, 9, 30)));
    }
}
