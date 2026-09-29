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

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import org.quartz.Calendar;
import org.quartz.RRuleExpression;

/**
 * This implementation of the Calendar excludes the set of times expressed by a
 * given RFC&nbsp;5545 {@link org.quartz.RRuleExpression RRuleExpression}. For
 * example, you could use this calendar to exclude every Sunday using the
 * expression &quot;FREQ=WEEKLY;BYDAY=SU&quot;.
 * <P>
 * It is important to remember that the RRULE here describes a set of times to
 * be <I>excluded</I> from firing. Whereas the RRULE in
 * {@link org.quartz.RRuleTrigger RRuleTrigger} describes a set of times that
 * can be <I>included</I> for firing. Thus, if an <CODE>RRuleTrigger</CODE> has
 * a given RRULE and is associated with an <CODE>RRuleCalendar</CODE> with the
 * <I>same</I> expression, the calendar will exclude all the times the trigger
 * includes, and they will cancel each other out.
 *
 * @see org.quartz.impl.calendar.CronCalendar
 */
public class RRuleCalendar extends BaseCalendar {
    private static final long serialVersionUID = 4184450261423012348L;

    RRuleExpression rruleExpression;

    /**
     * Create an <CODE>RRuleCalendar</CODE> with the given RRULE and no
     * <CODE>baseCalendar</CODE>.
     *
     * @param expression a String representation of the desired RRULE
     */
    public RRuleCalendar(String expression) throws ParseException {
        this(null, expression, null);
    }

    /**
     * Create an <CODE>RRuleCalendar</CODE> with the given RRULE and
     * <CODE>baseCalendar</CODE>.
     *
     * @param baseCalendar the base calendar for this calendar instance &ndash;
     *                     see {@link BaseCalendar} for more information on base
     *                     calendar functionality
     * @param expression   a String representation of the desired RRULE
     */
    public RRuleCalendar(Calendar baseCalendar, String expression) throws ParseException {
        this(baseCalendar, expression, null);
    }

    /**
     * Create an <CODE>RRuleCalendar</CODE> with the given RRULE,
     * <CODE>baseCalendar</CODE>, and <code>TimeZone</code>.
     *
     * @param baseCalendar the base calendar for this calendar instance &ndash;
     *                     see {@link BaseCalendar} for more information on base
     *                     calendar functionality
     * @param expression   a String representation of the desired RRULE
     * @param timeZone     Specifies for which time zone the
     *                     <code>expression</code> should be interpreted. If
     *                     <code>timeZone</code> is <code>null</code> then
     *                     <code>TimeZone.getDefault()</code> will be used.
     */
    public RRuleCalendar(Calendar baseCalendar, String expression, TimeZone timeZone) throws ParseException {
        super(baseCalendar);
        this.rruleExpression = new RRuleExpression(expression);
        this.rruleExpression.setTimeZone(timeZone);
    }

    @Override
    public Object clone() {
        RRuleCalendar clone = (RRuleCalendar) super.clone();
        clone.rruleExpression = new RRuleExpression(rruleExpression);
        return clone;
    }

    /**
     * Returns the time zone for which the <code>RRuleExpression</code> of this
     * <code>RRuleCalendar</code> will be resolved.
     */
    @Override
    public TimeZone getTimeZone() {
        return rruleExpression.getTimeZone();
    }

    /**
     * Sets the time zone for which the <code>RRuleExpression</code> of this
     * <code>RRuleCalendar</code> will be resolved. If <code>timeZone</code> is
     * <code>null</code> then <code>TimeZone.getDefault()</code> will be used.
     */
    @Override
    public void setTimeZone(TimeZone timeZone) {
        rruleExpression.setTimeZone(timeZone);
    }

    /**
     * Determines whether the given time (in milliseconds) is 'included' by the
     * <CODE>RRuleCalendar</CODE>.
     *
     * @param timeInMillis the date/time to test
     * @return a boolean indicating whether the specified time is 'included' by
     *         the <CODE>RRuleCalendar</CODE>
     */
    @Override
    public boolean isTimeIncluded(long timeInMillis) {
        if ((getBaseCalendar() != null) && (!getBaseCalendar().isTimeIncluded(timeInMillis))) {
            return false;
        }

        return (!(rruleExpression.isSatisfiedBy(new Date(timeInMillis))));
    }

    /**
     * Determines the next time included by the <CODE>RRuleCalendar</CODE> after
     * the specified time.
     *
     * @param timeInMillis the initial date/time after which to find an included
     *                     time
     * @return the time in milliseconds representing the next time included after
     *         the specified time.
     */
    @Override
    public long getNextIncludedTime(long timeInMillis) {
        long nextIncludedTime = timeInMillis + 1; // plus one millisecond

        while (!isTimeIncluded(nextIncludedTime)) {

            // If the time is in a range excluded by this calendar, we can move
            // to the end of the excluded time range and continue testing from
            // there. Otherwise, if nextIncludedTime is excluded by the
            // baseCalendar, ask it the next time it includes and begin testing
            // from there. Failing this, add one millisecond and continue
            // testing.
            if (rruleExpression.isSatisfiedBy(new Date(nextIncludedTime))) {
                nextIncludedTime = rruleExpression.getNextInvalidTimeAfter(new Date(nextIncludedTime)).getTime();
            } else if ((getBaseCalendar() != null) && (!getBaseCalendar().isTimeIncluded(nextIncludedTime))) {
                nextIncludedTime = getBaseCalendar().getNextIncludedTime(nextIncludedTime);
            } else {
                nextIncludedTime++;
            }
        }

        return nextIncludedTime;
    }

    /**
     * Returns a string representing the properties of the
     * <CODE>RRuleCalendar</CODE>.
     *
     * @return the properties of the RRuleCalendar in a String format
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("base calendar: [");
        if (getBaseCalendar() != null) {
            buffer.append(getBaseCalendar().toString());
        } else {
            buffer.append("null");
        }
        buffer.append("], excluded RRULE expression: '");
        buffer.append(rruleExpression);
        buffer.append("'");
        return buffer.toString();
    }

    /**
     * Returns the object representation of the RRULE that defines the dates and
     * times this calendar excludes.
     *
     * @return the RRULE expression
     * @see org.quartz.RRuleExpression
     */
    public RRuleExpression getRRuleExpression() {
        return rruleExpression;
    }

    /**
     * Sets the RRULE expression for the calendar to a new value.
     *
     * @param expression the new string value to build an RRULE expression from
     * @throws ParseException if the string expression cannot be parsed
     */
    public void setRRuleExpression(String expression) throws ParseException {
        this.rruleExpression = new RRuleExpression(expression);
    }

    /**
     * Sets the RRULE expression for the calendar to a new value.
     *
     * @param expression the new RRULE expression
     */
    public void setRRuleExpression(RRuleExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }

        this.rruleExpression = expression;
    }
}
