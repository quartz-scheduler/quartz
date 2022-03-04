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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.quartz.DateBuilder.*;
import static org.quartz.DateBuilder.MILLISECONDS_IN_DAY;



/**
 * Unit test for JobDetail.
 */
class DateBuilderTest {
    
    @Test
    void testBasicBuilding() {
    	
    	
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
    	
        assertEquals(t, v);
    }

    @Test
    void testBuilder() {

        Calendar vc = Calendar.getInstance();
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JULY);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 30);
        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MILLISECOND, 0);

        Date bd = newDate().inYear(2013).inMonth(JULY).onDay(1).atHourOfDay(10).atMinute(30).atSecond(0).build();
        assertEquals(vc.getTime(), bd);

        bd = newDate().inYear(2013).inMonthOnDay(JULY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        assertEquals(vc.getTime(), bd);


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
        assertEquals(vc.getTime(), bd);

        bd = newDateInLocale(lz).inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).inTimeZone(tz).build();
        assertEquals(vc.getTime(), bd);

        bd = newDateInTimezone(tz).inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).inLocale(lz).build();
        assertEquals(vc.getTime(), bd);

        bd = newDateInTimeZoneAndLocale(tz, lz).inYear(2013).inMonth(JUNE).onDay(1).atHourOfDay(10).atMinute(33).atSecond(12).build();
        assertEquals(vc.getTime(), bd);

    }

    @Test
    void testEvensBuilders() {

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
        assertEquals(vc.getTime(), bd);


        rd.set(Calendar.MILLISECOND, 13);
        bd = evenSecondDateBefore(rd.getTime());
        assertEquals(vc.getTime(), bd);

        vc.set(Calendar.SECOND, 13);
        rd.set(Calendar.MILLISECOND, 13);
        bd = evenSecondDate(rd.getTime());
        assertEquals(vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 34);
        rd.set(Calendar.SECOND, 13);
        bd = evenMinuteDate(rd.getTime());
        assertEquals(vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 33);
        rd.set(Calendar.SECOND, 13);
        bd = evenMinuteDateBefore(rd.getTime());
        assertEquals(vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 0);
        vc.set(Calendar.HOUR_OF_DAY, 11);
        rd.set(Calendar.SECOND, 13);
        bd = evenHourDate(rd.getTime());
        assertEquals(vc.getTime(), bd);

        vc.set(Calendar.SECOND, 0);
        vc.set(Calendar.MINUTE, 0);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        rd.set(Calendar.SECOND, 13);
        bd = evenHourDateBefore(rd.getTime());
        assertEquals(vc.getTime(), bd);


        Date td = new Date();
        bd = evenHourDateAfterNow();
        vc.setTime(bd);
        assertEquals(0, vc.get(Calendar.MINUTE));
        assertEquals(0, vc.get(Calendar.SECOND));
        assertEquals(0, vc.get(Calendar.MILLISECOND));
        assertTrue(bd.after(td));


        vc.set(Calendar.SECOND, 54);
        vc.set(Calendar.MINUTE, 13);
        vc.set(Calendar.HOUR_OF_DAY, 8);
        bd = nextGivenMinuteDate(vc.getTime(), 15);
        vc.setTime(bd);
        assertEquals(8, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, vc.get(Calendar.MINUTE));
        assertEquals(0, vc.get(Calendar.SECOND));
        assertEquals(0, vc.get(Calendar.MILLISECOND));
    }

    @Test
    void testGivenBuilders() {

        Calendar vc = Calendar.getInstance();

        vc.set(Calendar.SECOND, 54);
        vc.set(Calendar.MINUTE, 13);
        vc.set(Calendar.HOUR_OF_DAY, 8);
        Date bd = nextGivenMinuteDate(vc.getTime(), 45);
        vc.setTime(bd);
        assertEquals(8, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, vc.get(Calendar.MINUTE));
        assertEquals(0, vc.get(Calendar.SECOND));
        assertEquals(0, vc.get(Calendar.MILLISECOND));

        vc.set(Calendar.SECOND, 54);
        vc.set(Calendar.MINUTE, 46);
        vc.set(Calendar.HOUR_OF_DAY, 8);
        bd = nextGivenMinuteDate(vc.getTime(), 45);
        vc.setTime(bd);
        assertEquals(9, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, vc.get(Calendar.MINUTE));
        assertEquals(0, vc.get(Calendar.SECOND));
        assertEquals(0, vc.get(Calendar.MILLISECOND));
    }

    @Test
    void testAtBuilders() {

        Calendar rd = Calendar.getInstance();
        Calendar vc = Calendar.getInstance();

        rd.setTime(new Date());
        Date bd = todayAt(10, 33, 12);
        vc.setTime(bd);
        assertEquals(10, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals(33, vc.get(Calendar.MINUTE));
        assertEquals(12, vc.get(Calendar.SECOND));
        assertEquals(0, vc.get(Calendar.MILLISECOND));
        assertEquals(rd.get(Calendar.DAY_OF_YEAR), vc.get(Calendar.DAY_OF_YEAR));

        rd.setTime(new Date());
        rd.add(Calendar.MILLISECOND, (int)MILLISECONDS_IN_DAY); // increment the day (using this means on purpose - to test const)
        bd = tomorrowAt(10, 33, 12);
        vc.setTime(bd);
        assertEquals(10, vc.get(Calendar.HOUR_OF_DAY));
        assertEquals(33, vc.get(Calendar.MINUTE));
        assertEquals(12, vc.get(Calendar.SECOND));
        assertEquals(0, vc.get(Calendar.MILLISECOND));
        assertEquals(rd.get(Calendar.DAY_OF_YEAR), vc.get(Calendar.DAY_OF_YEAR));
    }

    @Test
    void testTranslate() {

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
        assertEquals(12, vc.get(Calendar.HOUR_OF_DAY));

        vc = Calendar.getInstance(tz2);
        vc.set(Calendar.YEAR, 2013);
        vc.set(Calendar.MONTH, Calendar.JUNE);
        vc.set(Calendar.DAY_OF_MONTH, 1);
        vc.set(Calendar.HOUR_OF_DAY, 10);
        vc.set(Calendar.MINUTE, 33);
        vc.set(Calendar.SECOND, 12);
        vc.set(Calendar.MILLISECOND, 0);

        vc.setTime( translateTime(vc.getTime(), tz2, tz1) );
        assertEquals(8, vc.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    void testMonthTranslations() {

        Calendar vc = Calendar.getInstance();

        Date bd = newDate().inYear(2013).inMonthOnDay(JANUARY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.JANUARY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(FEBRUARY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.FEBRUARY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(MARCH, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.MARCH, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(APRIL, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.APRIL, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(MAY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.MAY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(JUNE, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.JUNE, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(JULY, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.JULY, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(AUGUST, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.AUGUST, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(SEPTEMBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.SEPTEMBER, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(OCTOBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.OCTOBER, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(NOVEMBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.NOVEMBER, vc.get(Calendar.MONTH));

        bd = newDate().inYear(2013).inMonthOnDay(DECEMBER, 1).atHourMinuteAndSecond(10, 30, 0).build();
        vc.setTime(bd);
        assertEquals(Calendar.DECEMBER, vc.get(Calendar.MONTH));

    }


}
