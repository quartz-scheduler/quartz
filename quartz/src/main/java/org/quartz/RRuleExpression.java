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

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeSet;

/**
 * Provides a parser and evaluator for the recurrence rules defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10">RFC&nbsp;5545
 * &sect;3.3.10 (RECUR)</a>, commonly known as an iCalendar <code>RRULE</code>.
 *
 * <p>
 * An <code>RRuleExpression</code> is the <code>RRULE</code> analogue of
 * {@link CronExpression}: it is used to compute the set of instants at which a
 * {@link RRuleTrigger} should fire. Where a cron expression is self-contained,
 * an <code>RRULE</code> is defined relative to a start instant, the RFC&nbsp;5545
 * <code>DTSTART</code>. In Quartz that anchor is supplied by the owning
 * trigger's {@link Trigger#getStartTime() start-time} (or, for
 * {@link org.quartz.impl.calendar.RRuleCalendar}, by an explicit anchor). The
 * <code>DTSTART</code> also supplies the default value of any time component
 * (second, minute, hour) that the rule itself does not constrain.
 * </p>
 *
 * <p>
 * Following the RFC, the <code>DTSTART</code> is <b>not</b> automatically part
 * of the recurrence set: only instants that actually match the rule pattern
 * (and that are not earlier than the anchor) are produced. This mirrors the way
 * a {@link CronTrigger} treats its start-time as a lower bound rather than a
 * guaranteed fire-time.
 * </p>
 *
 * <p>
 * The string given to the constructor is the bare <code>RRULE</code> value,
 * i.e. the part that follows <code>RRULE:</code> in an iCalendar object, for
 * example:
 * </p>
 *
 * <pre>
 *   FREQ=DAILY;INTERVAL=1;BYHOUR=10;BYMINUTE=15
 *   FREQ=WEEKLY;BYDAY=MO,WE,FR;BYHOUR=8
 *   FREQ=MONTHLY;BYDAY=-1FR;BYHOUR=22
 *   FREQ=YEARLY;BYMONTH=1;BYMONTHDAY=1;COUNT=10
 * </pre>
 *
 * <p>
 * The following rule parts are supported: <code>FREQ</code> (required),
 * <code>INTERVAL</code>, <code>COUNT</code>, <code>UNTIL</code>,
 * <code>WKST</code>, <code>BYMONTH</code>, <code>BYWEEKNO</code>,
 * <code>BYYEARDAY</code>, <code>BYMONTHDAY</code>, <code>BYDAY</code> (including
 * ordinals such as <code>2MO</code> or <code>-1SU</code>), <code>BYHOUR</code>,
 * <code>BYMINUTE</code>, <code>BYSECOND</code> and <code>BYSETPOS</code>. The
 * <code>expand</code>/<code>limit</code> semantics of each part follow the table
 * in RFC&nbsp;5545 &sect;3.3.10.
 * </p>
 *
 * @see RRuleTrigger
 * @see RRuleScheduleBuilder
 * @see org.quartz.impl.calendar.RRuleCalendar
 */
