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
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
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
 * trigger's {@link Trigger#getStartTime() start-time}. The <code>DTSTART</code>
 * also supplies the default value of any time component (second, minute, hour)
 * and of the day (weekday, day of month, month) that the rule itself does not
 * constrain. If no anchor is set (e.g. for an
 * {@link org.quartz.impl.calendar.RRuleCalendar}), midnight of 1970-01-01 in the
 * expression's time zone is used.
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
 * in RFC&nbsp;5545 &sect;3.3.10. Combinations that the RFC marks as N/A or
 * forbids (e.g. <code>BYMONTHDAY</code> with <code>FREQ=WEEKLY</code>) are
 * rejected with a {@link ParseException} rather than silently ignored.
 * </p>
 *
 * <p>
 * Deviations from, and clarifications of, RFC&nbsp;5545:
 * </p>
 * <ul>
 * <li>An <code>UNTIL</code> value without a trailing <code>Z</code> is
 * interpreted in the expression's time zone (the RFC requires a UTC value when
 * <code>DTSTART</code> has a time zone). A date-only <code>UNTIL</code> (e.g.
 * <code>UNTIL=20260107</code>) includes the whole day.</li>
 * <li><code>BYSECOND=60</code> (a leap second) is rejected, as Java time has no
 * leap seconds.</li>
 * <li>Local times that fall into a daylight-saving gap are shifted forward by
 * the length of the gap; ambiguous local times (in a daylight-saving overlap)
 * resolve to the first occurrence, as RFC&nbsp;5545 &sect;3.3.5 prescribes.</li>
 * </ul>
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

    /**
     * Safety bound on the number of consecutive empty periods scanned in one
     * call, after which the rule is treated as exhausted. It does not limit how
     * far from the anchor a query can be (that is computed directly); it only
     * stops rules that can never match, such as
     * <code>FREQ=SECONDLY;INTERVAL=2;BYSECOND=5</code> (for an anchor on an even
     * second), from scanning up to {@link #MAX_YEAR}. Empty days, hours and
     * minutes of sub-daily rules are skipped as a whole, so rules that do match
     * stay far below this bound.
     */
    private static final int MAX_EMPTY_PERIODS = 1_000_000;

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

    // UNTIL is stored as its parsed wall-clock value plus a flag telling whether
    // it was expressed in UTC ("...Z"). This lets the effective instant be
    // re-resolved whenever the time zone changes.
    private LocalDateTime until;      // null == not present
    private boolean untilUtc;

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
    private Date startTime; // DTSTART anchor; null == midnight 1970-01-01 local

    // Derived evaluation state; rebuilt lazily whenever the time zone or the
    // anchor changes.
    private transient volatile Context context;
    private transient CountCursor countCursor;  // guarded by this
    private transient Context finalFireTimeContext; // guarded by this
    private transient Long finalFireTime;       // guarded by this

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
        setStartTime(expression.startTime);
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
        long rounded = floorSecond(date.getTime());
        if (rounded < context().anchor) {
            return false;
        }
        Date next = getTimeAfter(new Date(rounded - 1000L));
        return next != null && next.getTime() == rounded;
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
        long candidate = floorSecond(date.getTime()) + 1000L; // step in whole seconds
        if (count != null) {
            Date next = getTimeAfter(new Date(candidate - 1000L));
            while (next != null && next.getTime() == candidate) {
                candidate += 1000L;
                next = getTimeAfter(next);
            }
            return new Date(candidate);
        }
        Iterator<Long> it = occurrencesAfter(context(), candidate - 1000L);
        while (it.hasNext() && it.next() == candidate) {
            candidate += 1000L;
        }
        return new Date(candidate);
    }

    /**
     * Returns the next time the rule fires strictly after the given time, or
     * <code>null</code> if the rule will not fire again.
     */
    public Date getTimeAfter(Date afterTime) {
        Context ctx = context();
        long lowerBound = Math.max(floorSecond(afterTime.getTime()), ctx.anchor - 1000L); // exclusive
        Long next;
        if (count != null) {
            next = countedTimeAfter(ctx, lowerBound);
        } else {
            Iterator<Long> it = occurrencesAfter(ctx, lowerBound);
            next = it.hasNext() ? it.next() : null;
        }
        return next == null ? null : new Date(next);
    }

    /**
     * Returns the last time the rule fires strictly before the given time, or
     * <code>null</code> if there is no such time.
     */
    public Date getTimeBefore(Date endTime) {
        Context ctx = context();
        long end = endTime.getTime(); // exclusive
        if (count != null) {
            Long best = null;
            long emitted = 0;
            Iterator<Long> it = forward(ctx, 0);
            while (emitted < count && it.hasNext()) {
                long occ = it.next();
                if (occ >= end) {
                    break;
                }
                best = occ;
                emitted++;
            }
            return best == null ? null : new Date(best);
        }
        if (ctx.until != null) {
            end = Math.min(end, ctx.until + 1L);
        }
        end = Math.min(end, toMillis(LocalDate.of(MAX_YEAR + 1, 1, 1).atStartOfDay(), ctx.zone));
        if (end <= ctx.anchor) {
            return null;
        }
        // The period after the one containing 'end' may still hold earlier
        // instants (e.g. the clock-hour of an HOURLY period), so start there.
        Iterator<Long> it = backward(ctx, periodIndexAt(ctx, end) + 1);
        while (it.hasNext()) {
            long occ = it.next();
            if (occ < end) {
                return new Date(occ);
            }
        }
        return null;
    }

    /**
     * Returns the final time the rule will ever fire (for rules bounded by
     * <code>COUNT</code> or <code>UNTIL</code>), or <code>null</code> for an
     * unbounded rule.
     */
    public Date getFinalFireTime() {
        Context ctx = context();
        if (count == null) {
            return ctx.until == null ? null : getTimeBefore(new Date(ctx.until + 1L));
        }
        synchronized (this) {
            if (finalFireTimeContext != ctx) {
                Long last = null;
                long emitted = 0;
                Iterator<Long> it = forward(ctx, 0);
                while (emitted < count && it.hasNext()) {
                    last = it.next();
                    emitted++;
                }
                finalFireTime = last;
                finalFireTimeContext = ctx;
            }
            return finalFireTime == null ? null : new Date(finalFireTime);
        }
    }

    public TimeZone getTimeZone() {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        this.context = null;
    }

    /**
     * Returns the <code>DTSTART</code> anchor used to seed the recurrence. If no
     * anchor was set, this is midnight of 1970-01-01 in the expression's time
     * zone.
     */
    public Date getStartTime() {
        return startTime != null ? new Date(startTime.getTime()) : new Date(context().anchor);
    }

    /**
     * Sets the <code>DTSTART</code> anchor used to seed the recurrence. Passing
     * <code>null</code> restores the default anchor (midnight of 1970-01-01 in
     * the expression's time zone).
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime == null ? null : new Date(startTime.getTime());
        this.context = null;
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
        Long untilMillis = context().until;
        if (untilMillis != null) {
            sb.append("until: ").append(new Date(untilMillis)).append("\n");
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
        Set<String> seen = new HashSet<>();
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
            if (!seen.add(name)) {
                throw new ParseException("RRULE part must not occur more than once: " + name, 0);
            }
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
                    // RFC 5545 allows 60 (leap second), which Java time cannot represent.
                    bySecond = parseIntList(val, 0, 59, false, "BYSECOND");
                    break;
                case "BYSETPOS":
                    bySetPos = parseIntList(val, 1, 366, true, "BYSETPOS");
                    break;
                default:
                    throw new ParseException("Unsupported RRULE part: " + name, 0);
            }
        }
        validate();
    }

    /** Rejects the combinations that RFC 5545 §3.3.10 marks as N/A or forbids. */
    private void validate() throws ParseException {
        if (freq == null) {
            throw new ParseException("RRULE is missing the required FREQ part", 0);
        }
        if (count != null && until != null) {
            throw new ParseException("RRULE must not contain both COUNT and UNTIL", 0);
        }
        if (byWeekNo != null && freq != Freq.YEARLY) {
            throw new ParseException("BYWEEKNO is only valid with FREQ=YEARLY", 0);
        }
        if (byYearDay != null && (freq == Freq.DAILY || freq == Freq.WEEKLY || freq == Freq.MONTHLY)) {
            throw new ParseException("BYYEARDAY is not valid with FREQ=" + freq, 0);
        }
        if (byMonthDay != null && freq == Freq.WEEKLY) {
            throw new ParseException("BYMONTHDAY is not valid with FREQ=WEEKLY", 0);
        }
        if (byDay != null && hasOrdinalByDay()) {
            if (freq != Freq.MONTHLY && freq != Freq.YEARLY) {
                throw new ParseException(
                        "Ordinal BYDAY values are only valid with FREQ=MONTHLY or FREQ=YEARLY", 0);
            }
            if (byWeekNo != null) {
                throw new ParseException("Ordinal BYDAY values are not valid together with BYWEEKNO", 0);
            }
        }
        if (bySetPos != null && byMonth == null && byWeekNo == null && byYearDay == null
                && byMonthDay == null && byDay == null && byHour == null && byMinute == null
                && bySecond == null) {
            throw new ParseException("BYSETPOS must be used together with another BYxxx rule part", 0);
        }
    }

    private boolean hasOrdinalByDay() {
        for (WeekdayNum w : byDay) {
            if (w.ordinal != 0) {
                return true;
            }
        }
        return false;
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
            boolean utc = v.endsWith("Z") || v.endsWith("z");
            if (utc) {
                v = v.substring(0, v.length() - 1);
            }
            LocalDate date = LocalDate.of(
                    Integer.parseInt(v.substring(0, 4)),
                    Integer.parseInt(v.substring(4, 6)),
                    Integer.parseInt(v.substring(6, 8)));
            if (v.length() == 15 && (v.charAt(8) == 'T' || v.charAt(8) == 't')) {
                this.until = date.atTime(
                        Integer.parseInt(v.substring(9, 11)),
                        Integer.parseInt(v.substring(11, 13)),
                        Integer.parseInt(v.substring(13, 15)));
            } else if (v.length() == 8 && !utc) {
                // A date-only UNTIL includes the whole day.
                this.until = date.atTime(LocalTime.of(23, 59, 59));
            } else {
                throw new ParseException("Invalid UNTIL value: " + val, 0);
            }
            this.untilUtc = utc;
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

    private static DayOfWeek toDayOfWeek(int calendarDay) {
        return DayOfWeek.of(calendarDay == Calendar.SUNDAY ? 7 : calendarDay - 1);
    }

    private static void appendPart(StringBuilder sb, String name, int[] values) {
        if (values != null) {
            sb.append(name).append(": ").append(Arrays.toString(values)).append("\n");
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Evaluation context: everything derived from the time zone and anchor.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Context context() {
        Context ctx = context;
        if (ctx == null) {
            ctx = new Context();
            context = ctx;
        }
        return ctx;
    }

    private final class Context {
        final ZoneId zone;
        final long anchor;                // epoch millis, whole seconds
        final LocalDateTime anchorLocal;
        final Long until;                 // epoch millis, inclusive; null == none
        final WeekFields weekFields;
        final DayOfWeek wkstDay;
        // Day-level filters, including the defaults RFC 5545 takes from DTSTART.
        final int[] months;
        final int[] monthDays;
        final List<WeekdayNum> days;
        // Time-of-day expansions for DAILY and coarser frequencies.
        final int[] hours;
        final int[] minutes;
        final int[] seconds;

        Context() {
            zone = toZoneId(getTimeZone());
            anchor = startTime != null
                    ? floorSecond(startTime.getTime())
                    : LocalDate.of(1970, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli();
            anchorLocal = LocalDateTime.ofInstant(Instant.ofEpochMilli(anchor), zone);
            until = RRuleExpression.this.until == null ? null
                    : (untilUtc ? RRuleExpression.this.until.toInstant(ZoneOffset.UTC)
                                : RRuleExpression.this.until.atZone(zone).toInstant()).toEpochMilli();
            wkstDay = toDayOfWeek(wkst);
            weekFields = WeekFields.of(wkstDay, 4);

            int[] m = byMonth;
            int[] md = byMonthDay;
            List<WeekdayNum> d = byDay;
            if (byWeekNo == null && byYearDay == null && byMonthDay == null && byDay == null) {
                int calendarDay = anchorLocal.getDayOfWeek().getValue() % 7 + 1;
                switch (freq) {
                    case YEARLY:
                        md = new int[] {anchorLocal.getDayOfMonth()};
                        if (m == null) {
                            m = new int[] {anchorLocal.getMonthValue()};
                        }
                        break;
                    case MONTHLY:
                        md = new int[] {anchorLocal.getDayOfMonth()};
                        break;
                    case WEEKLY:
                        d = Collections.singletonList(new WeekdayNum(0, calendarDay));
                        break;
                    default:
                        break;
                }
            }
            months = m;
            monthDays = md;
            days = d;
            hours = byHour != null ? byHour : new int[] {anchorLocal.getHour()};
            minutes = byMinute != null ? byMinute : new int[] {anchorLocal.getMinute()};
            seconds = bySecond != null ? bySecond : new int[] {anchorLocal.getSecond()};
        }
    }

    private static ZoneId toZoneId(TimeZone tz) {
        try {
            return tz.toZoneId();
        } catch (DateTimeException e) {
            // A custom TimeZone without a known region id: fall back to its raw offset.
            return ZoneOffset.ofTotalSeconds(tz.getRawOffset() / 1000);
        }
    }

    private static long floorSecond(long millis) {
        return millis - Math.floorMod(millis, 1000L);
    }

    private static long toMillis(LocalDateTime ldt, ZoneId zone) {
        // atZone() shifts gap times forward and resolves overlaps to the earlier
        // offset, as RFC 5545 §3.3.5 prescribes.
        return ldt.atZone(zone).toInstant().toEpochMilli();
    }

    private static LocalDateTime toLocal(long millis, ZoneId zone) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // COUNT handling.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Remembers how far a COUNT-bounded recurrence has been enumerated, so that
     * the usual monotonic sequence of {@link #getTimeAfter(Date)} calls does not
     * have to re-enumerate from the anchor every time.
     */
    private static final class CountCursor {
        final Context context;
        final Iterator<Long> it;
        long emitted;
        long previous = Long.MIN_VALUE; // occurrence before 'last'
        long last = Long.MIN_VALUE;     // most recently enumerated occurrence

        CountCursor(Context context, Iterator<Long> it) {
            this.context = context;
            this.it = it;
        }
    }

    private synchronized Long countedTimeAfter(Context ctx, long lowerBound) {
        CountCursor cc = countCursor;
        if (cc != null && cc.context == ctx && cc.emitted > 0
                && cc.previous <= lowerBound && lowerBound < cc.last) {
            return cc.last;
        }
        if (cc == null || cc.context != ctx || cc.last > lowerBound) {
            cc = new CountCursor(ctx, forward(ctx, 0));
            countCursor = cc;
        }
        while (cc.emitted < count && cc.it.hasNext()) {
            long occ = cc.it.next();
            cc.emitted++;
            cc.previous = cc.last;
            cc.last = occ;
            if (occ > lowerBound) {
                return occ;
            }
        }
        return null;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Period iteration.
    //
    // Periods are numbered from 0 (the period containing the anchor). Period n
    // starts INTERVAL * n units after the anchor; the index of the period that
    // contains a given instant is computed exactly, so evaluation cost does not
    // depend on the distance between the anchor and the query.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Occurrences strictly after {@code lowerBound}, honouring UNTIL (not COUNT). */
    private Iterator<Long> occurrencesAfter(Context ctx, long lowerBound) {
        // Start one period early: an earlier start is always safe (instants at
        // or before the bound are skipped), a later one would miss instants.
        Iterator<Long> it = forward(ctx, periodIndexAt(ctx, lowerBound + 1L) - 1L);
        return new Iterator<Long>() {
            private Long next = advance();

            private Long advance() {
                while (it.hasNext()) {
                    long occ = it.next();
                    if (occ <= lowerBound) {
                        continue;
                    }
                    return ctx.until != null && occ > ctx.until ? null : occ;
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Long next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                Long v = next;
                next = advance();
                return v;
            }
        };
    }

    /** All occurrences from period {@code fromIndex} on, in ascending order. */
    private Iterator<Long> forward(Context ctx, long fromIndex) {
        return new PeriodIterator(ctx, Math.max(0L, fromIndex), true);
    }

    /** All occurrences from period {@code fromIndex} back to the anchor, in descending order. */
    private Iterator<Long> backward(Context ctx, long fromIndex) {
        return new PeriodIterator(ctx, fromIndex, false);
    }

    private final class PeriodIterator implements Iterator<Long> {
        private final Context ctx;
        private final boolean ascending;
        private long index;
        private long[] current = new long[0];
        private int pos;
        private int emptyPeriods;
        private boolean done;

        PeriodIterator(Context ctx, long index, boolean ascending) {
            this.ctx = ctx;
            this.index = index;
            this.ascending = ascending;
        }

        @Override
        public boolean hasNext() {
            while (pos >= current.length) {
                if (done || index < 0 || emptyPeriods > MAX_EMPTY_PERIODS) {
                    done = true;
                    return false;
                }
                if (periodYear(ctx, index) > MAX_YEAR) {
                    if (ascending) {
                        done = true;
                        return false;
                    }
                    index--;
                    continue;
                }
                Period p = expandPeriod(ctx, index);
                current = p.instants;
                pos = 0;
                emptyPeriods = current.length == 0 ? emptyPeriods + 1 : 0;
                if (!ascending) {
                    reverse(current);
                }
                index = ascending ? nextIndex(p) : previousIndex(p);
            }
            return true;
        }

        @Override
        public Long next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current[pos++];
        }

        private long nextIndex(Period p) {
            if (p.skipUntil == Long.MIN_VALUE) {
                return index + 1;
            }
            long step = stepMillis();
            return Math.max(index + 1, Math.floorDiv(p.skipUntil - ctx.anchor + step - 1, step));
        }

        private long previousIndex(Period p) {
            if (p.skipFrom == Long.MIN_VALUE) {
                return index - 1;
            }
            return Math.min(index - 1, Math.floorDiv(p.skipFrom - 1 - ctx.anchor, stepMillis()));
        }
    }

    private static void reverse(long[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            long t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    /**
     * The instants of one period. For an empty sub-daily period whose day, hour
     * or minute fails a limit, {@code [skipFrom, skipUntil)} is the enclosing
     * day, hour or minute, none of whose periods can match either.
     */
    private static final class Period {
        static final Period EMPTY = new Period(new long[0], Long.MIN_VALUE, Long.MIN_VALUE);
        final long[] instants;
        final long skipFrom;
        final long skipUntil;

        Period(long[] instants, long skipFrom, long skipUntil) {
            this.instants = instants;
            this.skipFrom = skipFrom;
            this.skipUntil = skipUntil;
        }
    }

    private boolean isSubDaily() {
        return freq == Freq.SECONDLY || freq == Freq.MINUTELY || freq == Freq.HOURLY;
    }

    private long stepMillis() {
        switch (freq) {
            case SECONDLY: return 1000L * interval;
            case MINUTELY: return 60_000L * interval;
            case HOURLY: return 3_600_000L * interval;
            default: throw new IllegalStateException("not a sub-daily frequency: " + freq);
        }
    }

    /** Index of the period whose span contains the given instant. */
    private long periodIndexAt(Context ctx, long millis) {
        if (isSubDaily()) {
            return Math.floorDiv(millis - ctx.anchor, stepMillis());
        }
        LocalDate target = toLocal(millis, ctx.zone).toLocalDate();
        LocalDate anchor = ctx.anchorLocal.toLocalDate();
        long units;
        switch (freq) {
            case DAILY:
                units = target.toEpochDay() - anchor.toEpochDay();
                break;
            case WEEKLY:
                // Both are WKST days, so the difference is a whole number of weeks.
                units = (weekStart(ctx, target).toEpochDay() - weekStart(ctx, anchor).toEpochDay()) / 7;
                break;
            case MONTHLY:
                units = (target.getYear() * 12L + target.getMonthValue())
                        - (anchor.getYear() * 12L + anchor.getMonthValue());
                break;
            default:
                units = target.getYear() - (long) anchor.getYear();
                break;
        }
        return Math.floorDiv(units, interval);
    }

    private static LocalDate weekStart(Context ctx, LocalDate day) {
        int sinceWkst = Math.floorMod(day.getDayOfWeek().getValue() - ctx.wkstDay.getValue(), 7);
        return day.minusDays(sinceWkst);
    }

    private long periodStartMillis(Context ctx, long index) {
        return ctx.anchor + index * stepMillis();
    }

    private LocalDate periodDate(Context ctx, long index) {
        LocalDate anchor = ctx.anchorLocal.toLocalDate();
        long n = index * interval;
        switch (freq) {
            case DAILY: return anchor.plusDays(n);
            case WEEKLY: return anchor.plusWeeks(n);
            case MONTHLY: return anchor.plusMonths(n);
            case YEARLY: return anchor.plusYears(n);
            default: return toLocal(periodStartMillis(ctx, index), ctx.zone).toLocalDate();
        }
    }

    private int periodYear(Context ctx, long index) {
        return periodDate(ctx, index).getYear();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Per-period expansion (RFC 5545 3.3.10 expand/limit semantics).
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Period expandPeriod(Context ctx, long index) {
        if (isSubDaily()) {
            return expandSubDaily(ctx, periodStartMillis(ctx, index));
        }
        LocalDate ref = periodDate(ctx, index);
        TreeSet<Long> instants = new TreeSet<>();
        LocalDate first;
        LocalDate last;
        switch (freq) {
            case DAILY:
                first = last = ref;
                break;
            case WEEKLY:
                first = weekStart(ctx, ref);
                last = first.plusDays(6);
                break;
            case MONTHLY:
                first = ref.withDayOfMonth(1);
                last = ref.withDayOfMonth(ref.lengthOfMonth());
                break;
            default:
                first = ref.withDayOfYear(1);
                last = ref.withDayOfYear(ref.lengthOfYear());
                break;
        }
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            if (!dayMatches(ctx, day)) {
                continue;
            }
            for (int h : ctx.hours) {
                for (int m : ctx.minutes) {
                    for (int s : ctx.seconds) {
                        instants.add(toMillis(day.atTime(h, m, s), ctx.zone));
                    }
                }
            }
        }
        return new Period(select(ctx, instants), Long.MIN_VALUE, Long.MIN_VALUE);
    }

    private Period expandSubDaily(Context ctx, long periodStart) {
        LocalDateTime local = toLocal(periodStart, ctx.zone);
        LocalDate day = local.toLocalDate();
        if (!dayMatches(ctx, day)) {
            return new Period(new long[0],
                    toMillis(day.atStartOfDay(), ctx.zone),
                    toMillis(day.plusDays(1).atStartOfDay(), ctx.zone));
        }
        // Work in absolute time within the period's clock hour / minute, so that
        // a repeated hour in a daylight-saving overlap is handled correctly.
        long minuteStart = periodStart - local.getSecond() * 1000L;
        long hourStart = minuteStart - local.getMinute() * 60_000L;
        if (byHour != null && !contains(byHour, local.getHour())) {
            return new Period(new long[0], hourStart, hourStart + 3_600_000L);
        }
        TreeSet<Long> instants = new TreeSet<>();
        if (freq == Freq.HOURLY) {
            int[] minutes = byMinute != null ? byMinute : new int[] {local.getMinute()};
            int[] seconds = bySecond != null ? bySecond : new int[] {local.getSecond()};
            for (int m : minutes) {
                for (int s : seconds) {
                    instants.add(hourStart + m * 60_000L + s * 1000L);
                }
            }
        } else {
            if (byMinute != null && !contains(byMinute, local.getMinute())) {
                return new Period(new long[0], minuteStart, minuteStart + 60_000L);
            }
            if (freq == Freq.MINUTELY) {
                int[] seconds = bySecond != null ? bySecond : new int[] {local.getSecond()};
                for (int s : seconds) {
                    instants.add(minuteStart + s * 1000L);
                }
            } else if (bySecond == null || contains(bySecond, local.getSecond())) {
                instants.add(periodStart);
            }
        }
        return new Period(select(ctx, instants), Long.MIN_VALUE, Long.MIN_VALUE);
    }

    /** Applies BYSETPOS to the period's candidate set, then the anchor lower bound. */
    private long[] select(Context ctx, TreeSet<Long> sorted) {
        if (sorted.isEmpty()) {
            return Period.EMPTY.instants;
        }
        // BYSETPOS selects from the whole period's candidate set (RFC 5545),
        // so it must be applied before the anchor lower-bound is enforced.
        List<Long> candidates = new ArrayList<>(sorted);
        List<Long> selected = candidates;
        if (bySetPos != null) {
            TreeSet<Integer> picked = new TreeSet<>();
            int size = candidates.size();
            for (int pos : bySetPos) {
                int idx = pos > 0 ? pos - 1 : size + pos;
                if (idx >= 0 && idx < size) {
                    picked.add(idx);
                }
            }
            selected = new ArrayList<>(picked.size());
            for (int idx : picked) {
                selected.add(candidates.get(idx));
            }
        }
        long[] out = new long[selected.size()];
        int n = 0;
        for (long v : selected) {
            if (v >= ctx.anchor) {
                out[n++] = v;
            }
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    /** Whether a day passes all day-level rule parts (BYMONTH .. BYDAY). */
    private boolean dayMatches(Context ctx, LocalDate day) {
        if (ctx.months != null && !contains(ctx.months, day.getMonthValue())) {
            return false;
        }
        if (byWeekNo != null && !weekNoMatches(ctx, day)) {
            return false;
        }
        if (byYearDay != null && !containsSigned(byYearDay, day.getDayOfYear(), day.lengthOfYear())) {
            return false;
        }
        if (ctx.monthDays != null && !containsSigned(ctx.monthDays, day.getDayOfMonth(), day.lengthOfMonth())) {
            return false;
        }
        return ctx.days == null || byDayMatches(ctx, day);
    }

    private boolean weekNoMatches(Context ctx, LocalDate day) {
        int week = day.get(ctx.weekFields.weekOfWeekBasedYear());
        int weekYear = day.get(ctx.weekFields.weekBasedYear());
        // The week containing December 28th is always the last week of its year.
        int weeksInYear = LocalDate.of(weekYear, 12, 28).get(ctx.weekFields.weekOfWeekBasedYear());
        return containsSigned(byWeekNo, week, weeksInYear);
    }

    private boolean byDayMatches(Context ctx, LocalDate day) {
        // Ordinals count within the month for MONTHLY, and for YEARLY with BYMONTH;
        // otherwise within the year.
        boolean inMonth = freq == Freq.MONTHLY || byMonth != null;
        for (WeekdayNum w : ctx.days) {
            if (toDayOfWeek(w.calendarDay) != day.getDayOfWeek()) {
                continue;
            }
            if (w.ordinal == 0) {
                return true;
            }
            int pos = inMonth ? day.getDayOfMonth() : day.getDayOfYear();
            int length = inMonth ? day.lengthOfMonth() : day.lengthOfYear();
            int nth = (pos - 1) / 7 + 1;
            int nthFromEnd = -((length - pos) / 7 + 1);
            if (w.ordinal == nth || w.ordinal == nthFromEnd) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code value} (1-based) matches a list of positive or negative (from-the-end) positions. */
    private static boolean containsSigned(int[] arr, int value, int length) {
        for (int a : arr) {
            if (a == value || a == value - length - 1) {
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
}
