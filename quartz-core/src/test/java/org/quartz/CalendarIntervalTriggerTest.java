/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
 */
package org.quartz;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;

/**
 * Unit tests for DateIntervalTrigger.
 */
public class CalendarIntervalTriggerTest  extends SerializationTestSupport {
    
    private static final String[] VERSIONS = new String[] {"2.0"};

    @Test
    public void testQTZ331FireTimeAfterBoundary() {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(2013, Calendar.FEBRUARY, 15);

        Date startTime = start.getTime();
        start.add(Calendar.DAY_OF_MONTH, 1);
        Date triggerTime = start.getTime();

        CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl("test", startTime, null, IntervalUnit.DAY, 1);
        assertThat(trigger.getFireTimeAfter(startTime), equalTo(triggerTime));


        Date after = new Date(start.getTimeInMillis() - 500);
        assertThat(trigger.getFireTimeAfter(after), equalTo(triggerTime));
    }

    public void testQTZ330DaylightSavingsCornerCase() {
        TimeZone edt = TimeZone.getTimeZone("America/New_York");

        Calendar start = Calendar.getInstance();
        start.clear();
        start.setTimeZone(edt);
        start.set(2012, Calendar.MARCH, 16, 2, 30, 0);

        Calendar after = Calendar.getInstance();
        after.clear();
        after.setTimeZone(edt);
        after.set(2013, Calendar.APRIL, 19, 2, 30, 0);

        BaseCalendar baseCalendar = new BaseCalendar(edt);

        CalendarIntervalTriggerImpl intervalTrigger = new CalendarIntervalTriggerImpl("QTZ-330", start.getTime(), null, DateBuilder.IntervalUnit.DAY, 1);
        intervalTrigger.setTimeZone(edt);
        intervalTrigger.setPreserveHourOfDayAcrossDaylightSavings(true);
        intervalTrigger.computeFirstFireTime(baseCalendar);

        Date fireTime = intervalTrigger.getFireTimeAfter(after.getTime());
        assertThat(fireTime.after(after.getTime()), is(true));
    }

