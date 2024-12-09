/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.junit.jupiter.api.Test;

import java.io.*;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class CronExpressionTest extends SerializationTestSupport {
    private static final String[] VERSIONS = new String[] {"1.5.2"};

    private static final TimeZone EST_TIME_ZONE = TimeZone.getTimeZone("US/Eastern"); 

    /**
     * Get the object to serialize when generating serialized file for future
     * tests, and against which to validate deserialized object.
     */
    @Override
    protected Object getTargetObject() throws ParseException {
        CronExpression cronExpression = new CronExpression("0 15 10 * * ? 2005");
        cronExpression.setTimeZone(EST_TIME_ZONE);
        
        return cronExpression;
    }
    
    /**
     * Get the Quartz versions for which we should verify
     * serialization backwards compatibility.
     */
    @Override
    protected String[] getVersions() {
        return VERSIONS;
    }
    
    /**
     * Verify that the target object and the object we just deserialized 
     * match.
     */
    @Override
    protected void verifyMatch(Object target, Object deserialized) {
        CronExpression targetCronExpression = (CronExpression)target;
        CronExpression deserializedCronExpression = (CronExpression)deserialized;
        
        assertNotNull(deserializedCronExpression);
        assertEquals(targetCronExpression.getCronExpression(), deserializedCronExpression.getCronExpression());
        assertEquals(targetCronExpression.getTimeZone(), deserializedCronExpression.getTimeZone());
    }

    void testTooManyTokens() throws Exception {
        try {
            new CronExpression("0 15 10 * * ? 2005 *"); // too many tokens/terms in expression
            fail("Expected ParseException did not occur for invalid expression");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().contains("too many"),
                    "Incorrect ParseException thrown");
        }

    }

    /*
     * Test method for 'org.quartz.CronExpression.isSatisfiedBy(Date)'.
     */
    @Test
    void testIsSatisfiedBy() throws Exception {
        CronExpression cronExpression = new CronExpression("0 15 10 * * ? 2005");
        
        Calendar cal = Calendar.getInstance();
        
        cal.set(2005, Calendar.JUNE, 1, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(Calendar.DAY_OF_MONTH, 30);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(Calendar.YEAR, 2006);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.JUNE, 1, 10, 16, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.JUNE, 1, 10, 14, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test for March
        cal = Calendar.getInstance();
        cal.set(2005, Calendar.MARCH, 1, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(Calendar.DAY_OF_MONTH, 31);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.MARCH, 1, 10, 16, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.MARCH, 1, 10, 14, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test for February
        cal = Calendar.getInstance();
        cal.set(2005, Calendar.FEBRUARY, 1, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(Calendar.DAY_OF_MONTH, 28);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.FEBRUARY, 1, 10, 16, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.FEBRUARY, 1, 10, 14, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test specific day of month
        cronExpression = new CronExpression("0 15 10 12 * ? 2005");
        cal.set(2005, Calendar.DECEMBER, 12, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.DECEMBER, 11, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.DECEMBER, 13, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
    }

    @Test
    void testIsSatisfiedByLastDayOfMonth() throws Exception {
        Calendar cal = Calendar.getInstance();

        // Test months with 31 days
        CronExpression cronExpression = new CronExpression("0 15 10 L * ? 2005");
        cal.set(2005, Calendar.DECEMBER, 31, 10, 15, 0); // December has 31 days
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.DECEMBER, 30, 10, 15, 0); // Not the last day
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test months with 30 days
        cronExpression = new CronExpression("0 15 10 L * ? 2005");
        cal.set(2005, Calendar.SEPTEMBER, 30, 10, 15, 0); // September has 30 days
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.SEPTEMBER, 29, 10, 15, 0); // Not the last day
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test February (non-leap year)
        cronExpression = new CronExpression("0 15 10 L 2 ? 2005");
        cal.set(2005, Calendar.FEBRUARY, 28, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.FEBRUARY, 27, 10, 15, 0); // Not the last day
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test February (leap year)
        cronExpression = new CronExpression("0 15 10 L 2 ? 2004");
        cal.set(2004, Calendar.FEBRUARY, 29, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2004, Calendar.FEBRUARY, 28, 10, 15, 0); // Not the last day
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
    }

    @Test
    void testIsSatisfiedByLastDayOfMonthWithOffset() throws Exception {
        Calendar cal = Calendar.getInstance();

        // Test months with 31 days
        CronExpression cronExpression = new CronExpression("0 15 10 L-2 * ? 2005");
        cal.set(2005, Calendar.DECEMBER, 29, 10, 15, 0); // December has 31 days, L-2 = 29th
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.DECEMBER, 28, 10, 15, 0); // Not L-2
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.DECEMBER, 30, 10, 15, 0); // Not L-2
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.DECEMBER, 31, 10, 15, 0); // Not L-2
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test months with 30 days
        cronExpression = new CronExpression("0 15 10 L-1 * ? 2005");
        cal.set(2005, Calendar.SEPTEMBER, 29, 10, 15, 0); // September has 30 days, L-1 = 29th
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.SEPTEMBER, 27, 10, 15, 0); // Not L-1
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.SEPTEMBER, 28, 10, 15, 0); // Not L-1
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.SEPTEMBER, 30, 10, 15, 0); // Not L-1
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test February (non-leap year)
        cronExpression = new CronExpression("0 15 10 L-3 2 ? 2005");
        cal.set(2005, Calendar.FEBRUARY, 25, 10, 15, 0); // February has 28 days in 2005, L-3 = 25th
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.FEBRUARY, 24, 10, 15, 0); // Not L-3
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.FEBRUARY, 26, 10, 15, 0); // Not L-3
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2005, Calendar.FEBRUARY, 28, 10, 15, 0); // Not L-3
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        // Test February (leap year)
        cronExpression = new CronExpression("0 15 10 L-3 2 ? 2000");
        cal.set(2000, Calendar.FEBRUARY, 26, 10, 15, 0); // February has 29 days in 2000, L-3 = 26th
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2000, Calendar.FEBRUARY, 25, 10, 15, 0); // Not L-3
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2000, Calendar.FEBRUARY, 27, 10, 15, 0); // Not L-3
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2000, Calendar.FEBRUARY, 29, 10, 15, 0); // Not L-3
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
    }

    @Test
    void testLastDayOffset() throws Exception {
        CronExpression cronExpression = new CronExpression("0 15 10 L-2 * ? 2010");
        
        Calendar cal = Calendar.getInstance();
        
        cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0); // last day - 2
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 28, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.FEBRUARY, 26, 10, 15, 0); // last day - 2 for February
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.FEBRUARY, 25, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.JUNE, 28, 10, 15, 0); // last day - 2 for June
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.JUNE, 27, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cronExpression = new CronExpression("0 15 10 L-5W * ? 2010");
        
        cal.set(2010, Calendar.OCTOBER, 26, 10, 15, 0); // last day - 5
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 25, 10, 15, 0); // not last day - 5
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.OCTOBER, 27, 10, 15, 0); // not last day - 5
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.SEPTEMBER, 24, 10, 15, 0); // last day - 5 (September has 30 days)
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.SEPTEMBER, 23, 10, 15, 0); // not last day - 5
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.SEPTEMBER, 25, 10, 15, 0); // not last day - 5
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cronExpression = new CronExpression("0 15 10 L-1 * ? 2010");
        
        cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0); // last day - 1
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cronExpression = new CronExpression("0 15 10 L-1W * ? 2010");
        
        cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0); // nearest weekday to last day - 1 (29th is a friday in 2010)
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cronExpression = new CronExpression("0 15 10 1,L * ? 2010");
        
        cal.set(2010, Calendar.OCTOBER, 1, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 31, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.FEBRUARY, 1, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.FEBRUARY, 28, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.FEBRUARY, 27, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cronExpression = new CronExpression("0 15 10 L-1W,L-1 * ? 2010");
        
        cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0); // nearest weekday to last day - 1 (29th is a friday in 2010)
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0); // last day - 1
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.FEBRUARY, 26, 10, 15, 0); // nearest weekday to last day - 1 (26th is a friday in 2010)
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.FEBRUARY, 27, 10, 15, 0); // last day - 1
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cronExpression = new CronExpression("0 15 10 2W,16 * ? 2010");
        
        cal.set(2010, Calendar.OCTOBER, 1, 10, 15, 0); // nearest weekday to the 2nd of the month (1st is a friday in 2010)
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 2, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.OCTOBER, 16, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
        
        cal.set(2010, Calendar.NOVEMBER, 2, 10, 15, 0); // 2nd is a Tuesday in November 2010
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.NOVEMBER, 16, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));
    }

    /*
     * QUARTZ-571: Showing that expressions with months correctly serialize.
     */
    @Test
    void testQuartz571() throws Exception {
        CronExpression cronExpression = new CronExpression("19 15 10 4 Apr ? ");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cronExpression);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        CronExpression newExpression = (CronExpression) ois.readObject();

        assertEquals(newExpression.getCronExpression(), cronExpression.getCronExpression());

        // if broken, this will throw an exception
        newExpression.getNextValidTimeAfter(new Date());
    }

    /**
     * QTZ-259 : last day offset causes repeating fire time
     * 
     */
    @Test
 	void testQtz259() throws Exception {
 		CronScheduleBuilder schedBuilder = CronScheduleBuilder.cronSchedule("0 0 0 L-2 * ? *");
 		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("test").withSchedule(schedBuilder).build();
 				
 		int i = 0;
 		Date previousDate = trigger.getFireTimeAfter(new Date());
 		while (++i < 26) {
 			Date date = trigger.getFireTimeAfter(previousDate);
 			System.out.println("fireTime: " + date + ", previousFireTime: " + previousDate);
            assertNotEquals(previousDate, date, "Next fire time is the same as previous fire time!");
 			previousDate = date;
 		}
 	}
    
    /**
     * QTZ-259 : last day offset causes repeating fire time
     * 
     */
    @Test
 	void testQtz259LW() throws Exception {
 		CronScheduleBuilder schedBuilder = CronScheduleBuilder.cronSchedule("0 0 0 LW * ? *");
 		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("test").withSchedule(schedBuilder).build();
 				
 		int i = 0;
 		Date pdate = trigger.getFireTimeAfter(new Date());
 		while (++i < 26) {
 			Date date = trigger.getFireTimeAfter(pdate);
 			System.out.println("fireTime: " + date + ", previousFireTime: " + pdate);
            assertNotEquals(pdate, date, "Next fire time is the same as previous fire time!");
 			pdate = date;
 		}
 	}
 	
    /*
     * QUARTZ-574: Showing that storeExpressionVals correctly calculates the month number
     */
    @Test
    void testQuartz574() {
        try {
            new CronExpression("* * * * Foo ? ");
            fail("Expected ParseException did not fire for nonexistent month");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Invalid Month value:"),
                    "Incorrect ParseException thrown");
        }

        try {
            new CronExpression("* * * * Jan-Foo ? ");
            fail("Expected ParseException did not fire for nonexistent month");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Invalid Month value:"),
                    "Incorrect ParseException thrown");
        }
    }

    @Test
    void testQuartz621() {
        try {
            new CronExpression("0 0 * * * *");
            fail("Expected ParseException did not fire for wildcard day-of-month and day-of-week");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 0 * 4 * *");
            fail("Expected ParseException did not fire for specified day-of-month and wildcard day-of-week");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 0 * * * 4");
            fail("Expected ParseException did not fire for wildcard day-of-month and specified day-of-week");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."),
                    "Incorrect ParseException thrown");
        }
    }

    @Test
    void testQuartz640() throws ParseException {
        try {
            new CronExpression("0 43 9 ? * SAT,SUN,L");
            fail("Expected ParseException did not fire for L combined with other days of the week");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Support for specifying 'L' with other days of the week is not implemented"),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 43 9 ? * 6,7,L");
            fail("Expected ParseException did not fire for L combined with other days of the week");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("Support for specifying 'L' with other days of the week is not implemented"),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 43 9 ? * 5L");
        } catch(ParseException pe) {
            fail("Unexpected ParseException thrown for supported '5L' expression.");
        }
    }

    @Test
    void testQtz96() throws ParseException {
        try {
            new CronExpression("0/5 * * 32W 1 ?");
            fail("Expected ParseException did not fire for W with value larger than 31");
        } catch(ParseException pe) {
            assertTrue(pe.getMessage().startsWith("The 'W' option does not make sense with values larger than"),
                    "Incorrect ParseException thrown");
        }
    }

    void testQtz395_CopyConstructorMustPreserveTimeZone () throws ParseException {
        TimeZone nonDefault = TimeZone.getTimeZone("Europe/Brussels");
        if (nonDefault.equals(TimeZone.getDefault())) {
            nonDefault = EST_TIME_ZONE;
        }
        CronExpression cronExpression = new CronExpression("0 15 10 * * ? 2005");
        cronExpression.setTimeZone(nonDefault);

        CronExpression copyCronExpression = new CronExpression(cronExpression);
        assertEquals(nonDefault, copyCronExpression.getTimeZone());
    }

    // Issue #58
    @Test
    void testSecRangeIntervalAfterSlash() throws Exception {
        // Test case 1
        try {
            new CronExpression("/120 0 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in '_blank/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 60 : 120", e.getMessage());
        }

        // Test case 2
        try {
            new CronExpression("0/120 0 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in in '0/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 60 : 120", e.getMessage());
        }

        // Test case 3
        try {
            new CronExpression("/ 0 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in '_blank/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }

        // Test case 4
        try {
            new CronExpression("0/ 0 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in '0/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }
    }


    // Issue #58
    @Test
    void testMinRangeIntervalAfterSlash() throws Exception {
        // Test case 1
        try {
            new CronExpression("0 /120 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in '_blank/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 60 : 120", e.getMessage());
        }

        // Test case 2
        try {
            new CronExpression("0 0/120 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in in '0/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 60 : 120", e.getMessage());
        }

        // Test case 3
        try {
            new CronExpression("0 / 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in '_blank/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }

        // Test case 4
        try {
            new CronExpression("0 0/ 8-18 ? * 2-6");
            fail("Cron did not validate bad range interval in '0/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }
    }

    // Issue #58
    @Test
    void testHourRangeIntervalAfterSlash() throws Exception {
        // Test case 1
        try {
            new CronExpression("0 0 /120 ? * 2-6");
            fail("Cron did not validate bad range interval in '_blank/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 24 : 120", e.getMessage());
        }

        // Test case 2
        try {
            new CronExpression("0 0 0/120 ? * 2-6");
            fail("Cron did not validate bad range interval in in '0/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 24 : 120", e.getMessage());
        }

        // Test case 3
        try {
            new CronExpression("0 0 / ? * 2-6");
            fail("Cron did not validate bad range interval in '_blank/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }

        // Test case 4
        try {
            new CronExpression("0 0 0/ ? * 2-6");
            fail("Cron did not validate bad range interval in '0/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }
    }

    // Issue #58
    @Test
    void testDayOfMonthRangeIntervalAfterSlash() throws Exception {
        // Test case 1
        try {
            new CronExpression("0 0 0 /120 * 2-6");
            fail("Cron did not validate bad range interval in '_blank/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 31 : 120", e.getMessage());
        }

        // Test case 2
        try {
            new CronExpression("0 0 0 0/120 * 2-6");
            fail("Cron did not validate bad range interval in in '0/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 31 : 120", e.getMessage());
        }

        // Test case 3
        try {
            new CronExpression("0 0 0 / * 2-6");
            fail("Cron did not validate bad range interval in '_blank/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }

        // Test case 4
        try {
            new CronExpression("0 0 0 0/ * 2-6");
            fail("Cron did not validate bad range interval in '0/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }
    }

    // Issue #58
    @Test
    void testMonthRangeIntervalAfterSlash() throws Exception {
        // Test case 1
        try {
            new CronExpression("0 0 0 ? /120 2-6");
            fail("Cron did not validate bad range interval in '_blank/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 12 : 120", e.getMessage());
        }

        // Test case 2
        try {
            new CronExpression("0 0 0 ? 0/120 2-6");
            fail("Cron did not validate bad range interval in in '0/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 12 : 120", e.getMessage());
        }

        // Test case 3
        try {
            new CronExpression("0 0 0 ? / 2-6");
            fail("Cron did not validate bad range interval in '_blank/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }

        // Test case 4
        try {
            new CronExpression("0 0 0 ? 0/ 2-6");
            fail("Cron did not validate bad range interval in '0/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }
    }



    // Issue #58
    @Test
    void testDayOfWeekRangeIntervalAfterSlash() throws Exception {
        // Test case 1
        try {
            new CronExpression("0 0 0 ? * /120");
            fail("Cron did not validate bad range interval in '_blank/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 7 : 120", e.getMessage());
        }

        // Test case 2
        try {
            new CronExpression("0 0 0 ? * 0/120");
            fail("Cron did not validate bad range interval in in '0/xxx' form");
        } catch (ParseException e) {
            assertEquals("Increment > 7 : 120", e.getMessage());
        }

        // Test case 3
        try {
            new CronExpression("0 0 0 ? * /");
            fail("Cron did not validate bad range interval in '_blank/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }

        // Test case 4
        try {
            new CronExpression("0 0 0 ? * 0/");
            fail("Cron did not validate bad range interval in '0/_blank'");
        } catch (ParseException e) {
            assertEquals("'/' must be followed by an integer.", e.getMessage());
        }
    }
    
    // execute with version number to generate a new version's serialized form
    public static void main(String[] args) throws Exception {
        new CronExpressionTest().writeJobDataFile("1.5.2");
    }

}
