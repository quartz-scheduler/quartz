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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.quartz.RRuleScheduleBuilder.dailyAtHourAndMinute;
import static org.quartz.RRuleScheduleBuilder.monthlyOnDayAndHourAndMinute;
import static org.quartz.RRuleScheduleBuilder.rruleSchedule;
import static org.quartz.RRuleScheduleBuilder.weeklyOnDayAndHourAndMinute;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RRuleScheduleBuilder}.
 */
public class RRuleScheduleBuilderTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static Date d(int y, int mo, int day, int h, int mi, int s) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, mo - 1, day, h, mi, s);
        return c.getTime();
    }

    @Test
    void testRRuleScheduleKeepsExpressionAndTimeZone() {
        RRuleTrigger trigger = newTrigger().withIdentity("test")
                .withSchedule(rruleSchedule("FREQ=DAILY;BYHOUR=10;BYMINUTE=0").inTimeZone(UTC))
                .build();
        assertEquals("FREQ=DAILY;BYHOUR=10;BYMINUTE=0", trigger.getRRuleExpression());
        assertEquals(UTC, trigger.getTimeZone());
    }

    @Test
    void testDailyAtHourAndMinute() {
        RRuleTrigger trigger = newTrigger().withIdentity("test")
                .withSchedule(dailyAtHourAndMinute(10, 15))
                .build();
        assertEquals("FREQ=DAILY;BYHOUR=10;BYMINUTE=15;BYSECOND=0", trigger.getRRuleExpression());
    }

    @Test
    void testWeeklyOnDayAndHourAndMinute() {
        RRuleTrigger trigger = newTrigger().withIdentity("test")
                .withSchedule(weeklyOnDayAndHourAndMinute(DateBuilder.WEDNESDAY, 10, 15))
                .build();
        assertEquals("FREQ=WEEKLY;BYDAY=WE;BYHOUR=10;BYMINUTE=15;BYSECOND=0", trigger.getRRuleExpression());
    }

    @Test
    void testMonthlyOnDayAndHourAndMinute() {
        RRuleTrigger trigger = newTrigger().withIdentity("test")
                .withSchedule(monthlyOnDayAndHourAndMinute(15, 10, 15))
                .build();
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=15;BYHOUR=10;BYMINUTE=15;BYSECOND=0", trigger.getRRuleExpression());
    }

    @Test
    void testBuiltTriggerComputesCorrectFirstFire() {
        RRuleTrigger trigger = newTrigger().withIdentity("test")
                .withSchedule(rruleSchedule("FREQ=DAILY;BYHOUR=10;BYMINUTE=0;BYSECOND=0").inTimeZone(UTC))
                .startAt(d(2026, 1, 5, 9, 0, 0))
                .build();
        Date first = trigger.getFireTimeAfter(new Date(trigger.getStartTime().getTime() - 1000L));
        assertEquals(d(2026, 1, 5, 10, 0, 0), first);
        assertEquals(d(2026, 1, 6, 10, 0, 0), trigger.getFireTimeAfter(first));
    }
}