public final class RRuleExpression implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /**
     * The maximum year that the evaluator will scan to before giving up (and
     * treating the rule as exhausted). Mirrors {@link CronExpression#MAX_YEAR}.
     */
    public static final int MAX_YEAR = Calendar.getInstance().get(Calendar.YEAR) + 100;

    /** Safety bound on the number of recurrence periods scanned in a single call. */
    private static final int MAX_PERIODS = 10_000_000;

    enum Freq {
        SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    /** A weekday together with an optional ordinal (0 meaning "no ordinal"). */
    static final class WeekdayNum implements Serializable {
        private static final long serialVersionUID = 1L;
        final int ordinal; // 0 == none, e.g. 2 for "2MO", -1 for "-1SU"
        final int calendarDay; // Calendar.SUNDAY .. Calendar.SATURDAY

        WeekdayNum(int ordinal, int calendarDay) {
            this.ordinal = ordinal;
            this.calendarDay = calendarDay;
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Parsed rule state.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private final String expression;
    private Freq freq;
    private int interval = 1;
    private Integer count;            // null == not present
    private int wkst = Calendar.MONDAY;

    // UNTIL is stored as parsed wall-clock components plus a flag telling us
    // whether it was expressed in UTC ("...Z"). This lets the effective instant
    // be re-resolved whenever the time zone changes.
    private boolean hasUntil;
    private boolean untilUtc;
    private int untilYear, untilMonth, untilDay, untilHour, untilMinute, untilSecond;

    private int[] byMonth;      // 1..12
    private int[] byWeekNo;     // -53..-1, 1..53
    private int[] byYearDay;    // -366..-1, 1..366
    private int[] byMonthDay;   // -31..-1, 1..31
    private List<WeekdayNum> byDay;
    private int[] byHour;       // 0..23
    private int[] byMinute;     // 0..59
    private int[] bySecond;     // 0..59
    private int[] bySetPos;     // -366..-1, 1..366

    private TimeZone timeZone = TimeZone.getDefault();
    private Date startTime = new Date(0L); // DTSTART anchor; normally set by the trigger

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Construction.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Constructs a new <code>RRuleExpression</code> based on the specified
     * RFC&nbsp;5545 <code>RRULE</code> value.
     *
     * @param rrule the <code>RRULE</code> value to parse (without the leading
     *              <code>RRULE:</code>)
     * @throws ParseException if the value cannot be parsed
     */
    public RRuleExpression(String rrule) throws ParseException {
        if (rrule == null) {
            throw new IllegalArgumentException("rrule cannot be null");
        }
        this.expression = rrule.trim();
        parse(this.expression);
    }

    /**
     * Constructs a new <code>RRuleExpression</code> as a copy of an existing one
     * (including its time zone and anchor).
     */
    public RRuleExpression(RRuleExpression expression) {
        try {
            this.expression = expression.getExpression();
            parse(this.expression);
        } catch (ParseException e) {
            // never happens; the source expression already parsed successfully
            throw new AssertionError(e);
        }
        setTimeZone(expression.getTimeZone());
        setStartTime(expression.getStartTime());
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Public evaluation API (mirrors CronExpression).
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Indicates whether the given date satisfies the recurrence rule. Note that
     * milliseconds are ignored when testing, as the rule resolves to whole
     * seconds.
     */
    public boolean isSatisfiedBy(Date date) {
        Calendar test = Calendar.getInstance(getTimeZone(), Locale.US);
        test.setTime(date);
        test.set(Calendar.MILLISECOND, 0);
        Date rounded = test.getTime();
        if (rounded.before(startTimeSeconds())) {
            return false;
        }
        Date next = getTimeAfter(new Date(rounded.getTime() - 1000L));
        return next != null && next.equals(rounded);
    }

    /** Returns the next date/time <i>at or after</i> the given date that satisfies the rule. */
    public Date getNextValidTimeAfter(Date date) {
        return getTimeAfter(new Date(date.getTime() - 1000L));
    }

    /**
     * Returns the next date/time after the given date that does <b>not</b>
     * satisfy the rule (used by {@link org.quartz.impl.calendar.RRuleCalendar}).
     */
    public Date getNextInvalidTimeAfter(Date date) {
        Calendar cal = Calendar.getInstance(getTimeZone(), Locale.US);
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        long candidate = cal.getTimeInMillis() + 1000L; // step in whole seconds
        Date last = getTimeAfter(new Date(candidate - 1000L));
        while (last != null && last.getTime() == candidate) {
            candidate += 1000L;
            last = getTimeAfter(new Date(candidate - 1000L));
        }
        return new Date(candidate);
    }

    /**
     * Returns the next time the rule fires strictly after the given time, or
     * <code>null</code> if the rule will not fire again.
     */
    public Date getTimeAfter(Date afterTime) {
        long after = floorSecond(afterTime.getTime());
        long anchor = startTimeSeconds().getTime();
        long lowerBound = Math.max(after, anchor - 1000L); // exclusive lower bound

        Calendar untilCal = resolveUntil();
        Long untilMillis = untilCal == null ? null : untilCal.getTimeInMillis();

        if (count != null) {
            // COUNT rules must be enumerated from the anchor.
            long emitted = 0;
            for (long occ : new OccurrenceScan(anchor)) {
                if (count != null && emitted >= count) {
                    return null;
                }
                emitted++;
                if (untilMillis != null && occ > untilMillis) {
                    return null;
                }
                if (occ > lowerBound) {
                    return new Date(occ);
                }
            }
            return null;
        }

        long scanFrom = Math.max(anchor, lowerBound);
        for (long occ : new OccurrenceScan(scanFrom)) {
            if (occ <= lowerBound) {
                continue;
            }
            if (untilMillis != null && occ > untilMillis) {
                return null;
            }
            return new Date(occ);
        }
        return null;
    }

    /**
     * Returns the last time the rule fires strictly before the given time, or
     * <code>null</code> if there is no such time.
     */
    public Date getTimeBefore(Date endTime) {
        long end = endTime.getTime();
        long anchor = startTimeSeconds().getTime();
        if (end <= anchor) {
            return null;
        }
        Long best = null;
        for (long occ : new OccurrenceScan(anchor)) {
            if (occ >= end) {
                break;
            }
            best = occ;
        }
        return best == null ? null : new Date(best);
    }

    /**
     * Returns the final time the rule will ever fire (for rules bounded by
     * <code>COUNT</code> or <code>UNTIL</code>), or <code>null</code> for an
     * unbounded rule.
     */
    public Date getFinalFireTime() {
        Calendar untilCal = resolveUntil();
        if (count == null && untilCal == null) {
            return null;
        }
        long anchor = startTimeSeconds().getTime();
        Long untilMillis = untilCal == null ? null : untilCal.getTimeInMillis();
        Long last = null;
        long emitted = 0;
        for (long occ : new OccurrenceScan(anchor)) {
            if (count != null && emitted >= count) {
                break;
            }
            if (untilMillis != null && occ > untilMillis) {
                break;
            }
            last = occ;
            emitted++;
        }
        return last == null ? null : new Date(last);
    }

    public TimeZone getTimeZone() {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /** Returns the <code>DTSTART</code> anchor used to seed the recurrence. */
    public Date getStartTime() {
        return startTime;
    }

    /** Sets the <code>DTSTART</code> anchor used to seed the recurrence. */
    public void setStartTime(Date startTime) {
        this.startTime = startTime == null ? new Date(0L) : new Date(startTime.getTime());
    }

    /** Returns the original <code>RRULE</code> string this expression was built from. */
    public String getExpression() {
        return expression;
    }

    public String getExpressionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("FREQ: ").append(freq).append("\n");
        sb.append("interval: ").append(interval).append("\n");
        if (count != null) {
            sb.append("count: ").append(count).append("\n");
        }
        Calendar until = resolveUntil();
        if (until != null) {
            sb.append("until: ").append(until.getTime()).append("\n");
        }
        appendPart(sb, "byMonth", byMonth);
        appendPart(sb, "byWeekNo", byWeekNo);
        appendPart(sb, "byYearDay", byYearDay);
        appendPart(sb, "byMonthDay", byMonthDay);
        if (byDay != null) {
            sb.append("byDay: ");
            for (WeekdayNum w : byDay) {
                if (w.ordinal != 0) {
                    sb.append(w.ordinal);
                }
                sb.append(dayName(w.calendarDay)).append(' ');
            }
            sb.append("\n");
        }
        appendPart(sb, "byHour", byHour);
        appendPart(sb, "byMinute", byMinute);
        appendPart(sb, "bySecond", bySecond);
        appendPart(sb, "bySetPos", bySetPos);
        return sb.toString();
    }

    /** Indicates whether the specified value can be parsed into a valid rule. */
    public static boolean isValidExpression(String rrule) {
        try {
            new RRuleExpression(rrule);
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }

    /** Validates the specified value, throwing a {@link ParseException} if invalid. */
    public static void validateExpression(String rrule) throws ParseException {
        new RRuleExpression(rrule);
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public Object clone() {
        return new RRuleExpression(this);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Parsing.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void parse(String rrule) throws ParseException {
        if (rrule.isEmpty()) {
            throw new ParseException("RRULE is empty", 0);
        }
        String value = rrule;
        // Tolerate a leading "RRULE:" prefix.
        if (value.regionMatches(true, 0, "RRULE:", 0, 6)) {
            value = value.substring(6);
        }
        for (String rawPart : value.split(";")) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            if (eq < 0) {
                throw new ParseException("Invalid RRULE part (missing '='): " + part, 0);
            }
            String name = part.substring(0, eq).trim().toUpperCase(Locale.US);
            String val = part.substring(eq + 1).trim();
            switch (name) {
                case "FREQ":
                    freq = parseFreq(val);
                    break;
                case "INTERVAL":
                    interval = parsePositiveInt(val, "INTERVAL");
                    break;
                case "COUNT":
                    count = parsePositiveInt(val, "COUNT");
                    break;
                case "UNTIL":
                    parseUntil(val);
                    break;
                case "WKST":
                    wkst = parseWeekday(val);
                    break;
                case "BYMONTH":
                    byMonth = parseIntList(val, 1, 12, false, "BYMONTH");
                    break;
                case "BYWEEKNO":
                    byWeekNo = parseIntList(val, 1, 53, true, "BYWEEKNO");
                    break;
                case "BYYEARDAY":
                    byYearDay = parseIntList(val, 1, 366, true, "BYYEARDAY");
                    break;
                case "BYMONTHDAY":
                    byMonthDay = parseIntList(val, 1, 31, true, "BYMONTHDAY");
                    break;
                case "BYDAY":
                    byDay = parseByDay(val);
                    break;
                case "BYHOUR":
                    byHour = parseIntList(val, 0, 23, false, "BYHOUR");
                    break;
                case "BYMINUTE":
                    byMinute = parseIntList(val, 0, 59, false, "BYMINUTE");
                    break;
                case "BYSECOND":
                    bySecond = parseIntList(val, 0, 60, false, "BYSECOND");
                    break;
                case "BYSETPOS":
                    bySetPos = parseIntList(val, 1, 366, true, "BYSETPOS");
                    break;
                default:
                    throw new ParseException("Unsupported RRULE part: " + name, 0);
            }
        }
        if (freq == null) {
            throw new ParseException("RRULE is missing the required FREQ part", 0);
        }
        if (count != null && hasUntil) {
            throw new ParseException("RRULE must not contain both COUNT and UNTIL", 0);
        }
        // BYWEEKNO / BYYEARDAY are only meaningful with FREQ=YEARLY.
        if (byWeekNo != null && freq != Freq.YEARLY) {
            throw new ParseException("BYWEEKNO is only valid with FREQ=YEARLY", 0);
        }
        // Ordinal BYDAY values (e.g. 2MO) are only valid with MONTHLY/YEARLY.
        if (byDay != null && freq != Freq.MONTHLY && freq != Freq.YEARLY) {
            for (WeekdayNum w : byDay) {
                if (w.ordinal != 0) {
                    throw new ParseException(
                            "Ordinal BYDAY values are only valid with FREQ=MONTHLY or FREQ=YEARLY", 0);
                }
            }
        }
    }

    private static Freq parseFreq(String val) throws ParseException {
        try {
            return Freq.valueOf(val.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            throw new ParseException("Invalid FREQ value: " + val, 0);
        }
    }

    private static int parsePositiveInt(String val, String part) throws ParseException {
        try {
            int i = Integer.parseInt(val);
            if (i <= 0) {
                throw new ParseException(part + " must be a positive integer: " + val, 0);
            }
            return i;
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid " + part + " value: " + val, 0);
        }
    }

    private void parseUntil(String val) throws ParseException {
        // Accept forms: yyyyMMdd, yyyyMMdd'T'HHmmss, yyyyMMdd'T'HHmmss'Z'
        try {
            String v = val.trim();
            boolean utc = v.endsWith("Z");
            if (utc) {
                v = v.substring(0, v.length() - 1);
            }
            int y = Integer.parseInt(v.substring(0, 4));
            int mo = Integer.parseInt(v.substring(4, 6));
            int d = Integer.parseInt(v.substring(6, 8));
            int h = 0, mi = 0, s = 0;
            if (v.length() >= 15 && (v.charAt(8) == 'T' || v.charAt(8) == 't')) {
                h = Integer.parseInt(v.substring(9, 11));
                mi = Integer.parseInt(v.substring(11, 13));
                s = Integer.parseInt(v.substring(13, 15));
            } else if (v.length() != 8) {
                throw new ParseException("Invalid UNTIL value: " + val, 0);
            }
            this.hasUntil = true;
            this.untilUtc = utc;
            this.untilYear = y;
            this.untilMonth = mo;
            this.untilDay = d;
            this.untilHour = h;
            this.untilMinute = mi;
            this.untilSecond = s;
        } catch (RuntimeException e) {
            throw new ParseException("Invalid UNTIL value: " + val, 0);
        }
    }

    private static int parseWeekday(String val) throws ParseException {
        Integer d = weekdayCode(val.toUpperCase(Locale.US));
        if (d == null) {
            throw new ParseException("Invalid weekday: " + val, 0);
        }
        return d;
    }

    private static int[] parseIntList(String val, int min, int max, boolean allowNegative, String part)
            throws ParseException {
        String[] toks = val.split(",");
        int[] out = new int[toks.length];
        for (int i = 0; i < toks.length; i++) {
            try {
                int v = Integer.parseInt(toks[i].trim());
                int abs = Math.abs(v);
                if (abs < min || abs > max || (!allowNegative && v < 0)) {
                    throw new ParseException("Value out of range for " + part + ": " + toks[i], 0);
                }
                out[i] = v;
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid " + part + " value: " + toks[i], 0);
            }
        }
        return out;
    }

    private static List<WeekdayNum> parseByDay(String val) throws ParseException {
        List<WeekdayNum> out = new ArrayList<>();
        for (String rawTok : val.split(",")) {
            String tok = rawTok.trim().toUpperCase(Locale.US);
            if (tok.length() < 2) {
                throw new ParseException("Invalid BYDAY value: " + rawTok, 0);
            }
            String dayCode = tok.substring(tok.length() - 2);
            Integer calDay = weekdayCode(dayCode);
            if (calDay == null) {
                throw new ParseException("Invalid BYDAY weekday: " + rawTok, 0);
            }
            String ord = tok.substring(0, tok.length() - 2);
            int ordinal = 0;
            if (!ord.isEmpty()) {
                try {
                    ordinal = Integer.parseInt(ord);
                    if (ordinal == 0 || Math.abs(ordinal) > 53) {
                        throw new ParseException("Invalid BYDAY ordinal: " + rawTok, 0);
                    }
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid BYDAY ordinal: " + rawTok, 0);
                }
            }
            out.add(new WeekdayNum(ordinal, calDay));
        }
        return out;
    }

    private static Integer weekdayCode(String code) {
        switch (code) {
            case "SU": return Calendar.SUNDAY;
            case "MO": return Calendar.MONDAY;
            case "TU": return Calendar.TUESDAY;
            case "WE": return Calendar.WEDNESDAY;
            case "TH": return Calendar.THURSDAY;
            case "FR": return Calendar.FRIDAY;
            case "SA": return Calendar.SATURDAY;
            default: return null;
        }
    }

    private static String dayName(int calendarDay) {
        switch (calendarDay) {
            case Calendar.SUNDAY: return "SU";
            case Calendar.MONDAY: return "MO";
            case Calendar.TUESDAY: return "TU";
            case Calendar.WEDNESDAY: return "WE";
            case Calendar.THURSDAY: return "TH";
            case Calendar.FRIDAY: return "FR";
            case Calendar.SATURDAY: return "SA";
            default: return "?";
        }
    }

    private static void appendPart(StringBuilder sb, String name, int[] values) {
        if (values != null) {
            sb.append(name).append(": ").append(Arrays.toString(values)).append("\n");
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Evaluation helpers.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Date startTimeSeconds() {
        return new Date(floorSecond(startTime.getTime()));
    }

    private static long floorSecond(long millis) {
        return millis - Math.floorMod(millis, 1000L);
    }

    private Calendar resolveUntil() {
        if (!hasUntil) {
            return null;
        }
        TimeZone tz = untilUtc ? TimeZone.getTimeZone("UTC") : getTimeZone();
        Calendar cal = Calendar.getInstance(tz, Locale.US);
        cal.clear();
        cal.set(untilYear, untilMonth - 1, untilDay, untilHour, untilMinute, untilSecond);
        return cal;
    }

    private Calendar anchorCalendar() {
        Calendar cal = Calendar.getInstance(getTimeZone(), Locale.US);
        cal.setFirstDayOfWeek(wkst);
        cal.setMinimalDaysInFirstWeek(4);
        cal.setTime(startTimeSeconds());
        return cal;
    }

    /**
     * Lazily produces the recurrence instants (as epoch-millis) in ascending
     * order, beginning with the first period whose window can contain
     * {@code scanFrom}. The iterator is finite only because of the
     * {@link #MAX_YEAR} / {@link #MAX_PERIODS} guards.
     */
    private final class OccurrenceScan implements Iterable<Long> {
        private final long scanFrom;

        OccurrenceScan(long scanFrom) {
            this.scanFrom = scanFrom;
        }

        @Override
        public java.util.Iterator<Long> iterator() {
            return new java.util.Iterator<Long>() {
                private final Calendar periodCursor = firstPeriod(scanFrom);
                private java.util.Iterator<Long> current = java.util.Collections.emptyIterator();
                private Long nextValue = advance();
                private int periodsScanned = 0;

                private Long advance() {
                    while (true) {
                        if (current.hasNext()) {
                            return current.next();
                        }
                        if (periodsScanned++ > MAX_PERIODS
                                || periodCursor.get(Calendar.YEAR) > MAX_YEAR) {
                            return null;
                        }
                        List<Long> occ = expandPeriod(periodCursor);
                        stepPeriod(periodCursor);
                        current = occ.iterator();
                    }
                }

                @Override
                public boolean hasNext() {
                    return nextValue != null;
                }

                @Override
                public Long next() {
                    Long v = nextValue;
                    nextValue = advance();
                    return v;
                }
            };
        }
    }

    /** Returns the calendar positioned at the start of the first period to scan. */
    private Calendar firstPeriod(long scanFrom) {
        Calendar cursor = anchorCalendar();
        long anchor = cursor.getTimeInMillis();
        if (scanFrom <= anchor) {
            return cursor;
        }
        // Jump close to scanFrom, then the period iterator handles the remainder.
        int periods = estimatePeriods(anchor, scanFrom);
        if (periods > 1) {
            addPeriods(cursor, periods - 1);
        }
        return cursor;
    }

    private int estimatePeriods(long anchor, long target) {
        long deltaSeconds = (target - anchor) / 1000L;
        long p;
        switch (freq) {
            case SECONDLY:
                p = deltaSeconds / interval;
                break;
            case MINUTELY:
                p = deltaSeconds / (60L * interval);
                break;
            case HOURLY:
                p = deltaSeconds / (3600L * interval);
                break;
            case DAILY:
                p = deltaSeconds / (86400L * interval);
                break;
            case WEEKLY:
                p = deltaSeconds / (7L * 86400L * interval);
                break;
            case MONTHLY:
                p = (deltaSeconds / (28L * 86400L)) / interval;
                break;
            case YEARLY:
            default:
                p = (deltaSeconds / (365L * 86400L)) / interval;
                break;
        }
        if (p < 0) {
            p = 0;
        }
        if (p > MAX_PERIODS) {
            p = MAX_PERIODS;
        }
        return (int) p;
    }

    private void stepPeriod(Calendar cursor) {
        addPeriods(cursor, 1);
    }

    private void addPeriods(Calendar cursor, int n) {
        switch (freq) {
            case SECONDLY:
                cursor.add(Calendar.SECOND, n * interval);
                break;
            case MINUTELY:
                cursor.add(Calendar.MINUTE, n * interval);
                break;
            case HOURLY:
                cursor.add(Calendar.HOUR_OF_DAY, n * interval);
                break;
            case DAILY:
                cursor.add(Calendar.DAY_OF_MONTH, n * interval);
                break;
            case WEEKLY:
                cursor.add(Calendar.WEEK_OF_YEAR, n * interval);
                break;
            case MONTHLY:
                cursor.add(Calendar.MONTH, n * interval);
                break;
            case YEARLY:
            default:
                cursor.add(Calendar.YEAR, n * interval);
                break;
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Per-period expansion (RFC 5545 3.3.10 expand/limit semantics).
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private List<Long> expandPeriod(Calendar periodStart) {
        List<Long> result;
        switch (freq) {
            case SECONDLY:
            case MINUTELY:
            case HOURLY:
                result = expandSubDaily(periodStart);
                break;
            case DAILY:
                result = expandDaily(periodStart);
                break;
            case WEEKLY:
                result = expandWeekly(periodStart);
                break;
            case MONTHLY:
                result = expandMonthly(periodStart);
                break;
            case YEARLY:
            default:
                result = expandYearly(periodStart);
                break;
        }
        // BYSETPOS selects from the whole period's candidate set (RFC 5545),
        // so it must be applied before the anchor lower-bound is enforced.
        TreeSet<Long> sorted = new TreeSet<>(result);
        List<Long> selected = applySetPos(new ArrayList<>(sorted));
        long anchor = startTimeSeconds().getTime();
        List<Long> out = new ArrayList<>(selected.size());
        for (long v : selected) {
            if (v >= anchor) {
                out.add(v);
            }
        }
        return out;
    }

    private List<Long> applySetPos(List<Long> occ) {
        if (bySetPos == null || occ.isEmpty()) {
            return occ;
        }
        List<Long> out = new ArrayList<>();
        int size = occ.size();
        TreeSet<Integer> picked = new TreeSet<>();
        for (int pos : bySetPos) {
            int idx = pos > 0 ? pos - 1 : size + pos;
            if (idx >= 0 && idx < size) {
                picked.add(idx);
            }
        }
        for (int idx : picked) {
            out.add(occ.get(idx));
        }
        return out;
    }

    // ---- sub-daily (SECONDLY / MINUTELY / HOURLY) --------------------------

    private List<Long> expandSubDaily(Calendar periodStart) {
        List<Long> out = new ArrayList<>();
        int y = periodStart.get(Calendar.YEAR);
        int mo = periodStart.get(Calendar.MONTH) + 1;
        int dom = periodStart.get(Calendar.DAY_OF_MONTH);
        int doy = periodStart.get(Calendar.DAY_OF_YEAR);
        int dow = periodStart.get(Calendar.DAY_OF_WEEK);
        int h = periodStart.get(Calendar.HOUR_OF_DAY);
        int mi = periodStart.get(Calendar.MINUTE);
        int s = periodStart.get(Calendar.SECOND);

        // Date-level limits.
        if (!dayMatchesLimits(periodStart, y, mo, dom, doy, dow)) {
            return out;
        }
        // Time-level limits/expansions per frequency.
        int[] hours, minutes, seconds;
        if (freq == Freq.HOURLY) {
            if (byHour != null && !contains(byHour, h)) {
                return out;
            }
            hours = new int[] {h};
            minutes = byMinute != null ? byMinute : new int[] {mi};
            seconds = bySecond != null ? bySecond : new int[] {s};
        } else if (freq == Freq.MINUTELY) {
            if (byHour != null && !contains(byHour, h)) {
                return out;
            }
            if (byMinute != null && !contains(byMinute, mi)) {
                return out;
            }
            hours = new int[] {h};
            minutes = new int[] {mi};
            seconds = bySecond != null ? bySecond : new int[] {s};
        } else { // SECONDLY
            if (byHour != null && !contains(byHour, h)) {
                return out;
            }
            if (byMinute != null && !contains(byMinute, mi)) {
                return out;
            }
            if (bySecond != null && !contains(bySecond, s)) {
                return out;
            }
            hours = new int[] {h};
            minutes = new int[] {mi};
            seconds = new int[] {s};
        }
        for (int hh : hours) {
            for (int mm : minutes) {
                for (int ss : seconds) {
                    addInstant(out, periodStart, y, mo, dom, hh, mm, ss);
                }
            }
        }
        return out;
    }

    // ---- DAILY -------------------------------------------------------------

    private List<Long> expandDaily(Calendar periodStart) {
        List<Long> out = new ArrayList<>();
        int y = periodStart.get(Calendar.YEAR);
        int mo = periodStart.get(Calendar.MONTH) + 1;
        int dom = periodStart.get(Calendar.DAY_OF_MONTH);
        int doy = periodStart.get(Calendar.DAY_OF_YEAR);
        int dow = periodStart.get(Calendar.DAY_OF_WEEK);
        if (!dayMatchesLimits(periodStart, y, mo, dom, doy, dow)) {
            return out;
        }
        emitTimes(out, periodStart, y, mo, dom);
        return out;
    }

    // ---- WEEKLY ------------------------------------------------------------

    private List<Long> expandWeekly(Calendar periodStart) {
        List<Long> out = new ArrayList<>();
        // Walk the seven days of this week starting from WKST.
        Calendar dayCal = (Calendar) periodStart.clone();
        // Move back to the WKST day of the current week.
        int diff = dayCal.get(Calendar.DAY_OF_WEEK) - wkst;
        if (diff < 0) {
            diff += 7;
        }
        dayCal.add(Calendar.DAY_OF_MONTH, -diff);

        int[] anchorDow = new int[] {anchorCalendar().get(Calendar.DAY_OF_WEEK)};
        for (int i = 0; i < 7; i++) {
            int y = dayCal.get(Calendar.YEAR);
            int mo = dayCal.get(Calendar.MONTH) + 1;
            int dom = dayCal.get(Calendar.DAY_OF_MONTH);
            int dow = dayCal.get(Calendar.DAY_OF_WEEK);
            boolean dayOk;
            if (byDay != null) {
                dayOk = byDayContains(dow);
            } else {
                dayOk = dow == anchorDow[0];
            }
            if (dayOk && (byMonth == null || contains(byMonth, mo))) {
                emitTimes(out, dayCal, y, mo, dom);
            }
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return out;
    }

    // ---- MONTHLY -----------------------------------------------------------

    private List<Long> expandMonthly(Calendar periodStart) {
        List<Long> out = new ArrayList<>();
        int y = periodStart.get(Calendar.YEAR);
        int mo = periodStart.get(Calendar.MONTH) + 1;
        if (byMonth != null && !contains(byMonth, mo)) {
            return out;
        }
        int daysInMonth = daysInMonth(y, mo);
        TreeSet<Integer> days = new TreeSet<>();

        if (byMonthDay != null) {
            for (int md : byMonthDay) {
                int d = md > 0 ? md : daysInMonth + md + 1;
                if (d >= 1 && d <= daysInMonth) {
                    days.add(d);
                }
            }
            if (byDay != null) {
                days.removeIf(d -> !byDayContains(dayOfWeek(y, mo, d)));
            }
        } else if (byDay != null) {
            days.addAll(monthlyByDay(y, mo, daysInMonth));
        } else {
            int anchorDom = anchorCalendar().get(Calendar.DAY_OF_MONTH);
            if (anchorDom <= daysInMonth) {
                days.add(anchorDom);
            }
        }
        for (int d : days) {
            emitTimes(out, periodStart, y, mo, d);
        }
        return out;
    }

    private List<Integer> monthlyByDay(int y, int mo, int daysInMonth) {
        // Group matching day-of-month values by weekday so ordinals can select.
        List<Integer> result = new ArrayList<>();
        for (WeekdayNum w : byDay) {
            List<Integer> matches = new ArrayList<>();
            for (int d = 1; d <= daysInMonth; d++) {
                if (dayOfWeek(y, mo, d) == w.calendarDay) {
                    matches.add(d);
                }
            }
            if (w.ordinal == 0) {
                result.addAll(matches);
            } else if (w.ordinal > 0 && w.ordinal <= matches.size()) {
                result.add(matches.get(w.ordinal - 1));
            } else if (w.ordinal < 0 && -w.ordinal <= matches.size()) {
                result.add(matches.get(matches.size() + w.ordinal));
            }
        }
        return result;
    }

    // ---- YEARLY ------------------------------------------------------------

    private List<Long> expandYearly(Calendar periodStart) {
        List<Long> out = new ArrayList<>();
        int y = periodStart.get(Calendar.YEAR);
        int[] months = byMonth != null ? byMonth : null;

        TreeSet<int[]> dates = new TreeSet<>((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
        // each int[] is {month(1-12), day}

        if (byWeekNo != null) {
            collectByWeekNo(y, dates);
            if (byDay != null) {
                dates.removeIf(md -> !byDayContains(dayOfWeek(y, md[0], md[1])));
            }
            filterMonths(dates, months);
        } else if (byYearDay != null) {
            int daysInYear = daysInYear(y);
            for (int yd : byYearDay) {
                int d = yd > 0 ? yd : daysInYear + yd + 1;
                if (d >= 1 && d <= daysInYear) {
                    dates.add(yearDayToDate(y, d));
                }
            }
            if (byDay != null) {
                dates.removeIf(md -> !byDayContains(dayOfWeek(y, md[0], md[1])));
            }
            filterMonths(dates, months);
        } else if (byMonthDay != null) {
            int[] scope = months != null ? months : allMonths();
            for (int mo : scope) {
                int dim = daysInMonth(y, mo);
                for (int md : byMonthDay) {
                    int d = md > 0 ? md : dim + md + 1;
                    if (d >= 1 && d <= dim) {
                        dates.add(new int[] {mo, d});
                    }
                }
            }
            if (byDay != null) {
                dates.removeIf(md -> !byDayContains(dayOfWeek(y, md[0], md[1])));
            }
        } else if (byDay != null) {
            boolean hasOrdinal = false;
            for (WeekdayNum w : byDay) {
                if (w.ordinal != 0) {
                    hasOrdinal = true;
                    break;
                }
            }
            if (hasOrdinal && months == null) {
                collectYearlyOrdinalByDay(y, dates);
            } else {
                int[] scope = months != null ? months : allMonths();
                for (int mo : scope) {
                    dates.addAll(monthlyByDayAsDates(y, mo));
                }
            }
        } else {
            // No day-level rule parts: use the anchor's month/day within scope.
            Calendar anchor = anchorCalendar();
            int anchorMonth = anchor.get(Calendar.MONTH) + 1;
            int anchorDom = anchor.get(Calendar.DAY_OF_MONTH);
            int[] scope = months != null ? months : new int[] {anchorMonth};
            for (int mo : scope) {
                if (anchorDom <= daysInMonth(y, mo)) {
                    dates.add(new int[] {mo, anchorDom});
                }
            }
        }

        for (int[] md : dates) {
            emitTimes(out, periodStart, y, md[0], md[1]);
        }
        return out;
    }

    private void collectByWeekNo(int y, TreeSet<int[]> dates) {
        Calendar cal = Calendar.getInstance(getTimeZone(), Locale.US);
        cal.setFirstDayOfWeek(wkst);
        cal.setMinimalDaysInFirstWeek(4);
        cal.clear();
        cal.set(y, Calendar.JUNE, 15);
        int weeksInYear = cal.getActualMaximum(Calendar.WEEK_OF_YEAR);
        for (int wn : byWeekNo) {
            int week = wn > 0 ? wn : weeksInYear + wn + 1;
            for (int dowOffset = 0; dowOffset < 7; dowOffset++) {
                Calendar c = Calendar.getInstance(getTimeZone(), Locale.US);
                c.setFirstDayOfWeek(wkst);
                c.setMinimalDaysInFirstWeek(4);
                c.clear();
                c.set(Calendar.YEAR, y);
                c.set(Calendar.WEEK_OF_YEAR, week);
                c.set(Calendar.DAY_OF_WEEK, ((wkst - 1 + dowOffset) % 7) + 1);
                if (c.get(Calendar.YEAR) == y) {
                    dates.add(new int[] {c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)});
                }
            }
        }
    }

    private void collectYearlyOrdinalByDay(int y, TreeSet<int[]> dates) {
        int daysInYear = daysInYear(y);
        for (WeekdayNum w : byDay) {
            List<int[]> matches = new ArrayList<>();
            for (int d = 1; d <= daysInYear; d++) {
                int[] md = yearDayToDate(y, d);
                if (dayOfWeek(y, md[0], md[1]) == w.calendarDay) {
                    matches.add(md);
                }
            }
            if (w.ordinal == 0) {
                dates.addAll(matches);
            } else if (w.ordinal > 0 && w.ordinal <= matches.size()) {
                dates.add(matches.get(w.ordinal - 1));
            } else if (w.ordinal < 0 && -w.ordinal <= matches.size()) {
                dates.add(matches.get(matches.size() + w.ordinal));
            }
        }
    }

    private List<int[]> monthlyByDayAsDates(int y, int mo) {
        List<int[]> out = new ArrayList<>();
        for (int d : monthlyByDay(y, mo, daysInMonth(y, mo))) {
            out.add(new int[] {mo, d});
        }
        return out;
    }

    private void filterMonths(TreeSet<int[]> dates, int[] months) {
        if (months != null) {
            dates.removeIf(md -> !contains(months, md[0]));
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Shared helpers.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private boolean dayMatchesLimits(Calendar cal, int y, int mo, int dom, int doy, int dow) {
        if (byMonth != null && !contains(byMonth, mo)) {
            return false;
        }
        if (byYearDay != null) {
            int daysInYear = daysInYear(y);
            boolean ok = false;
            for (int yd : byYearDay) {
                int d = yd > 0 ? yd : daysInYear + yd + 1;
                if (d == doy) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                return false;
            }
        }
        if (byMonthDay != null) {
            int dim = daysInMonth(y, mo);
            boolean ok = false;
            for (int md : byMonthDay) {
                int d = md > 0 ? md : dim + md + 1;
                if (d == dom) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                return false;
            }
        }
        if (byDay != null && !byDayContains(dow)) {
            return false;
        }
        return true;
    }

    private void emitTimes(List<Long> out, Calendar dayRef, int y, int mo, int dom) {
        int[] hours = byHour != null ? byHour : new int[] {anchorCalendar().get(Calendar.HOUR_OF_DAY)};
        int[] minutes = byMinute != null ? byMinute : new int[] {anchorCalendar().get(Calendar.MINUTE)};
        int[] seconds = bySecond != null ? bySecond : new int[] {anchorCalendar().get(Calendar.SECOND)};
        for (int hh : hours) {
            for (int mm : minutes) {
                for (int ss : seconds) {
                    addInstant(out, dayRef, y, mo, dom, hh, mm, ss);
                }
            }
        }
    }

    private void addInstant(List<Long> out, Calendar ref, int y, int mo, int dom, int h, int mi, int s) {
        Calendar c = Calendar.getInstance(getTimeZone(), Locale.US);
        c.setLenient(true);
        c.clear();
        c.set(y, mo - 1, dom, h, mi, s);
        // Guard against non-existent civil dates (e.g. Feb 30) produced by lenient set.
        if (c.get(Calendar.YEAR) != y || c.get(Calendar.MONTH) != mo - 1
                || c.get(Calendar.DAY_OF_MONTH) != dom) {
            return;
        }
        out.add(c.getTimeInMillis());
    }

    private boolean byDayContains(int calendarDay) {
        for (WeekdayNum w : byDay) {
            if (w.calendarDay == calendarDay) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(int[] arr, int v) {
        for (int a : arr) {
            if (a == v) {
                return true;
            }
        }
        return false;
    }

    private int dayOfWeek(int y, int mo, int d) {
        GregorianCalendar c = new GregorianCalendar(getTimeZone(), Locale.US);
        c.clear();
        c.set(y, mo - 1, d);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    private int daysInMonth(int y, int mo) {
        GregorianCalendar c = new GregorianCalendar(getTimeZone(), Locale.US);
        c.clear();
        c.set(y, mo - 1, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private int daysInYear(int y) {
        GregorianCalendar c = new GregorianCalendar(getTimeZone(), Locale.US);
        c.clear();
        c.set(y, Calendar.JANUARY, 1);
        return c.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    private int[] yearDayToDate(int y, int doy) {
        GregorianCalendar c = new GregorianCalendar(getTimeZone(), Locale.US);
        c.clear();
        c.set(Calendar.YEAR, y);
        c.set(Calendar.DAY_OF_YEAR, doy);
        return new int[] {c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)};
    }

    private static int[] allMonths() {
        return new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    }
}
