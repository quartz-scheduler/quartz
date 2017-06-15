package org.quartz;

import java.util.Date;

import org.quartz.impl.triggers.RecurrenceRuleTriggerImpl;
import org.quartz.spi.MutableTrigger;

import biweekly.property.RecurrenceRule;
import biweekly.util.DayOfWeek;
import biweekly.util.Frequency;
import biweekly.util.Recurrence;

/**
 * <code>RecurrenceRuleScheduleBuilder</code> is a {@link ScheduleBuilder} that defines {@link RecurrenceRule}-based schedules for <code>Trigger</code>s.
 * 
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related entities via a Domain-Specific Language (DSL). The DSL can best be utilized through the usage of static imports of the methods on the classes
 * <code>TriggerBuilder</code>, <code>JobBuilder</code>, <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> and the various <code>ScheduleBuilder</code> implementations.
 * </p>
 * 
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 * 
 * <pre>
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 * 
 * Trigger trigger = newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;)).withSchedule(dailyAtHourAndMinute(10, 0))
 *         .startAt(futureDate(10, MINUTES)).build();
 * 
 * scheduler.scheduleJob(job, trigger);
 * </pre>
 * 
 * @see RecurrenceRule
 * @see RecurrenceRuleTrigger
 * 
 */
public class RecurrenceRuleScheduleBuilder extends ScheduleBuilder<RecurrenceRuleTrigger> {

    /** The recurrence rule. */
    private Recurrence recurrenceRule;
    /** The policy to handle misfire instructions. */
    private int misfireInstruction = RecurrenceRuleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    /**
     * Constructor.
     * 
     * @param rrule
     *            The recurrence rule.
     */
    protected RecurrenceRuleScheduleBuilder(final Recurrence rrule) {
        if (rrule == null) {
            throw new NullPointerException("recurrence rule cannot be null");
        }
        this.recurrenceRule = rrule;
    }

    /**
     * Build the actual Trigger -- NOT intended to be invoked by end users, but will rather be invoked by a TriggerBuilder which this ScheduleBuilder is given to.
     * 
     * @return The trigger that has been built.
     * 
     * @see TriggerBuilder#withSchedule(ScheduleBuilder)
     */
    @Override
    protected MutableTrigger build () {
        RecurrenceRuleTriggerImpl ct = new RecurrenceRuleTriggerImpl();

        ct.setRecurrenceRule(recurrenceRule);
        ct.setMisfireInstruction(misfireInstruction);
        ct.setStartTime(new Date());

        return ct;
    }

    /**
     * Create a RecurrenceRuleScheduleBuilder with the given recurrence rule expression string - which is presumed to be valid RFC5545 recurrence rule expression (and hence only a RuntimeException will be thrown if it is
     * not).
     * 
     * @param recurrenceRuleExpression
     *            the recurrence rule expression string to base the schedule on.
     * @return the new RecurrenceRuleScheduleBuilder
     * 
     * @see RecurrenceRule
     */
    public static RecurrenceRuleScheduleBuilder recurrenceRuleSchedule (final String recurrenceRuleExpression) {
        return recurrenceRuleSchedule(RecurrenceRuleUtils.parseRecurrenceRuleExpression(recurrenceRuleExpression));
    }

    /**
     * Create a RecurrenceRuleScheduleBuilder with the given recurrence rule expression.
     * 
     * @param rrule
     *            the recurrence rule expression to base the schedule on.
     * @return the new RecurrenceRuleScheduleBuilder
     * @see RecurrenceRule
     */
    public static RecurrenceRuleScheduleBuilder recurrenceRuleSchedule (final Recurrence rrule) {
        return new RecurrenceRuleScheduleBuilder(rrule);
    }

    /**
     * Create a RecurrenceRuleScheduleBuilder with a recurrence rule expression that sets the schedule to fire every day at the given time (hour and minute).
     * 
     * @param hour
     *            the hour of day to fire
     * @param minute
     *            the minute of the given hour to fire
     * @return the new RecurrenceRuleScheduleBuilder
     * @see RecurrenceRule
     */
    public static RecurrenceRuleScheduleBuilder dailyAtHourAndMinute (final int hour, final int minute) {
        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        String recurrenceRuleExpression = String.format("FREQ=DAILY;BYMINUTE=%d;BYHOUR=%d", minute, hour);

        return recurrenceRuleSchedule(recurrenceRuleExpression);
    }

