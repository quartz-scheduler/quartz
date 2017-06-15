package org.quartz;

import biweekly.util.Recurrence;

/**
 * The public interface for inspecting settings specific to a RecurrenceRuleTrigger, which is used to fire a <code>{@link org.quartz.Job}</code> at given moments in time, defined with recurrence rules.
 * 
 * <p>
 * The recurrence rule definition is part of the RFC 5545 also known as iCalendar. The iCalendar format is designed to transmit calendar-based data, such as events. The RFC 5545 recurrence rules provide advanced features for
 * creating a firing schedule that are much more powerful and flexible than cron ({@link org.quartz.CronTrigger}). For example, with recurrence rule it is possible to define a trigger that executes every two days.
 * </p>
 * 
 * <p>
 * The format of a "recurrence rule" string is documented on the {@link org.quartz.RecurrenceRule} class.
 * </p>
 * 
 * @see CronScheduleBuilder
 * @see TriggerBuilder
 * 
 */
public interface RecurrenceRuleTrigger extends Trigger {

    /**
     * <p>
     * Used to indicate the 'repeat count' of the trigger is indefinite. Or in other words, the trigger should repeat continually until the trigger's ending timestamp.
     * </p>
     */
    int REPEAT_INDEFINITELY = -1;

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>{@link CronTrigger}</code> wants to be fired now by <code>Scheduler</code>.
     * </p>
     */
    int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = 1;

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>{@link CronTrigger}</code> wants to have it's next-fire-time updated to the next time in the schedule after the current time
     * (taking into account any associated <code>{@link Calendar}</code>, but it does not want to be fired now.
     * </p>
     */
    int MISFIRE_INSTRUCTION_DO_NOTHING = 2;

    /**
     * <p>
     * Returns the recurrence rule as a string compliant with RFC 5545.
     * </p>
     * 
     * @return the recurrence rule.
     */
    String getRecurrenceRuleExpression ();

    /**
     * <p>
     * Get the the number of times for interval this trigger should repeat, after which it will be automatically deleted.
     * </p>
     * 
     * @return the number of times this trigger should repeat
     * 
     * @see #REPEAT_INDEFINITELY
     */
    int getRepeatCount ();

    /**
     * <p>
     * Gets the trigger builder for <code>RecurrenceRuleTrigger</code>.
     * </p>
     * 
     * @return the trigger builder for this interface.
     */
    @Override
    TriggerBuilder<RecurrenceRuleTrigger> getTriggerBuilder ();

    Recurrence getRecurrenceRule ();
}
