package org.quartz;

import static org.quartz.TriggerBuilder.newTrigger;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.Trigger;

import biweekly.util.DayOfWeek;
import biweekly.util.Frequency;

public class RecurrenceRuleScheduleBuilderTest {

    @Test
    public void testDailyAtHourAndMinute () {
        RecurrenceRuleTrigger trigger = newTrigger().startNow().withSchedule(RecurrenceRuleScheduleBuilder.dailyAtHourAndMinute(15, 55))
                .build();
        Assert.assertNotNull(trigger.getRecurrenceRule());
        Assert.assertEquals(0, Frequency.DAILY.compareTo(trigger.getRecurrenceRule().getFrequency()));
        Assert.assertTrue(trigger.getRecurrenceRule().getByHour().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByHour().get(0).intValue(), 15);
        Assert.assertTrue(trigger.getRecurrenceRule().getByMinute().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByMinute().get(0).intValue(), 55);
    }

    @Test
    public void testAtHourAndMinuteOnGivenDaysOfWeek () {
        RecurrenceRuleTrigger trigger = newTrigger().startNow()
                .withSchedule(RecurrenceRuleScheduleBuilder.atHourAndMinuteOnGivenDaysOfWeek(15, 55, DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .build();
        Assert.assertNotNull(trigger.getRecurrenceRule());
        Assert.assertEquals(0, Frequency.DAILY.compareTo(trigger.getRecurrenceRule().getFrequency()));
        Assert.assertTrue(trigger.getRecurrenceRule().getByHour().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByHour().get(0).intValue(), 15);
        Assert.assertTrue(trigger.getRecurrenceRule().getByMinute().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByMinute().get(0).intValue(), 55);
        Assert.assertTrue(trigger.getRecurrenceRule().getByDay().size() == 2);
        Assert.assertEquals(trigger.getRecurrenceRule().getByDay().get(0).getDay(), DayOfWeek.MONDAY);
        Assert.assertEquals(trigger.getRecurrenceRule().getByDay().get(1).getDay(), DayOfWeek.FRIDAY);
    }

    @Test
    public void testWeeklyOnDayAndHourAndMinute () {
        RecurrenceRuleTrigger trigger = newTrigger().startNow()
                .withSchedule(RecurrenceRuleScheduleBuilder.weeklyOnDayAndHourAndMinute(DayOfWeek.MONDAY, 3, 33)).build();
        Assert.assertNotNull(trigger.getRecurrenceRule());
        Assert.assertEquals(0, Frequency.WEEKLY.compareTo(trigger.getRecurrenceRule().getFrequency()));
        Assert.assertTrue(trigger.getRecurrenceRule().getByHour().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByHour().get(0).intValue(), 3);
        Assert.assertTrue(trigger.getRecurrenceRule().getByMinute().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByMinute().get(0).intValue(), 33);
        Assert.assertTrue(trigger.getRecurrenceRule().getByDay().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByDay().get(0).getDay(), DayOfWeek.MONDAY);
    }

    @Test
    public void testMonthlyOnDayAndHourAndMinute () {
        RecurrenceRuleTrigger trigger = newTrigger().startNow()
                .withSchedule(RecurrenceRuleScheduleBuilder.monthlyOnDayAndHourAndMinute(15, 3, 33)).build();
        Assert.assertNotNull(trigger.getRecurrenceRule());
        Assert.assertEquals(0, Frequency.MONTHLY.compareTo(trigger.getRecurrenceRule().getFrequency()));
        Assert.assertTrue(trigger.getRecurrenceRule().getByHour().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByHour().get(0).intValue(), 3);
        Assert.assertTrue(trigger.getRecurrenceRule().getByMinute().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByMinute().get(0).intValue(), 33);
        Assert.assertTrue(trigger.getRecurrenceRule().getByMonthDay().size() == 1);
        Assert.assertEquals(trigger.getRecurrenceRule().getByMonthDay().get(0).intValue(), 15);
    }

    @Test
    public void testWithMisfireHandlingInstructionIgnoreMisfires () {
        RecurrenceRuleTrigger trigger = newTrigger().withSchedule(
                RecurrenceRuleScheduleBuilder.monthlyOnDayAndHourAndMinute(15, 3, 33).withMisfireHandlingInstructionIgnoreMisfires())
                .build();
        Assert.assertEquals(trigger.getMisfireInstruction(), Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
    }

    @Test
    public void testWithMisfireHandlingInstructionDoNothing () {
        RecurrenceRuleTrigger trigger = newTrigger()
                .withSchedule(
                        RecurrenceRuleScheduleBuilder.monthlyOnDayAndHourAndMinute(15, 3, 33).withMisfireHandlingInstructionDoNothing())
                .build();
        Assert.assertEquals(trigger.getMisfireInstruction(), CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    }

    @Test
    public void testWithMisfireHandlingInstructionFireAndProceed () {
        RecurrenceRuleTrigger trigger = newTrigger().withSchedule(
                RecurrenceRuleScheduleBuilder.monthlyOnDayAndHourAndMinute(15, 3, 33).withMisfireHandlingInstructionFireAndProceed())
                .build();
        Assert.assertEquals(trigger.getMisfireInstruction(), CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }
}