    /**
     * Create a RecurrenceRuleScheduleBuilder with a recurrence rule expression that sets the schedule to fire at the given day at the given time (hour and minute) on the given days of the week.
     * 
     * @param daysOfWeek
     *            the days of the week to fire
     * @param hour
     *            the hour of day to fire
     * @param minute
     *            the minute of the given hour to fire
     * @return the new RecurrenceRuleScheduleBuilder
     * @see RecurrenceRule
     * @see DateBuilder#MONDAY
     * @see DateBuilder#TUESDAY
     * @see DateBuilder#WEDNESDAY
     * @see DateBuilder#THURSDAY
     * @see DateBuilder#FRIDAY
     * @see DateBuilder#SATURDAY
     * @see DateBuilder#SUNDAY
     */
    public static RecurrenceRuleScheduleBuilder atHourAndMinuteOnGivenDaysOfWeek (final int hour, final int minute,
            final DayOfWeek... daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.length == 0) {
            throw new IllegalArgumentException("You must specify at least one day of week.");
        }

        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        return recurrenceRuleSchedule(new Recurrence.Builder(Frequency.DAILY).byDay(daysOfWeek).byHour(hour).byMinute(minute).build());
    }

    /**
     * Create a RecurrenceRuleScheduleBuilder with a recurrence rule expression that sets the schedule to fire one per week on the given day at the given time (hour and minute).
     * 
     * @param dayOfWeek
     *            the day of the week to fire
     * @param hour
     *            the hour of day to fire
     * @param minute
     *            the minute of the given hour to fire
     * @return the new RecurrenceRuleScheduleBuilder
     * @see RecurrenceRule
     * @see DateBuilder#MONDAY
     * @see DateBuilder#TUESDAY
     * @see DateBuilder#WEDNESDAY
     * @see DateBuilder#THURSDAY
     * @see DateBuilder#FRIDAY
     * @see DateBuilder#SATURDAY
     * @see DateBuilder#SUNDAY
     */
    public static RecurrenceRuleScheduleBuilder weeklyOnDayAndHourAndMinute (DayOfWeek dayOfWeek, int hour, int minute) {

        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        return recurrenceRuleSchedule(new Recurrence.Builder(Frequency.WEEKLY).byDay(dayOfWeek).byHour(hour).byMinute(minute).build());

    }

    /**
     * Create a RecurrenceRuleScheduleBuilder with a recurrence rule expression that sets the schedule to fire one per month on the given day of month at the given time (hour and minute).
     * 
     * @param dayOfMonth
     *            the day of the month to fire
     * @param hour
     *            the hour of day to fire
     * @param minute
     *            the minute of the given hour to fire
     * @return the new RecurrenceRuleScheduleBuilder
     * @see RecurrenceRule
     */
    public static RecurrenceRuleScheduleBuilder monthlyOnDayAndHourAndMinute (final int dayOfMonth, final int hour, final int minute) {
        DateBuilder.validateDayOfMonth(dayOfMonth);
        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);

        return recurrenceRuleSchedule(
                new Recurrence.Builder(Frequency.MONTHLY).byMonthDay(dayOfMonth).byHour(hour).byMinute(minute).build());
    }

    /**
     * The <code>TimeZone</code> in which to base the schedule.
     * 
     * @param timezone
     *            the time-zone for the schedule.
     * @return the updated RecurrenceRuleScheduleBuilder
     * @see CronExpression#getTimeZone()
     */
    /*
     * public RecurrenceRuleScheduleBuilder inTimeZone (final TimeZone timezone) { recurrenceRule.setTimeZone(timezone); return this; }
     */

    /**
     * If the Trigger misfires, use the {@link Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
     * 
     * @return the updated RecurrenceRuleScheduleBuilder
     * @see Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
     */
    public RecurrenceRuleScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires () {
        misfireInstruction = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
        return this;
    }

    /**
     * If the Trigger misfires, use the {@link CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
     * 
     * @return the updated RecurrenceRuleScheduleBuilder
     * @see CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
     */
    public RecurrenceRuleScheduleBuilder withMisfireHandlingInstructionDoNothing () {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
        return this;
    }

    /**
     * If the Trigger misfires, use the {@link CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
     * 
     * @return the updated RecurrenceRuleScheduleBuilder
     * @see CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
     */
    public RecurrenceRuleScheduleBuilder withMisfireHandlingInstructionFireAndProceed () {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        return this;
    }

}
