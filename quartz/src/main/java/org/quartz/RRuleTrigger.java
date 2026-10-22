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

import java.util.TimeZone;

/**
 * The public interface for inspecting settings specific to an
 * <code>RRuleTrigger</code>, which is used to fire a
 * <code>{@link org.quartz.Job}</code> at moments in time defined by an
 * <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10">RFC&nbsp;5545
 * <code>RRULE</code></a> recurrence rule.
 *
 * <p>
 * An <code>RRuleTrigger</code> is the RFC&nbsp;5545 analogue of a
 * {@link CronTrigger}. Where a cron expression is self-contained, an
 * <code>RRULE</code> is evaluated relative to the trigger's
 * {@link #getStartTime() start-time}, which plays the role of the RFC&nbsp;5545
 * <code>DTSTART</code>. As with cron, the start-time acts as a lower bound: the
 * start instant itself only fires if it matches the rule.
 * </p>
 *
 * <p>
 * The format of an <code>RRULE</code> string is documented on the
 * {@link org.quartz.RRuleExpression} class. Some examples:
 * </p>
 * <table>
 * <caption>Examples of RRULE expressions and their meanings.</caption>
 * <tr><th>Expression</th><th>&nbsp;</th><th>Meaning</th></tr>
 * <tr><td><code>"FREQ=DAILY;BYHOUR=10;BYMINUTE=15"</code></td><td>&nbsp;</td>
 * <td><code>Fire at 10:15am every day</code></td></tr>
 * <tr><td><code>"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=10;BYMINUTE=15"</code></td><td>&nbsp;</td>
 * <td><code>Fire at 10:15am every weekday</code></td></tr>
 * <tr><td><code>"FREQ=MONTHLY;BYDAY=-1FR;BYHOUR=10;BYMINUTE=15"</code></td><td>&nbsp;</td>
 * <td><code>Fire at 10:15am on the last Friday of every month</code></td></tr>
 * <tr><td><code>"FREQ=YEARLY;BYMONTH=1;BYMONTHDAY=1;COUNT=10"</code></td><td>&nbsp;</td>
 * <td><code>Fire on January 1st for the next ten years</code></td></tr>
 * </table>
 *
 * @see RRuleExpression
 * @see RRuleScheduleBuilder
 * @see TriggerBuilder
 */
public interface RRuleTrigger extends Trigger {

    long serialVersionUID = 8028016963996402142L;

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link RRuleTrigger}</code> wants to be fired now
     * by <code>Scheduler</code>.
     * </p>
     */
    int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = 1;

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link RRuleTrigger}</code> wants to have its
     * next-fire-time updated to the next time in the schedule after the
     * current time (taking into account any associated
     * <code>{@link Calendar}</code>), but it does not want to be fired now.
     * </p>
     */
    int MISFIRE_INSTRUCTION_DO_NOTHING = 2;

    /**
     * Returns the RFC&nbsp;5545 <code>RRULE</code> string of this trigger.
     */
    String getRRuleExpression();

    /**
     * <p>
     * Returns the time zone for which the <code>rruleExpression</code> of
     * this <code>RRuleTrigger</code> will be resolved.
     * </p>
     */
    TimeZone getTimeZone();

    String getExpressionSummary();

    TriggerBuilder<RRuleTrigger> getTriggerBuilder();
}
