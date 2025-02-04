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

import static org.junit.jupiter.api.Assertions.*;
import static org.quartz.DateBuilder.dateOf;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TimeOfDay;
import org.quartz.TriggerUtils;
import org.quartz.impl.calendar.CronCalendar;

/**
 * Unit test for {@link DailyTimeIntervalTriggerImpl}.
 * 
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
class DailyTimeIntervalTriggerImplTest  {

  @Test
  void testNormalExample() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(11, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(72); // this interval will give three firings per day (8:00, 9:12, and 10:24)
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(10, 24, 0, 16, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testQuartzCalendarExclusion() throws Exception {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(new TimeOfDay(8, 0));
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    CronCalendar cronCal = new CronCalendar("* * 9-12 * * ?"); // exclude 9-12    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, cronCal, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(13, 0, 0, 1, 1, 2011), fireTimes.get(1));
    assertEquals(dateOf(23, 0, 0, 4, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testValidateTimeOfDayOrder() {
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTimeOfDay(new TimeOfDay(12, 0, 0));
    trigger.setEndTimeOfDay(new TimeOfDay(8, 0, 0));
    try {
      trigger.validate();
      fail("Trigger should be invalidate when time of day is not in order.");
    } catch (SchedulerException e) {
      // expected.
    }
  }

  @Test
  void testValidateInterval() throws Exception {
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setName("test");
    trigger.setGroup("test");
    trigger.setJobKey(JobKey.jobKey("test"));
    
    trigger.setRepeatIntervalUnit(IntervalUnit.HOUR);
    trigger.setRepeatInterval(25);
    try {
      trigger.validate();
      fail("Trigger should be invalidate when interval is greater than 24 hours.");
    } catch (SchedulerException e) {
      // expected.
    }
    
    trigger.setRepeatIntervalUnit(IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60 * 25);
    try {
      trigger.validate();
      fail("Trigger should be invalidate when interval is greater than 24 hours.");
    } catch (SchedulerException e) {
      // expected.
    }

    trigger.setRepeatIntervalUnit(IntervalUnit.SECOND);
    trigger.setRepeatInterval(60 * 60 * 25);
    try {
      trigger.validate();
      fail("Trigger should be invalidate when interval is greater than 24 hours.");
    } catch (SchedulerException e) {
      // expected.
    }
    
    try {
      trigger.setRepeatIntervalUnit(IntervalUnit.DAY);
      trigger.validate();
      fail("Trigger should be invalidate when interval unit > HOUR.");
    } catch (Exception e) {
      // expected.
    }

    try {
      trigger.setRepeatIntervalUnit(IntervalUnit.SECOND);
      trigger.setRepeatInterval(0);
      trigger.validate();
      fail("Trigger should be invalidate when interval is zero.");
    } catch (Exception e) {
      // expected.
    }
  }

  @Test
  void testStartTimeWithoutStartTimeOfDay() throws Exception {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(0, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(23, 0, 0, 2, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testEndTimeWithoutEndTimeOfDay() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = dateOf(22, 0, 0, 2, 1, 2011);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setEndTime(endTime);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(47, fireTimes.size());
    assertEquals(dateOf(0, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(22, 0, 0, 2, 1, 2011), fireTimes.get(46));
  }

  @Test
  void testStartTimeBeforeStartTimeOfDay() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(23, 0, 0, 3, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testStartTimeBeforeStartTimeOfDayOnInvalidDay() throws Exception {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011); // Jan 1, 2011 was a saturday...
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    Set<Integer> daysOfWeek = new HashSet<Integer>();
    daysOfWeek.add(DateBuilder.MONDAY);
    daysOfWeek.add(DateBuilder.TUESDAY);
    daysOfWeek.add(DateBuilder.WEDNESDAY);
    daysOfWeek.add(DateBuilder.THURSDAY);
    daysOfWeek.add(DateBuilder.FRIDAY);
    trigger.setDaysOfWeek(daysOfWeek);
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    assertEquals(dateOf(8, 0, 0, 3, 1, 2011), trigger.getFireTimeAfter(dateOf(6, 0, 0, 22, 5, 2010)));

    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 3, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(23, 0, 0, 5, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testStartTimeAfterStartTimeOfDay() {
    Date startTime = dateOf(9, 23, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(10, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(9, 0, 0, 4, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testEndTimeBeforeEndTimeOfDay() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = dateOf(16, 0, 0, 2, 1, 2011);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setEndTime(endTime);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(35, fireTimes.size());
    assertEquals(dateOf(0, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(17, 0, 0, 1, 1, 2011), fireTimes.get(17));
    assertEquals(dateOf(16, 0, 0, 2, 1, 2011), fireTimes.get(34));
  }

  @Test
  void testEndTimeAfterEndTimeOfDay() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = dateOf(18, 0, 0, 2, 1, 2011);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setEndTime(endTime);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(36, fireTimes.size());
    assertEquals(dateOf(0, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(17, 0, 0, 1, 1, 2011), fireTimes.get(17));
    assertEquals(dateOf(17, 0, 0, 2, 1, 2011), fireTimes.get(35));
  }

  @Test
  void testTimeOfDayWithStartTime() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(17, 0, 0, 1, 1, 2011), fireTimes.get(9)); // The 10th hours is the end of day.
    assertEquals(dateOf(15, 0, 0, 5, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testTimeOfDayWithEndTime() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = dateOf(0, 0, 0, 4, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setEndTime(endTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(30, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(17, 0, 0, 1, 1, 2011), fireTimes.get(9)); // The 10th hours is the end of day.
    assertEquals(dateOf(17, 0, 0, 3, 1, 2011), fireTimes.get(29));
  }

  @Test
  void testTimeOfDayWithEndTime2() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 23, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(23, 59, 59); // edge case when endTime is last second of day, which is default too.
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());    
    assertEquals(dateOf(8, 23, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(23, 23, 0, 3, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testAllDaysOfTheWeek() {
    Set<Integer> daysOfWeek = DailyTimeIntervalScheduleBuilder.ALL_DAYS_OF_THE_WEEK;
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011); // SAT
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setDaysOfWeek(daysOfWeek);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(17, 0, 0, 1, 1, 2011), fireTimes.get(9)); // The 10th hours is the end of day.
    assertEquals(dateOf(15, 0, 0, 5, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testMonThroughFri() {
    Set<Integer> daysOfWeek = DailyTimeIntervalScheduleBuilder.MONDAY_THROUGH_FRIDAY;
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011); // SAT(7)
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setDaysOfWeek(daysOfWeek);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 3, 1, 2011), fireTimes.get(0));
    assertEquals(Calendar.MONDAY, getDayOfWeek(fireTimes.get(0)));
    assertEquals(dateOf(8, 0, 0, 4, 1, 2011), fireTimes.get(10));
    assertEquals(Calendar.TUESDAY, getDayOfWeek(fireTimes.get(10)));
    assertEquals(dateOf(15, 0, 0, 7, 1, 2011), fireTimes.get(47));
    assertEquals(Calendar.FRIDAY, getDayOfWeek(fireTimes.get(47)));
  }

  @Test
  void testSatAndSun() {
    Set<Integer> daysOfWeek = DailyTimeIntervalScheduleBuilder.SATURDAY_AND_SUNDAY;
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011); // SAT(7)
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setDaysOfWeek(daysOfWeek);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(Calendar.SATURDAY, getDayOfWeek(fireTimes.get(0)));
    assertEquals(dateOf(8, 0, 0, 2, 1, 2011), fireTimes.get(10));
    assertEquals(Calendar.SUNDAY, getDayOfWeek(fireTimes.get(10)));
    assertEquals(dateOf(15, 0, 0, 15, 1, 2011), fireTimes.get(47));
    assertEquals(Calendar.SATURDAY, getDayOfWeek(fireTimes.get(47)));
  }

  @Test
  void testMonOnly() {
    Set<Integer> daysOfWeek = new HashSet<Integer>();
    daysOfWeek.add(Calendar.MONDAY);
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011); // SAT(7)
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(17, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setDaysOfWeek(daysOfWeek);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(60);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 3, 1, 2011), fireTimes.get(0));
    assertEquals(Calendar.MONDAY, getDayOfWeek(fireTimes.get(0)));
    assertEquals(dateOf(8, 0, 0, 10, 1, 2011), fireTimes.get(10));
    assertEquals(Calendar.MONDAY, getDayOfWeek(fireTimes.get(10)));
    assertEquals(dateOf(15, 0, 0, 31, 1, 2011), fireTimes.get(47));
    assertEquals(Calendar.MONDAY, getDayOfWeek(fireTimes.get(47)));
  }


  private int getDayOfWeek(Date dateTime) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(dateTime);
    return cal.get(Calendar.DAY_OF_WEEK);
  }

  @Test
  void testTimeOfDayWithEndTimeOddInterval() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = dateOf(0, 0, 0, 4, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(10, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setEndTime(endTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(23);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(18, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(9, 55, 0, 1, 1, 2011), fireTimes.get(5));
    assertEquals(dateOf(9, 55, 0, 3, 1, 2011), fireTimes.get(17));
  }

  @Test
  void testHourInterval() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = dateOf(13, 0, 0, 15, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 1, 15);
    TimeOfDay endTimeOfDay = new TimeOfDay(16, 1, 15);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTime(endTime);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.HOUR);
    trigger.setRepeatInterval(2);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 1, 15, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(12, 1, 15, 10, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testSecondInterval() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 2);
    TimeOfDay endTimeOfDay = new TimeOfDay(13, 30, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.SECOND);
    trigger.setRepeatInterval(72);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 2, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(8, 56, 26, 1, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testRepeatCountInf() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(11, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(72);
    
    // Setting this (which is default) should make the trigger just as normal one.
    trigger.setRepeatCount(DailyTimeIntervalTrigger.REPEAT_INDEFINITELY);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);
    assertEquals(48, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(10, 24, 0, 16, 1, 2011), fireTimes.get(47));
  }

  @Test
  void testRepeatCount() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(11, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(72);
    trigger.setRepeatCount(7);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);    
    assertEquals(8, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    assertEquals(dateOf(9, 12, 0, 3, 1, 2011), fireTimes.get(7));
  }

  @Test
  void testRepeatCount0() {
    Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
    TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
    TimeOfDay endTimeOfDay = new TimeOfDay(11, 0, 0);
    DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
    trigger.setStartTime(startTime);
    trigger.setStartTimeOfDay(startTimeOfDay);
    trigger.setEndTimeOfDay(endTimeOfDay);
    trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
    trigger.setRepeatInterval(72);
    trigger.setRepeatCount(0);
    
    List<Date> fireTimes = TriggerUtils.computeFireTimes(trigger, null, 48);    
    assertEquals(1, fireTimes.size());
    assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
  }

  @Test
  void testGetFireTime() {
        Date startTime = dateOf(0, 0, 0, 1, 1, 2011);
        TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
        TimeOfDay endTimeOfDay = new TimeOfDay(13, 0, 0);
        DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
        trigger.setStartTime(startTime);
        trigger.setStartTimeOfDay(startTimeOfDay);
        trigger.setEndTimeOfDay(endTimeOfDay);
        trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.HOUR);
        trigger.setRepeatInterval(1);

        assertEquals(dateOf(8, 0, 0, 1, 1, 2011), trigger.getFireTimeAfter(dateOf(0, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2011), trigger.getFireTimeAfter(dateOf(7, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2011), trigger.getFireTimeAfter(dateOf(7, 59, 59, 1, 1, 2011)));
        assertEquals(dateOf(9, 0, 0, 1, 1, 2011), trigger.getFireTimeAfter(dateOf(8, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(10, 0, 0, 1, 1, 2011), trigger.getFireTimeAfter(dateOf(9, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(13, 0, 0, 1, 1, 2011), trigger.getFireTimeAfter(dateOf(12, 59, 59, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 2, 1, 2011), trigger.getFireTimeAfter(dateOf(13, 0, 0, 1, 1, 2011)));
    }

  @Test
  void testGetFireTimeWithDateBeforeStartTime() {
        Date startTime = dateOf(0, 0, 0, 1, 1, 2012);
        TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
        TimeOfDay endTimeOfDay = new TimeOfDay(13, 0, 0);
        DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
        trigger.setStartTime(startTime);
        trigger.setStartTimeOfDay(startTimeOfDay);
        trigger.setEndTimeOfDay(endTimeOfDay);
        trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.HOUR);
        trigger.setRepeatInterval(1);

        // NOTE that if you pass a date past the startTime, you will get the first firing on or after the startTime back!
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(0, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(7, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(7, 59, 59, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(8, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(9, 0, 0, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(12, 59, 59, 1, 1, 2011)));
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(13, 0, 0, 1, 1, 2011)));

        // Now try some test times at or after startTime
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(0, 0, 0, 1, 1, 2012)));
        assertEquals(dateOf(8, 0, 0, 2, 1, 2012), trigger.getFireTimeAfter(dateOf(13, 0, 0, 1, 1, 2012)));
    }

  @Test
  void testGetFireTimeWhenStartTimeAndTimeOfDayIsSame() {
        // A test case for QTZ-369
        Date startTime = dateOf(8, 0, 0, 1, 1, 2012);
        TimeOfDay startTimeOfDay = new TimeOfDay(8, 0, 0);
        TimeOfDay endTimeOfDay = new TimeOfDay(13, 0, 0);
        DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
        trigger.setStartTime(startTime);
        trigger.setStartTimeOfDay(startTimeOfDay);
        trigger.setEndTimeOfDay(endTimeOfDay);
        trigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.HOUR);
        trigger.setRepeatInterval(1);

        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getFireTimeAfter(dateOf(0, 0, 0, 1, 1, 2012)));
    }

  @Test
  void testExtraConstructors() {
        // A test case for QTZ-389 - some extra constructors didn't set all parameters
        DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl(
                "triggerName", "triggerGroup", "jobName", "jobGroup",
                dateOf(8, 0, 0, 1, 1, 2012), null,
                new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0),
                IntervalUnit.HOUR, 1);

        assertEquals("triggerName", trigger.getName());
        assertEquals("triggerGroup", trigger.getGroup());
        assertEquals("jobName", trigger.getJobName());
        assertEquals("jobGroup", trigger.getJobGroup());
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getStartTime());
        assertNull(trigger.getEndTime());
        assertEquals(new TimeOfDay(8, 0, 0), trigger.getStartTimeOfDay());
        assertEquals(new TimeOfDay(17, 0, 0), trigger.getEndTimeOfDay());
        assertEquals(IntervalUnit.HOUR, trigger.getRepeatIntervalUnit());
        assertEquals(1, trigger.getRepeatInterval());

        trigger = new DailyTimeIntervalTriggerImpl(
                "triggerName", "triggerGroup",
                dateOf(8, 0, 0, 1, 1, 2012), null,
                new TimeOfDay(8, 0, 0), new TimeOfDay(17, 0, 0),
                IntervalUnit.HOUR, 1);

        assertEquals("triggerName", trigger.getName());
        assertEquals("triggerGroup", trigger.getGroup());
        assertNull(trigger.getJobName());
        assertEquals("DEFAULT", trigger.getJobGroup());
        assertEquals(dateOf(8, 0, 0, 1, 1, 2012), trigger.getStartTime());
        assertNull(trigger.getEndTime());
        assertEquals(new TimeOfDay(8, 0, 0), trigger.getStartTimeOfDay());
        assertEquals(new TimeOfDay(17, 0, 0), trigger.getEndTimeOfDay());
        assertEquals(IntervalUnit.HOUR, trigger.getRepeatIntervalUnit());
        assertEquals(1, trigger.getRepeatInterval());
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, 0})
    void testSetRepeatIntervalWithInvalidValues(int repeatInterval) {
        DailyTimeIntervalTriggerImpl trigger = new DailyTimeIntervalTriggerImpl();
        assertThrows(IllegalArgumentException.class, () -> trigger.setRepeatInterval(repeatInterval));
    }
}
