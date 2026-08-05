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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;

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
 * The actual recurrence expansion is delegated to the
 * <a href="https://github.com/dmfs/lib-recur"><code>org.dmfs:lib-recur</code></a>
 * library; this class adapts it to the Quartz idioms used by
 * {@link org.quartz.impl.triggers.RRuleTriggerImpl}.
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

    private final String expression; // the original value, as supplied
    private final String body;       // the value with any leading "RRULE:" stripped

    private transient RecurrenceRule rule;

    private TimeZone timeZone = TimeZone.getDefault();
    private Date startTime = new Date(0L); // DTSTART anchor; normally set by the trigger

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
        String value = this.expression;
        if (value.regionMatches(true, 0, "RRULE:", 0, 6)) {
            value = value.substring(6);
        }
        this.body = value;
        // RFC-level validation for the parts where lib-recur is more permissive
        // than RFC 5545 (out-of-range BY* values, misplaced BYWEEKNO / ordinal
        // BYDAY). This keeps behaviour aligned with the custom implementation.
        validate(this.body);
        this.rule = compile(this.body);
    }

    /**
     * Constructs a new <code>RRuleExpression</code> as a copy of an existing one
     * (including its time zone and anchor).
     */
    public RRuleExpression(RRuleExpression expression) {
        try {
            this.expression = expression.getExpression();
            String value = this.expression;
            if (value.regionMatches(true, 0, "RRULE:", 0, 6)) {
                value = value.substring(6);
            }
            this.body = value;
            this.rule = compile(this.body);
        } catch (ParseException e) {
            // never happens; the source expression already parsed successfully
            throw new AssertionError(e);
        }
        setTimeZone(expression.getTimeZone());
        setStartTime(expression.getStartTime());
    }

    private static RecurrenceRule compile(String body) throws ParseException {
        try {
            return new RecurrenceRule(body);
        } catch (InvalidRecurrenceRuleException e) {
            throw new ParseException("Invalid RRULE '" + body + "': " + e.getMessage(), 0);
        }
    }

    private RecurrenceRule rule() {
        if (rule == null) {
            try {
                rule = new RecurrenceRule(body);
            } catch (InvalidRecurrenceRuleException e) {
                throw new AssertionError(e);
            }
        }
        return rule;
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
        if (rounded.getTime() < floorSecond(startTime.getTime())) {
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
        long anchor = floorSecond(startTime.getTime());
        RecurrenceRuleIterator it = rule().iterator(anchor, getTimeZone());
        it.fastForward(after + 1000L); // first instance strictly after 'after'
        if (!it.hasNext()) {
            return null;
        }
        long ms = it.nextMillis();
        if (yearOf(ms) > MAX_YEAR) {
            return null;
        }
        return new Date(ms);
    }

    /**
     * Returns the last time the rule fires strictly before the given time, or
     * <code>null</code> if there is no such time.
     */
    public Date getTimeBefore(Date endTime) {
        long end = endTime.getTime();
        long anchor = floorSecond(startTime.getTime());
        if (end <= anchor) {
            return null;
        }
        RecurrenceRuleIterator it = rule().iterator(anchor, getTimeZone());
        Long best = null;
        while (it.hasNext()) {
            long ms = it.peekMillis();
            if (ms >= end || yearOf(ms) > MAX_YEAR) {
                break;
            }
            best = it.nextMillis();
        }
        return best == null ? null : new Date(best);
    }

    /**
     * Returns the final time the rule will ever fire (for rules bounded by
     * <code>COUNT</code> or <code>UNTIL</code>), or <code>null</code> for an
     * unbounded rule.
     */
    public Date getFinalFireTime() {
        if (rule().isInfinite()) {
            return null;
        }
        long anchor = floorSecond(startTime.getTime());
        RecurrenceRuleIterator it = rule().iterator(anchor, getTimeZone());
        Long last = null;
        while (it.hasNext()) {
            long ms = it.nextMillis();
            if (yearOf(ms) > MAX_YEAR) {
                break;
            }
            last = ms;
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
        return rule().toString();
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
    // RFC-level validation (ranges + FREQ-dependent constraints).
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private static void validate(String body) throws ParseException {
        if (body.isEmpty()) {
            throw new ParseException("RRULE is empty", 0);
        }
        String freq = null;
        // first pass: locate FREQ (it may appear after the BY* parts)
        for (String rawPart : body.split(";")) {
            String part = rawPart.trim();
            int eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            if (part.substring(0, eq).trim().equalsIgnoreCase("FREQ")) {
                freq = part.substring(eq + 1).trim().toUpperCase(Locale.US);
            }
        }
        for (String rawPart : body.split(";")) {
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
                case "INTERVAL":
                case "COUNT":
                    requirePositive(val, name);
                    break;
                case "BYMONTH":
                    checkRange(val, 1, 12, false, name);
                    break;
                case "BYWEEKNO":
                    checkRange(val, 1, 53, true, name);
                    if (!"YEARLY".equals(freq)) {
                        throw new ParseException("BYWEEKNO is only valid with FREQ=YEARLY", 0);
                    }
                    break;
                case "BYYEARDAY":
                    checkRange(val, 1, 366, true, name);
                    break;
                case "BYMONTHDAY":
                    checkRange(val, 1, 31, true, name);
                    break;
                case "BYHOUR":
                    checkRange(val, 0, 23, false, name);
                    break;
                case "BYMINUTE":
                    checkRange(val, 0, 59, false, name);
                    break;
                case "BYSECOND":
                    checkRange(val, 0, 60, false, name);
                    break;
                case "BYSETPOS":
                    checkRange(val, 1, 366, true, name);
                    break;
                case "BYDAY":
                    checkByDay(val, freq);
                    break;
                default:
                    // FREQ / UNTIL / WKST and X- extensions are left to lib-recur.
                    break;
            }
        }
    }

    private static void requirePositive(String val, String part) throws ParseException {
        try {
            if (Integer.parseInt(val) <= 0) {
                throw new ParseException(part + " must be a positive integer: " + val, 0);
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid " + part + " value: " + val, 0);
        }
    }

    private static void checkRange(String val, int min, int max, boolean allowNegative, String part)
            throws ParseException {
        for (String tok : val.split(",")) {
            try {
                int v = Integer.parseInt(tok.trim());
                int abs = Math.abs(v);
                if (abs < min || abs > max || (!allowNegative && v < 0)) {
                    throw new ParseException("Value out of range for " + part + ": " + tok, 0);
                }
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid " + part + " value: " + tok, 0);
            }
        }
    }

    private static void checkByDay(String val, String freq) throws ParseException {
        for (String rawTok : val.split(",")) {
            String tok = rawTok.trim().toUpperCase(Locale.US);
            if (tok.length() < 2) {
                throw new ParseException("Invalid BYDAY value: " + rawTok, 0);
            }
            String dayCode = tok.substring(tok.length() - 2);
            if (!isWeekday(dayCode)) {
                throw new ParseException("Invalid BYDAY weekday: " + rawTok, 0);
            }
            String ord = tok.substring(0, tok.length() - 2);
            if (!ord.isEmpty()) {
                int ordinal;
                try {
                    ordinal = Integer.parseInt(ord);
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid BYDAY ordinal: " + rawTok, 0);
                }
                if (ordinal == 0 || Math.abs(ordinal) > 53) {
                    throw new ParseException("Invalid BYDAY ordinal: " + rawTok, 0);
                }
                if (!"MONTHLY".equals(freq) && !"YEARLY".equals(freq)) {
                    throw new ParseException(
                            "Ordinal BYDAY values are only valid with FREQ=MONTHLY or FREQ=YEARLY", 0);
                }
            }
        }
    }

    private static boolean isWeekday(String code) {
        switch (code) {
            case "SU":
            case "MO":
            case "TU":
            case "WE":
            case "TH":
            case "FR":
            case "SA":
                return true;
            default:
                return false;
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Helpers.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private static long floorSecond(long millis) {
        return millis - Math.floorMod(millis, 1000L);
    }

    private int yearOf(long millis) {
        GregorianCalendar c = new GregorianCalendar(getTimeZone(), Locale.US);
        c.setTimeInMillis(millis);
        return c.get(Calendar.YEAR);
    }
}
