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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.junit.jupiter.api.Test;

/**
 * Differential test of {@link RRuleExpression} against the independent
 * <a href="https://github.com/dmfs/lib-recur">lib-recur</a> implementation,
 * using randomly generated rules, anchors, query times and time zones.
 */
public class RRuleExpressionDifferentialTest {

    private static final String[] ZONES = {
        "UTC", "Europe/Prague", "America/New_York", "Asia/Kolkata", "Australia/Sydney"
    };
    private static final String[] DAYS = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
    private static final String[] FREQS = {"SECONDLY", "MINUTELY", "HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
    private static final int CASES = 5000;
    private static final int INSTANCES = 20;
    private static final long YEAR_MILLIS = 365L * 24 * 3600 * 1000;

    private final Random random = new Random(5545L);

    @Test
    void testAgainstLibRecur() throws Exception {
        List<String> failures = new ArrayList<>();
        int compared = 0;
        for (int i = 0; i < CASES && failures.size() < 20; i++) {
            String rule = randomRule();
            TimeZone tz = TimeZone.getTimeZone(ZONES[random.nextInt(ZONES.length)]);
            if (knownDivergence(rule, tz)) {
                continue;
            }
            // Anchors between 1990 and 2030, at whole seconds.
            long anchor = (631152000L + (long) (random.nextDouble() * 1262304000L)) * 1000L;
            boolean subDaily = rule.contains("SECONDLY") || rule.contains("MINUTELY") || rule.contains("HOURLY");
            long horizon = subDaily ? 3L * 24 * 3600 * 1000 : 40L * YEAR_MILLIS;
            long query = anchor + (long) (random.nextDouble() * horizon) / 1000L * 1000L;

            RRuleExpression ours = new RRuleExpression(rule);
            ours.setTimeZone(tz);
            ours.setStartTime(new Date(anchor));
            String where = rule + " tz=" + tz.getID() + " anchor=" + Instant.ofEpochMilli(anchor);

            List<Long> theirs;
            try {
                theirs = libRecur(rule, tz, anchor, Long.MIN_VALUE, INSTANCES);
            } catch (IllegalArgumentException | IllegalStateException e) {
                continue; // lib-recur gives up on rules with long runs of empty periods
            }
            compared++;
            List<Long> mine = new ArrayList<>();
            Date cur = new Date(anchor - 1000L);
            for (int n = 0; n < INSTANCES; n++) {
                Date next = ours.getTimeAfter(cur);
                if (next == null) {
                    break;
                }
                mine.add(next.getTime());
                cur = next;
            }
            // Compare local wall-clock times: in a daylight-saving overlap lib-recur
            // picks the second occurrence, while RFC 5545 §3.3.5 prescribes the first.
            if (!local(mine, tz).equals(local(theirs, tz))) {
                failures.add("sequence " + where + "\n  ours:   " + local(mine, tz) + "\n  theirs: " + local(theirs, tz));
                continue;
            }

            List<Long> after;
            try {
                after = libRecur(rule, tz, anchor, query + 1000L, 1);
            } catch (IllegalArgumentException | IllegalStateException e) {
                continue;
            }
            Date next = ours.getTimeAfter(new Date(query));
            Long expectedNext = after.isEmpty() ? null : after.get(0);
            Long actualNext = next == null ? null : next.getTime();
            if (!java.util.Objects.equals(local(expectedNext, tz), local(actualNext, tz))) {
                failures.add("getTimeAfter(" + Instant.ofEpochMilli(query) + ") " + where
                        + "\n  ours: " + fmtOne(actualNext) + " theirs: " + fmtOne(expectedNext));
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " mismatches (of " + compared + "):\n"
                + String.join("\n", failures));
    }

    /**
     * Cases where lib-recur deliberately differs from RRuleExpression:
     * <ul>
     * <li>FREQ=YEARLY with BYMONTHDAY (or BYWEEKNO) but no BYMONTH (or BYDAY):
     * lib-recur restricts the set to the DTSTART month (or weekday), while
     * RRuleExpression expands over the whole year as the RFC 5545 expand table
     * says (as python-dateutil does).</li>
     * <li>Sub-daily rules in zones with daylight-saving time: lib-recur steps
     * in wall-clock time, RRuleExpression in exact (elapsed) time.</li>
     * <li>BYWEEKNO: lib-recur assigns the days of a week that spans New Year
     * to the week-based year, RRuleExpression (like python-dateutil) to the
     * calendar year they fall in.</li>
     * <li>BYSETPOS: lib-recur drops the instances before DTSTART before it
     * applies BYSETPOS to the first period, RRuleExpression (like
     * python-dateutil) after.</li>
     * <li>Negative BYYEARDAY with a sub-daily FREQ: lib-recur is off by one day
     * (e.g. it maps -332 to February 2nd instead of 3rd in a common year).</li>
     * </ul>
     * The RRuleExpression behaviour in these cases is covered by RRuleExpressionTest.
     */
    private static boolean knownDivergence(String rule, TimeZone tz) {
        boolean yearly = rule.contains("FREQ=YEARLY");
        if (yearly && rule.contains("BYMONTHDAY") && !rule.contains("BYMONTH=")) {
            return true;
        }
        if (rule.contains("BYWEEKNO") || rule.contains("BYSETPOS")) {
            return true;
        }
        boolean subDaily = rule.contains("SECONDLY") || rule.contains("MINUTELY") || rule.contains("HOURLY");
        if (subDaily && rule.matches(".*BYYEARDAY=[^;]*-.*")) {
            return true;
        }
        return subDaily && tz.useDaylightTime();
    }

    private static List<Long> libRecur(String rule, TimeZone tz, long anchor, long from, int n) throws Exception {
        RecurrenceRuleIterator it = new RecurrenceRule(rule).iterator(anchor, tz);
        if (from != Long.MIN_VALUE) {
            it.fastForward(from);
        }
        List<Long> out = new ArrayList<>();
        while (out.size() < n && it.hasNext()) {
            long v = it.nextMillis();
            if (Instant.ofEpochMilli(v).atZone(tz.toZoneId()).getYear() > RRuleExpression.MAX_YEAR) {
                break;
            }
            out.add(v);
        }
        return out;
    }

    private String randomRule() {
        String freq = FREQS[random.nextInt(FREQS.length)];
        boolean yearly = freq.equals("YEARLY");
        boolean monthly = freq.equals("MONTHLY");
        boolean weekly = freq.equals("WEEKLY");
        boolean subDaily = freq.endsWith("LY") && (freq.startsWith("SEC") || freq.startsWith("MIN") || freq.startsWith("HOUR"));
        StringBuilder sb = new StringBuilder("FREQ=").append(freq);
        if (random.nextInt(3) == 0) {
            sb.append(";INTERVAL=").append(1 + random.nextInt(subDaily ? 50 : 4));
        }
        if (random.nextInt(4) == 0) {
            sb.append(";WKST=").append(DAYS[random.nextInt(7)]);
        }
        boolean any = false;
        if (random.nextInt(4) == 0) {
            sb.append(";BYMONTH=").append(list(1, 12, false, 3));
            any = true;
        }
        boolean weekNo = yearly && random.nextInt(4) == 0;
        if (weekNo) {
            sb.append(";BYWEEKNO=").append(list(1, 53, true, 2));
            any = true;
        }
        if ((yearly || subDaily) && !weekNo && random.nextInt(5) == 0) {
            sb.append(";BYYEARDAY=").append(list(1, 366, true, 3));
            any = true;
        }
        if (!weekly && !weekNo && random.nextInt(3) == 0) {
            sb.append(";BYMONTHDAY=").append(list(1, 31, true, 3));
            any = true;
        }
        if (random.nextInt(2) == 0) {
            sb.append(";BYDAY=").append(byDay((monthly || yearly) && !weekNo));
            any = true;
        }
        if (random.nextInt(2) == 0) {
            sb.append(";BYHOUR=").append(list(0, 23, false, subDaily ? 6 : 2));
            any = true;
        }
        if (random.nextInt(3) == 0) {
            sb.append(";BYMINUTE=").append(list(0, 59, false, subDaily ? 10 : 2));
            any = true;
        }
        if (random.nextInt(4) == 0) {
            sb.append(";BYSECOND=").append(list(0, 59, false, subDaily ? 20 : 2));
            any = true;
        }
        if (any && random.nextInt(5) == 0) {
            sb.append(";BYSETPOS=").append(list(1, 3, true, 2));
        }
        int bound = random.nextInt(4);
        if (bound == 0) {
            sb.append(";COUNT=").append(1 + random.nextInt(30));
        } else if (bound == 1) {
            sb.append(";UNTIL=").append(2000 + random.nextInt(40)).append(String.format("%02d%02d",
                    1 + random.nextInt(12), 1 + random.nextInt(28)));
            // Date-only UNTIL is not generated: lib-recur then switches to all-day
            // (floating) evaluation. RRuleExpressionTest covers it instead.
            sb.append(String.format("T%02d%02d%02dZ", random.nextInt(24), random.nextInt(60), random.nextInt(60)));
        }
        return sb.toString();
    }

    private String list(int min, int max, boolean negatives, int maxSize) {
        int size = 1 + random.nextInt(maxSize);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int v = min + random.nextInt(max - min + 1);
            if (negatives && random.nextInt(4) == 0) {
                v = -v;
            }
            if (sb.indexOf("," + v + ",") >= 0 || sb.toString().equals(String.valueOf(v))) {
                continue;
            }
            sb.append(sb.length() == 0 ? "" : ",").append(v);
        }
        return sb.toString();
    }

    private String byDay(boolean ordinals) {
        int size = 1 + random.nextInt(3);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            String day = DAYS[random.nextInt(7)];
            String tok = day;
            if (ordinals && random.nextInt(2) == 0) {
                int ord = 1 + random.nextInt(5);
                tok = (random.nextInt(3) == 0 ? -ord : ord) + day;
            }
            if (sb.indexOf(tok) >= 0) {
                continue;
            }
            sb.append(sb.length() == 0 ? "" : ",").append(tok);
        }
        return sb.toString();
    }

    private static List<LocalDateTime> local(List<Long> l, TimeZone tz) {
        List<LocalDateTime> out = new ArrayList<>();
        for (Long v : l) {
            out.add(local(v, tz));
        }
        return out;
    }

    private static LocalDateTime local(Long v, TimeZone tz) {
        return v == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(v), tz.toZoneId());
    }

    private static String fmtOne(Long v) {
        return v == null ? "null" : Instant.ofEpochMilli(v).toString();
    }
}