    public void testYearlyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl yearlyTrigger = new CalendarIntervalTriggerImpl();
        yearlyTrigger.setStartTime(startCalendar.getTime());
        yearlyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.YEAR);
        yearlyTrigger.setRepeatInterval(2); // every two years;
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2009, Calendar.JUNE, 1, 9, 30, 17); // jump 4 years (2 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(yearlyTrigger, null, 4);
        Date secondTime = fireTimes.get(2); // get the third fire time
        
        assertEquals("Year increment result not as expected.", targetCalendar.getTime(), secondTime);
    }

    
    public void testMonthlyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl yearlyTrigger = new CalendarIntervalTriggerImpl();
        yearlyTrigger.setStartTime(startCalendar.getTime());
        yearlyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MONTH);
        yearlyTrigger.setRepeatInterval(5); // every five months
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.MONTH, 25); // jump 25 five months (5 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(yearlyTrigger, null, 6);
        Date fifthTime = fireTimes.get(5); // get the sixth fire time

        assertEquals("Month increment result not as expected.", targetCalendar.getTime(), fifthTime);
    }

    public void testWeeklyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl yearlyTrigger = new CalendarIntervalTriggerImpl();
        yearlyTrigger.setStartTime(startCalendar.getTime());
        yearlyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.WEEK);
        yearlyTrigger.setRepeatInterval(6); // every six weeks
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 7 * 6 * 4); // jump 24 weeks (4 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(yearlyTrigger, null, 7);
        Date fifthTime = fireTimes.get(4); // get the fifth fire time

        assertEquals("Week increment result not as expected.", targetCalendar.getTime(), fifthTime);
    }
    
    public void testDailyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(90); // every ninety days
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 360); // jump 360 days (4 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);
        Date fifthTime = fireTimes.get(4); // get the fifth fire time

        assertEquals("Day increment result not as expected.", targetCalendar.getTime(), fifthTime);
    }
    
    public void testHourlyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl yearlyTrigger = new CalendarIntervalTriggerImpl();
        yearlyTrigger.setStartTime(startCalendar.getTime());
        yearlyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.HOUR);
        yearlyTrigger.setRepeatInterval(100); // every 100 hours
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.HOUR, 400); // jump 400 hours (4 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(yearlyTrigger, null, 6);
        Date fifthTime = fireTimes.get(4); // get the fifth fire time

        assertEquals("Hour increment result not as expected.", targetCalendar.getTime(), fifthTime);
    }

    public void testMinutelyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl yearlyTrigger = new CalendarIntervalTriggerImpl();
        yearlyTrigger.setStartTime(startCalendar.getTime());
        yearlyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
        yearlyTrigger.setRepeatInterval(100); // every 100 minutes
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.MINUTE, 400); // jump 400 minutes (4 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(yearlyTrigger, null, 6);
        Date fifthTime = fireTimes.get(4); // get the fifth fire time

        assertEquals("Minutes increment result not as expected.", targetCalendar.getTime(), fifthTime);
    }

    public void testSecondlyIntervalGetFireTimeAfter() {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl yearlyTrigger = new CalendarIntervalTriggerImpl();
        yearlyTrigger.setStartTime(startCalendar.getTime());
        yearlyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.SECOND);
        yearlyTrigger.setRepeatInterval(100); // every 100 seconds
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(2005, Calendar.JUNE, 1, 9, 30, 17);
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.SECOND, 400); // jump 400 seconds (4 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(yearlyTrigger, null, 6);
        Date fifthTime = fireTimes.get(4); // get the third fire time

        assertEquals("Seconds increment result not as expected.", targetCalendar.getTime(), fifthTime);
    }

    public void testDaylightSavingsTransitions() {

        // Pick a day before a spring daylight savings transition...
        
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2010, Calendar.MARCH, 12, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(5); // every 5 days
        
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.setTime(startCalendar.getTime());
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 10); // jump 10 days (2 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        List<Date> fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);
        Date testTime = fireTimes.get(2); // get the third fire time

        assertEquals("Day increment result not as expected over spring 2010 daylight savings transition.", targetCalendar.getTime(), testTime);

        // And again, Pick a day before a spring daylight savings transition... (QTZ-240)
        
        startCalendar = Calendar.getInstance();
        startCalendar.set(2011, Calendar.MARCH, 12, 1, 0, 0);
        startCalendar.clear(Calendar.MILLISECOND);

        dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(1); // every day
        
        targetCalendar = Calendar.getInstance();
        targetCalendar.setTime(startCalendar.getTime());
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 2); // jump 2 days (2 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);
        testTime = fireTimes.get(2); // get the third fire time

        assertEquals("Day increment result not as expected over spring 2011 daylight savings transition.", targetCalendar.getTime(), testTime);
        
        // And again, Pick a day before a spring daylight savings transition... (QTZ-240) - and prove time of day is not preserved without setPreserveHourOfDayAcrossDaylightSavings(true)
        
        startCalendar = Calendar.getInstance();
        startCalendar.setTimeZone(TimeZone.getTimeZone("CET"));
        startCalendar.set(2011, Calendar.MARCH, 26, 4, 0, 0);
        startCalendar.clear(Calendar.MILLISECOND);

        dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(1); // every day
        dailyTrigger.setTimeZone(TimeZone.getTimeZone("EST"));
        
        targetCalendar = Calendar.getInstance();
        targetCalendar.setTimeZone(TimeZone.getTimeZone("CET"));
        targetCalendar.setTime(startCalendar.getTime());
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 2); // jump 2 days (2 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);

		testTime = fireTimes.get(2); // get the third fire time

        Calendar testCal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        testCal.setTimeInMillis(testTime.getTime());
        
        assertFalse("Day increment time-of-day result not as expected over spring 2011 daylight savings transition.", targetCalendar.get(Calendar.HOUR_OF_DAY) == testCal.get(Calendar.HOUR_OF_DAY));
        
        // And again, Pick a day before a spring daylight savings transition... (QTZ-240) - and prove time of day is preserved with setPreserveHourOfDayAcrossDaylightSavings(true)
        
        startCalendar = Calendar.getInstance();
        startCalendar.setTimeZone(TimeZone.getTimeZone("CET"));
        startCalendar.set(2011, Calendar.MARCH, 26, 4, 0, 0);
        startCalendar.clear(Calendar.MILLISECOND);

        dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(1); // every day
        dailyTrigger.setTimeZone(TimeZone.getTimeZone("CET"));
        dailyTrigger.setPreserveHourOfDayAcrossDaylightSavings(true);
        
        targetCalendar = Calendar.getInstance();
        targetCalendar.setTimeZone(TimeZone.getTimeZone("CET"));
        targetCalendar.setTime(startCalendar.getTime());
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 2); // jump 2 days (2 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);

		testTime = fireTimes.get(2); // get the third fire time

        testCal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        testCal.setTimeInMillis(testTime.getTime());
        
        assertTrue("Day increment time-of-day result not as expected over spring 2011 daylight savings transition.", targetCalendar.get(Calendar.HOUR_OF_DAY) == testCal.get(Calendar.HOUR_OF_DAY));
        
        // Pick a day before a fall daylight savings transition...
        
        startCalendar = Calendar.getInstance();
        startCalendar.set(2010, Calendar.OCTOBER, 31, 9, 30, 17);
        startCalendar.clear(Calendar.MILLISECOND);

        dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(5); // every 5 days
        
        targetCalendar = Calendar.getInstance();
        targetCalendar.setTime(startCalendar.getTime());
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 15); // jump 15 days (3 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);
        testTime = (Date) fireTimes.get(3); // get the fourth fire time

        assertEquals("Day increment result not as expected over fall 2010 daylight savings transition.", targetCalendar.getTime(), testTime);
        
        // And again, Pick a day before a fall daylight savings transition...  (QTZ-240)
        
        startCalendar = Calendar.getInstance();
        startCalendar.setTimeZone(TimeZone.getTimeZone("CEST"));
        startCalendar.set(2011, Calendar.OCTOBER, 29, 1, 30, 00);
        startCalendar.clear(Calendar.MILLISECOND);

        dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(1); // every day
        dailyTrigger.setTimeZone(TimeZone.getTimeZone("EST"));

        targetCalendar = Calendar.getInstance();
        targetCalendar.setTimeZone(TimeZone.getTimeZone("CEST"));
        targetCalendar.setTime(startCalendar.getTime());
        targetCalendar.setLenient(true);
        targetCalendar.add(Calendar.DAY_OF_YEAR, 3); // jump 3 days (3 intervals)
        targetCalendar.clear(Calendar.MILLISECOND);

        fireTimes = TriggerUtils.computeFireTimes(dailyTrigger, null, 6);
        testTime = (Date) fireTimes.get(3); // get the fourth fire time

        assertEquals("Day increment result not as expected over fall 2011 daylight savings transition.", targetCalendar.getTime(), testTime);        
    }
 
    
    public void testFinalFireTimes() {

        
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2010, Calendar.MARCH, 12, 9, 0, 0);
        startCalendar.clear(Calendar.MILLISECOND);

        CalendarIntervalTriggerImpl dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.DAY);
        dailyTrigger.setRepeatInterval(5); // every 5 days
        
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(startCalendar.getTime());
        endCalendar.setLenient(true);
        endCalendar.add(Calendar.DAY_OF_YEAR, 10); // jump 10 days (2 intervals)
        endCalendar.clear(Calendar.MILLISECOND);
        dailyTrigger.setEndTime(endCalendar.getTime());

        Date testTime = dailyTrigger.getFinalFireTime();

        assertEquals("Final fire time not computed correctly for day interval.", endCalendar.getTime(), testTime);

        
        startCalendar = Calendar.getInstance();
        startCalendar.set(2010, Calendar.MARCH, 12, 9, 0, 0);
        startCalendar.clear(Calendar.MILLISECOND);

        dailyTrigger = new CalendarIntervalTriggerImpl();
        dailyTrigger.setStartTime(startCalendar.getTime());
        dailyTrigger.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MINUTE);
        dailyTrigger.setRepeatInterval(5); // every 5 minutes
        
        endCalendar = Calendar.getInstance();
        endCalendar.setTime(startCalendar.getTime());
        endCalendar.setLenient(true);
        endCalendar.add(Calendar.DAY_OF_YEAR, 15); // jump 15 days 
        endCalendar.add(Calendar.MINUTE,-2); // back up two minutes
        endCalendar.clear(Calendar.MILLISECOND);
        dailyTrigger.setEndTime(endCalendar.getTime());

        testTime = dailyTrigger.getFinalFireTime();

        assertTrue("Final fire time not computed correctly for minutely interval.", (endCalendar.getTime().after(testTime)));

        endCalendar.add(Calendar.MINUTE,-3); // back up three more minutes
        
        assertTrue("Final fire time not computed correctly for minutely interval.", (endCalendar.getTime().equals(testTime)));
    }
    
    public void testMisfireInstructionValidity() throws ParseException {
        CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl();

        try {
            trigger.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
            trigger.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY);
            trigger.setMisfireInstruction(CalendarIntervalTriggerImpl.MISFIRE_INSTRUCTION_DO_NOTHING);
            trigger.setMisfireInstruction(CalendarIntervalTriggerImpl.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        }
        catch(Exception e) {
            fail("Unexpected exception while setting misfire instruction.");
        }
        
        try {
            trigger.setMisfireInstruction(CalendarIntervalTriggerImpl.MISFIRE_INSTRUCTION_DO_NOTHING + 1);
            
            fail("Expected exception while setting invalid misfire instruction but did not get it.");
        }
        catch(Exception e) {
        }
    }
    
    @Override
    protected Object getTargetObject() throws Exception {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("A", "B");
        
        CalendarIntervalTriggerImpl t = new CalendarIntervalTriggerImpl();
        t.setName("test");
        t.setGroup("testGroup");
        t.setCalendarName("MyCalendar");
        t.setDescription("CronTriggerDesc");
        t.setJobDataMap(jobDataMap);
        t.setRepeatInterval(5);
        t.setRepeatIntervalUnit(IntervalUnit.DAY);

        return t;    
    }


    @Override
    protected String[] getVersions() {
        return VERSIONS;
    }

    @Override
    protected void verifyMatch(Object target, Object deserialized) {
        CalendarIntervalTriggerImpl targetCalTrigger = (CalendarIntervalTriggerImpl)target;
        CalendarIntervalTriggerImpl deserializedCalTrigger = (CalendarIntervalTriggerImpl)deserialized;

        assertNotNull(deserializedCalTrigger);
        assertEquals(targetCalTrigger.getName(), deserializedCalTrigger.getName());
        assertEquals(targetCalTrigger.getGroup(), deserializedCalTrigger.getGroup());
        assertEquals(targetCalTrigger.getJobName(), deserializedCalTrigger.getJobName());
        assertEquals(targetCalTrigger.getJobGroup(), deserializedCalTrigger.getJobGroup());
//        assertEquals(targetCronTrigger.getStartTime(), deserializedCronTrigger.getStartTime());
        assertEquals(targetCalTrigger.getEndTime(), deserializedCalTrigger.getEndTime());
        assertEquals(targetCalTrigger.getCalendarName(), deserializedCalTrigger.getCalendarName());
        assertEquals(targetCalTrigger.getDescription(), deserializedCalTrigger.getDescription());
        assertEquals(targetCalTrigger.getJobDataMap(), deserializedCalTrigger.getJobDataMap());
        assertEquals(targetCalTrigger.getRepeatInterval(), deserializedCalTrigger.getRepeatInterval());
        assertEquals(targetCalTrigger.getRepeatIntervalUnit(), deserializedCalTrigger.getRepeatIntervalUnit());
        
    }
    
    // execute with version number to generate a new version's serialized form
    public static void main(String[] args) throws Exception {
        new CalendarIntervalTriggerTest().writeJobDataFile("2.0");
    }




}
