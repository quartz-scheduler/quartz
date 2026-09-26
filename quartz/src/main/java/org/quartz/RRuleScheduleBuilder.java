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

import java.text.ParseException;
import java.util.TimeZone;

import org.quartz.impl.triggers.RRuleTriggerImpl;
import org.quartz.spi.MutableTrigger;

/**
 * <code>RRuleScheduleBuilder</code> is a {@link ScheduleBuilder} that defines
 * {@link RRuleExpression RFC&nbsp;5545 <code>RRULE</code>}-based schedules for
 * <code>Trigger</code>s.
 *
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related
 * entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes
 * <code>TriggerBuilder</code>, <code>JobBuilder</code>,
 * <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> and
 * the various <code>ScheduleBuilder</code> implementations.
 * </p>
 *
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 *
 * <pre>
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 *
 * Trigger trigger = newTrigger()
 *         .withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 *         .withSchedule(rruleSchedule(&quot;FREQ=DAILY;BYHOUR=10;BYMINUTE=0&quot;))
 *         .startAt(futureDate(10, MINUTES)).build();
 *
 * scheduler.scheduleJob(job, trigger);
 * </pre>
 *
 * @see RRuleExpression
 * @see RRuleTrigger
 * @see ScheduleBuilder
 * @see CronScheduleBuilder
 * @see TriggerBuilder
 */
public class RRuleScheduleBuilder extends ScheduleBuilder<RRuleTrigger> {

    private final RRuleExpression rruleExpression;
    private int misfireInstruction = RRuleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    protected RRuleScheduleBuilder(RRuleExpression rruleExpression) {
        if (rruleExpression == null) {
            throw new NullPointerException("rruleExpression cannot be null");
        }
        this.rruleExpression = rruleExpression;
    }

    /**
     * Build the actual Trigger -- NOT intended to be invoked by end users, but
     * will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
     * given to.
     *
     * @see TriggerBuilder#withSchedule(ScheduleBuilder)
     */
    @Override
    public MutableTrigger build() {

        RRuleTriggerImpl rt = new RRuleTriggerImpl();

        rt.setRRuleExpression(rruleExpression);
        rt.setTimeZone(rruleExpression.getTimeZone());
        rt.setMisfireInstruction(misfireInstruction);

        return rt;
    }

    /**
     * Create an RRuleScheduleBuilder with the given RRULE string - which is
     * presumed to be valid (and hence only a RuntimeException will be thrown if
     * it is not).
     *
     * @param rruleExpression the RRULE string to base the schedule on.
     * @return the new RRuleScheduleBuilder
     * @throws RuntimeException wrapping a ParseException if the expression is invalid
     * @see RRuleExpression
     */
    public static RRuleScheduleBuilder rruleSchedule(String rruleExpression) {
        try {
            return rruleSchedule(new RRuleExpression(rruleExpression));
        } catch (ParseException e) {
            // all methods of construction ensure the expression is valid by
            // this point...
            throw new RuntimeException("RRuleExpression '" + rruleExpression + "' is invalid.", e);
        }
    }

    /**
     * Create an RRuleScheduleBuilder with the given RRULE string - which may not
     * be valid (and hence a ParseException will be thrown if it is not).
     *
     * @param rruleExpression the RRULE string to base the schedule on.
     * @return the new RRuleScheduleBuilder
     * @throws ParseException if the expression is invalid
     * @see RRuleExpression
     */
    public static RRuleScheduleBuilder rruleScheduleNonvalidatedExpression(String rruleExpression)
            throws ParseException {
        return rruleSchedule(new RRuleExpression(rruleExpression));
    }

    private static RRuleScheduleBuilder rruleScheduleNoParseException(String presumedValidRRuleExpression) {
        try {
            return rruleSchedule(new RRuleExpression(presumedValidRRuleExpression));
        } catch (ParseException e) {
            // all methods of construction ensure the expression is valid by
            // this point...
            throw new RuntimeException("RRuleExpression '" + presumedValidRRuleExpression
                    + "' is invalid, which should not be possible, please report bug to Quartz developers.", e);
        }
    }

