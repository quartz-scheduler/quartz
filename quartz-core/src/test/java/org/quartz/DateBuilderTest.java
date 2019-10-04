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

import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.quartz.DateBuilder.*;
import static org.quartz.DateBuilder.MILLISECONDS_IN_DAY;

import junit.framework.TestCase;

/**
 * Unit test for JobDetail.
 */
public class DateBuilderTest extends TestCase {
    
    public void testBasicBuilding() {
    	
    	
    	Date t = dateOf(10, 30, 0, 1, 7, 2013);  // july 1 10:30:00 am
    	
    	Calendar vc = Calendar.getInstance();
    	vc.set(Calendar.YEAR, 2013);
    	vc.set(Calendar.MONTH, Calendar.JULY);
    	vc.set(Calendar.DAY_OF_MONTH, 1);
    	vc.set(Calendar.HOUR_OF_DAY, 10);
    	vc.set(Calendar.MINUTE, 30);
    	vc.set(Calendar.SECOND, 0);
    	vc.set(Calendar.MILLISECOND, 0);
    	
    	Date v = vc.getTime();
    	
        assertEquals("DateBuilder-produced date is not as expected.", t, v);
    }

    public void testBuilder() {

        Calendar vc = Calendar.getInstance();
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JULY);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 30);
        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MILLISECOND, 0);

        Date bd = newDate().inYear(2013).inMonth(JULY).onDay(1).atHourOfDay(10).atMinute(30).atSecond(0).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        bd = newDate().inYear(2013).inMonthOnDay(JULY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);


        TimeZone tz = TimeZone.getTimeZone("GMT-4:00");
        Locale lz = Locale.TAIWAN;
        vc = Calendar.getInstance(tz, lz);
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JUNE);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 33);
        vc.set(Calendar.SECOND, 12);
        vc.set(Calendar.MILLISECOND, 0);

        bd = newDate().inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).inTimeZone(tz).inLocale(lz).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        bd = newDateInLocale(lz).inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).inTimeZone(tz).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        bd = newDateInTimezone(tz).inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).inLocale(lz).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        bd = newDateInTimeZoneAndLocale(tz, lz).inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

    }

    public void testEvensBuilders() {

        Calendar vc = Calendar.getInstance();
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JUNE);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 33);
        vc.set(Calendar.SECOND, 12);
        vc.set(Calendar.MILLISECOND, 0);

        Calendar rd = (Calendar) vc.clone();

        Date bd = newDate().inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).build();
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);


        rd.set(Calendar.MILLISECOND, 13);
        bd = evenSecondDateBefore(rd.getTime());
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        vc.set(Calendar.SECOND, 13);
        rd.set(Calendar.MILLISECOND, 13);
        bd = evenSecondDate(rd.getTime());
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 34);
        rd.set(Calendar.SECOND, 13);
        bd = evenMinuteDate(rd.getTime());
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 33);
        rd.set(Calendar.SECOND, 13);
        bd = evenMinuteDateBefore(rd.getTime());
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 0);
        vc.set(Calendar.HOUR_OF_DAY, 11);
        rd.set(Calendar.SECOND, 13);
        bd = evenHourDate(rd.getTime());
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 0);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        rd.set(Calendar.SECOND, 13);
        bd = evenHourDateBefore(rd.getTime());
        assertEquals("DateBuilder-produced date is not as expected.", vc.getTime(), bd);


        Date td = new Date();
        bd = evenHourDateAfterNow();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MINUTE));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.SECOND));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MILLISECOND));
        assertTrue("DateBuilder-produced date is not as expected.", bd.after(td));


        vc.set(Calendar.SECOND, 54);
        vc.set(Calendar.MINUTE, 13);
        vc.set(Calendar.HOUR_OF_DAY, 8);
        bd = nextGivenMinuteDate(vc.getTime(), 15);
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", 8, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals("DateBuilder-produced date is not as expected.", 15, vc.get(Calendar.MINUTE));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.SECOND));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MILLISECOND));
    }

    public void testGivenBuilders() {

        Calendar vc = Calendar.getInstance();

        vc.set(Calendar.SECOND, 54);
        vc.set(Calendar.MINUTE, 13);
        vc.set(Calendar.HOUR_OF_DAY, 8);
        Date bd = nextGivenMinuteDate(vc.getTime(), 45);
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", 8, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals("DateBuilder-produced date is not as expected.", 45, vc.get(Calendar.MINUTE));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.SECOND));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MILLISECOND));

        vc.set(Calendar.SECOND, 54);
        vc.set(Calendar.MINUTE, 46);
        vc.set(Calendar.HOUR_OF_DAY, 8);
        bd = nextGivenMinuteDate(vc.getTime(), 45);
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", 9, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MINUTE));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.SECOND));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MILLISECOND));
    }

    public void testAtBuilders() {

        Calendar rd = Calendar.getInstance();
        Calendar vc = Calendar.getInstance();

        rd.setTime(new Date());
        Date bd = todayAt(10, 33, 12);
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", 10, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals("DateBuilder-produced date is not as expected.", 33, vc.get(Calendar.MINUTE));
        assertEquals("DateBuilder-produced date is not as expected.", 12, vc.get(Calendar.SECOND));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MILLISECOND));
        assertEquals("DateBuilder-produced date is not as expected.", rd.get(Calendar.DAY_OF_YEAR), vc.get(Calendar.DAY_OF_YEAR));

        rd.setTime(new Date());
        rd.add(Calendar.MILLISECOND, (int)MILLISECONDS_IN_DAY); // increment the day (using this means on purpose - to test const)
        bd = tomorrowAt(10, 33, 12);
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", 10, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals("DateBuilder-produced date is not as expected.", 33, vc.get(Calendar.MINUTE));
        assertEquals("DateBuilder-produced date is not as expected.", 12, vc.get(Calendar.SECOND));
        assertEquals("DateBuilder-produced date is not as expected.", 0, vc.get(Calendar.MILLISECOND));
        assertEquals("DateBuilder-produced date is not as expected.", rd.get(Calendar.DAY_OF_YEAR), vc.get(Calendar.DAY_OF_YEAR));
    }

    public void testTranslate() {

        TimeZone tz1 = TimeZone.getTimeZone("GMT-2:00");
        TimeZone tz2 = TimeZone.getTimeZone("GMT-4:00");

        Calendar vc = Calendar.getInstance(tz1);
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JUNE);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 33);
        vc.set(Calendar.SECOND, 12);
        vc.set(Calendar.MILLISECOND, 0);

        vc.setTime( translateTime(vc.getTime(), tz1, tz2) );
        assertEquals("DateBuilder-produced date is not as expected.", 12, vc.get(Calendar.HOUR_OF_DAY));

        vc = Calendar.getInstance(tz2);
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JUNE);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 33);
        vc.set(Calendar.SECOND, 12);
        vc.set(Calendar.MILLISECOND, 0);

        vc.setTime( translateTime(vc.getTime(), tz2, tz1) );
        assertEquals("DateBuilder-produced date is not as expected.", 8, vc.get(Calendar.HOUR_OF_DAY));
    }

    public void testMonthTranslations() {

        Calendar vc = Calendar.getInstance();

        Date bd = newDate().inYear(2013).inMonthOnDay(JANUARY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.JANUARY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(FEBRUARY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.FEBRUARY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(MARCH, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.MARCH, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(APRIL, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.APRIL, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(MAY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.MAY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(JUNE, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.JUNE, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(JULY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.JULY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(AUGUST, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.AUGUST, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(SEPTEMBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.SEPTEMBER, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(OCTOBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.OCTOBER, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(NOVEMBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.NOVEMBER, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(DECEMBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals("DateBuilder-produced date is not as expected.", Calendar.DECEMBER, vc.get(Calendar.MONTH));

    }


}
