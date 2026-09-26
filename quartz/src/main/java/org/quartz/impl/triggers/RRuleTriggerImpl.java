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

package org.quartz.impl.triggers;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.RRuleExpression;
import org.quartz.RRuleScheduleBuilder;
import org.quartz.RRuleTrigger;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A concrete <code>{@link Trigger}</code> that is used to fire a
 * <code>{@link org.quartz.JobDetail}</code> at given moments in time, defined
 * with an RFC&nbsp;5545 <code>RRULE</code> recurrence rule.
 * </p>
 *
 * @see RRuleTrigger
 * @see RRuleExpression
 */
public class RRuleTriggerImpl extends AbstractTrigger<RRuleTrigger> implements RRuleTrigger, CoreTrigger {

    private static final long serialVersionUID = 8028016963996402142L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RRuleTriggerImpl.class);

    protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = RRuleExpression.MAX_YEAR;

    private RRuleExpression rruleEx = null;
    private Date startTime = null;
    private Date endTime = null;
    private Date nextFireTime = null;
    private Date previousFireTime = null;
    private transient TimeZone timeZone = null;

    /**
     * <p>
     * Create an <code>RRuleTrigger</code> with no settings.
     * </p>
     *
     * <p>
     * The start-time will also be set to the current time, and the time zone
     * will be set to the system's default time zone.
     * </p>
     */
    public RRuleTriggerImpl() {
        super();
        setStartTime(new Date());
        setTimeZone(TimeZone.getDefault());
    }

    @Override
    public Object clone() {
        RRuleTriggerImpl copy = (RRuleTriggerImpl) super.clone();
        if (rruleEx != null) {
            copy.setRRuleExpression(new RRuleExpression(rruleEx));
        }
        return copy;
    }

    public void setRRuleExpression(String rruleExpression) throws ParseException {
        TimeZone origTz = getTimeZone();
        this.rruleEx = new RRuleExpression(rruleExpression);
        this.rruleEx.setTimeZone(origTz);
        syncExpressionStartTime();
    }

    public String getRRuleExpression() {
        return rruleEx == null ? null : rruleEx.getExpression();
    }

    /**
     * Set the {@link RRuleExpression} to the given one. The <code>TimeZone</code>
     * on the passed-in expression over-rides any that was already set on the
     * trigger.
     */
    public void setRRuleExpression(RRuleExpression rruleExpression) {
        this.rruleEx = rruleExpression;
        this.timeZone = rruleExpression.getTimeZone();
        syncExpressionStartTime();
    }

    /** Keeps the expression's DTSTART anchor in sync with the trigger's start-time. */
    private void syncExpressionStartTime() {
        if (rruleEx != null && startTime != null) {
            rruleEx.setStartTime(startTime);
        }
    }

    @Override
    public Date getStartTime() {
        return this.startTime;
    }

    @Override
    public void setStartTime(Date startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        Date eTime = getEndTime();
        if (eTime != null && eTime.before(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }

        // round off millisecond...
        Calendar cl = Calendar.getInstance();
        cl.setTime(startTime);
        cl.set(Calendar.MILLISECOND, 0);

        this.startTime = cl.getTime();
        syncExpressionStartTime();
    }

    @Override
    public Date getEndTime() {
        return this.endTime;
    }

    @Override
    public void setEndTime(Date endTime) {
        Date sTime = getStartTime();
        if (sTime != null && endTime != null && sTime.after(endTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }

        this.endTime = endTime;
    }

    @Override
    public Date getNextFireTime() {
        return this.nextFireTime;
    }

    @Override
    public Date getPreviousFireTime() {
        return this.previousFireTime;
    }

    /**
     * <p>
     * Sets the next time at which the <code>RRuleTrigger</code> will fire.
     * <b>This method should not be invoked by client code.</b>
     * </p>
     */
    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    /**
     * <p>
     * Set the previous time at which the <code>RRuleTrigger</code> fired.
     * <b>This method should not be invoked by client code.</b>
     * </p>
     */
    public void setPreviousFireTime(Date previousFireTime) {
        this.previousFireTime = previousFireTime;
    }

    public TimeZone getTimeZone() {

        if (rruleEx != null) {
            return rruleEx.getTimeZone();
        }

        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    /**
     * <p>
     * Sets the time zone for which the <code>rruleExpression</code> of this
     * <code>RRuleTrigger</code> will be resolved.
     * </p>
     */
    public void setTimeZone(TimeZone timeZone) {
        if (rruleEx != null) {
            rruleEx.setTimeZone(timeZone);
        }
        this.timeZone = timeZone;
    }

    @Override
    public Date getFireTimeAfter(Date afterTime) {
        if (afterTime == null) {
            afterTime = new Date();
        }

        if (getStartTime().after(afterTime)) {
            afterTime = new Date(getStartTime().getTime() - 1000L);
        }

        if (getEndTime() != null && (afterTime.compareTo(getEndTime()) >= 0)) {
            return null;
        }

        Date pot = getTimeAfter(afterTime);
        if (getEndTime() != null && pot != null && pot.after(getEndTime())) {
            return null;
        }

        return pot;
    }

    @Override
    public Date getFinalFireTime() {
        Date resultTime;
        if (getEndTime() != null) {
            resultTime = getTimeBefore(new Date(getEndTime().getTime() + 1000L));
        } else {
            resultTime = (rruleEx == null) ? null : rruleEx.getFinalFireTime();
        }

        if ((resultTime != null) && (getStartTime() != null) && (resultTime.before(getStartTime()))) {
            return null;
        }

        return resultTime;
    }

    @Override
    public boolean mayFireAgain() {
        return (getNextFireTime() != null);
    }

    @Override
    protected boolean validateMisfireInstruction(int misfireInstruction) {
        return misfireInstruction >= MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
                && misfireInstruction <= MISFIRE_INSTRUCTION_DO_NOTHING;
    }

    /**
     * <p>
     * Updates the <code>RRuleTrigger</code>'s state based on the
     * MISFIRE_INSTRUCTION_XXX that was selected when the
     * <code>RRuleTrigger</code> was created.
     * </p>
     *
     * <p>
     * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY,
     * then the following scheme will be used:
     * </p>
     * <ul>
     * <li>The instruction will be interpreted as
     * <code>MISFIRE_INSTRUCTION_FIRE_ONCE_NOW</code></li>
     * </ul>
     */
    @Override
    public void updateAfterMisfire(org.quartz.Calendar cal) {
        int instr = getMisfireInstruction();

        if (instr == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
            return;
        }

        if (instr == MISFIRE_INSTRUCTION_SMART_POLICY) {
            instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        }

        if (instr == MISFIRE_INSTRUCTION_DO_NOTHING) {
            Date newFireTime = getFireTimeAfter(new Date());
            while (newFireTime != null && cal != null
                    && !cal.isTimeIncluded(newFireTime.getTime())) {
                newFireTime = getFireTimeAfter(newFireTime);
            }
            setNextFireTime(newFireTime);
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
            setNextFireTime(new Date());
        }
    }

    /**
     * <p>
     * Determines whether the date and (optionally) time of the given Calendar
     * instance falls on a scheduled fire-time of this trigger.
     * </p>
     *
     * <p>
     * Equivalent to calling <code>willFireOn(cal, false)</code>.
     * </p>
     *
     * @param test the date to compare
     * @see #willFireOn(Calendar, boolean)
     */
    public boolean willFireOn(Calendar test) {
        return willFireOn(test, false);
    }

    /**
     * <p>
     * Determines whether the date and (optionally) time of the given Calendar
     * instance falls on a scheduled fire-time of this trigger.
     * </p>
     *
     * @param test    the date to compare
     * @param dayOnly if set to true, the method will only determine if the
     *                trigger will fire during the day represented by the given
     *                Calendar (hours, minutes and seconds will be ignored).
     * @see #willFireOn(Calendar)
     */
    public boolean willFireOn(Calendar test, boolean dayOnly) {

        test = (Calendar) test.clone();

        test.set(Calendar.MILLISECOND, 0); // don't compare millis.

        if (dayOnly) {
            test.set(Calendar.HOUR_OF_DAY, 0);
            test.set(Calendar.MINUTE, 0);
            test.set(Calendar.SECOND, 0);
        }

        Date testTime = test.getTime();

        Date fta = getFireTimeAfter(new Date(test.getTime().getTime() - 1000));

        if (fta == null) {
            return false;
        }

        Calendar p = Calendar.getInstance(test.getTimeZone());
        p.setTime(fta);

        int year = p.get(Calendar.YEAR);
        int month = p.get(Calendar.MONTH);
        int day = p.get(Calendar.DATE);

        if (dayOnly) {
            return (year == test.get(Calendar.YEAR)
                    && month == test.get(Calendar.MONTH)
                    && day == test.get(Calendar.DATE));
        }

        while (fta.before(testTime)) {
            fta = getFireTimeAfter(fta);
        }

        return fta.equals(testTime);
    }

    /**
     * <p>
     * Called when the <code>{@link Scheduler}</code> has decided to 'fire'
     * the trigger (execute the associated <code>Job</code>), in order to
     * give the <code>Trigger</code> a chance to update itself for its next
     * triggering (if any).
     * </p>
     *
     * @see #executionComplete(JobExecutionContext, JobExecutionException)
     */
    @Override
    public void triggered(org.quartz.Calendar calendar) {
        previousFireTime = nextFireTime;
        nextFireTime = getFireTimeAfter(nextFireTime);

        while (nextFireTime != null && calendar != null
                && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }
    }

    /**
     * @see AbstractTrigger#updateWithNewCalendar(org.quartz.Calendar, long)
     */
    @Override
    public void updateWithNewCalendar(org.quartz.Calendar calendar, long misfireThreshold) {
        nextFireTime = getFireTimeAfter(previousFireTime);

        if (nextFireTime == null || calendar == null) {
            return;
        }

        Date now = new Date();
        while (nextFireTime != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {

            nextFireTime = getFireTimeAfter(nextFireTime);

            if (nextFireTime == null) {
                break;
            }

            // avoid infinite loop
            // Use gregorian only because the constant is based on Gregorian
            java.util.Calendar c = new java.util.GregorianCalendar();
            c.setTime(nextFireTime);
            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                nextFireTime = null;
            }

            if (nextFireTime != null && nextFireTime.before(now)) {
                long diff = now.getTime() - nextFireTime.getTime();
                if (diff >= misfireThreshold) {
                    nextFireTime = getFireTimeAfter(nextFireTime);
                }
            }
        }
    }

    /**
     * <p>
     * Called by the scheduler at the time a <code>Trigger</code> is first
     * added to the scheduler, in order to have the <code>Trigger</code>
     * compute its first fire time, based on any associated calendar.
     * </p>
     *
     * @return the first time at which the <code>Trigger</code> will be fired
     *         by the scheduler, which is also the same value
     *         <code>getNextFireTime()</code> will return (until after the first
     *         firing of the <code>Trigger</code>).
     */
    @Override
    public Date computeFirstFireTime(org.quartz.Calendar calendar) {
        nextFireTime = getFireTimeAfter(new Date(getStartTime().getTime() - 1000L));

        while (nextFireTime != null && calendar != null
                && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            nextFireTime = getFireTimeAfter(nextFireTime);
        }

        return nextFireTime;
    }

    public String getExpressionSummary() {
        return rruleEx == null ? null : rruleEx.getExpressionSummary();
    }

    /**
     * Used by extensions of RRuleTrigger to imply that there are additional
     * properties, specifically so that extensions can choose whether to be
     * stored as a serialized blob, or as a flattened RRuleTrigger table.
     */
    public boolean hasAdditionalProperties() {
        return false;
    }

    /**
     * Get a {@link ScheduleBuilder} that is configured to produce a schedule
     * identical to this trigger's schedule.
     *
     * @see #getTriggerBuilder()
     */
    @Override
    public ScheduleBuilder<RRuleTrigger> getScheduleBuilder() {

        RRuleScheduleBuilder cb = RRuleScheduleBuilder.rruleSchedule(getRRuleExpression())
                .inTimeZone(getTimeZone());

        int misfireInstruction = getMisfireInstruction();
        switch (misfireInstruction) {
            case MISFIRE_INSTRUCTION_SMART_POLICY:
                break;
            case MISFIRE_INSTRUCTION_DO_NOTHING:
                cb.withMisfireHandlingInstructionDoNothing();
                break;
            case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
                cb.withMisfireHandlingInstructionFireAndProceed();
                break;
            case MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY:
                cb.withMisfireHandlingInstructionIgnoreMisfires();
                break;
            default:
                LOGGER.warn("Unrecognized misfire policy {}. Derived builder will use the default RRULE trigger "
                        + "behavior (MISFIRE_INSTRUCTION_FIRE_ONCE_NOW)", misfireInstruction);
        }

        return cb;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Computation Functions
    //
    ////////////////////////////////////////////////////////////////////////////

    protected Date getTimeAfter(Date afterTime) {
        return (rruleEx == null) ? null : rruleEx.getTimeAfter(afterTime);
    }

    /**
     * Returns the time before the given time that this <code>RRuleTrigger</code>
     * will fire.
     */
    protected Date getTimeBefore(Date eTime) {
        return (rruleEx == null) ? null : rruleEx.getTimeBefore(eTime);
    }
}