    /**
     * Create an RRuleScheduleBuilder with the given {@link RRuleExpression}.
     *
     * @param rruleExpression the RRULE expression to base the schedule on.
     * @return the new RRuleScheduleBuilder
     * @see RRuleExpression
     */
    public static RRuleScheduleBuilder rruleSchedule(RRuleExpression rruleExpression) {
        return new RRuleScheduleBuilder(rruleExpression);
    }

    /**
     * Create an RRuleScheduleBuilder with an RRULE that fires every day at the
     * given time (hour and minute).
     *
     * @param hour   the hour of day to fire
     * @param minute the minute of the given hour to fire
     * @return the new RRuleScheduleBuilder
     */
    public static RRuleScheduleBuilder dailyAtHourAndMinute(int hour, int minute) {
        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        String rrule = String.format("FREQ=DAILY;BYHOUR=%d;BYMINUTE=%d;BYSECOND=0", hour, minute);

        return rruleScheduleNoParseException(rrule);
    }

    /**
     * Create an RRuleScheduleBuilder with an RRULE that fires once per week on
     * the given day at the given time (hour and minute).
     *
     * @param dayOfWeek the day of the week to fire (use the constants on
     *                  {@link DateBuilder}, e.g. {@link DateBuilder#MONDAY})
     * @param hour      the hour of day to fire
     * @param minute    the minute of the given hour to fire
     * @return the new RRuleScheduleBuilder
     */
    public static RRuleScheduleBuilder weeklyOnDayAndHourAndMinute(int dayOfWeek, int hour, int minute) {
        DateBuilder.validateDayOfWeek(dayOfWeek);
        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        String rrule = String.format("FREQ=WEEKLY;BYDAY=%s;BYHOUR=%d;BYMINUTE=%d;BYSECOND=0",
                rfcDay(dayOfWeek), hour, minute);

        return rruleScheduleNoParseException(rrule);
    }

    /**
     * Create an RRuleScheduleBuilder with an RRULE that fires once per month on
     * the given day of month at the given time (hour and minute).
     *
     * @param dayOfMonth the day of the month to fire
     * @param hour       the hour of day to fire
     * @param minute     the minute of the given hour to fire
     * @return the new RRuleScheduleBuilder
     */
    public static RRuleScheduleBuilder monthlyOnDayAndHourAndMinute(int dayOfMonth, int hour, int minute) {
        DateBuilder.validateDayOfMonth(dayOfMonth);
        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        String rrule = String.format("FREQ=MONTHLY;BYMONTHDAY=%d;BYHOUR=%d;BYMINUTE=%d;BYSECOND=0",
                dayOfMonth, hour, minute);

        return rruleScheduleNoParseException(rrule);
    }

    private static String rfcDay(int quartzDayOfWeek) {
        switch (quartzDayOfWeek) {
            case DateBuilder.SUNDAY: return "SU";
            case DateBuilder.MONDAY: return "MO";
            case DateBuilder.TUESDAY: return "TU";
            case DateBuilder.WEDNESDAY: return "WE";
            case DateBuilder.THURSDAY: return "TH";
            case DateBuilder.FRIDAY: return "FR";
            case DateBuilder.SATURDAY: return "SA";
            default:
                throw new IllegalArgumentException("Invalid day of week: " + quartzDayOfWeek);
        }
    }

    /**
     * The <code>TimeZone</code> in which to base the schedule.
     *
     * @param timezone the time-zone for the schedule.
     * @return the updated RRuleScheduleBuilder
     * @see RRuleExpression#getTimeZone()
     */
    public RRuleScheduleBuilder inTimeZone(TimeZone timezone) {
        rruleExpression.setTimeZone(timezone);
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
     *
     * @return the updated RRuleScheduleBuilder
     * @see Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
     */
    public RRuleScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
        misfireInstruction = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link RRuleTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
     *
     * @return the updated RRuleScheduleBuilder
     * @see RRuleTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
     */
    public RRuleScheduleBuilder withMisfireHandlingInstructionDoNothing() {
        misfireInstruction = RRuleTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link RRuleTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
     *
     * @return the updated RRuleScheduleBuilder
     * @see RRuleTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
     */
    public RRuleScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
        misfireInstruction = RRuleTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        return this;
    }
}
