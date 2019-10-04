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
package org.quartz.impl.calendar;

import org.quartz.SerializationTestSupport;

/**
 * Unit test for DailyCalendar.
 */
public class DailyCalendarTest extends SerializationTestSupport {
    private static final String[] VERSIONS = new String[] {"1.5.2"};
    
    public void testStringStartEndTimes() {
        DailyCalendar dailyCalendar = new DailyCalendar("1:20", "14:50");
        assertTrue(dailyCalendar.toString().indexOf("01:20:00:000 - 14:50:00:000") > 0);
        
        dailyCalendar = new DailyCalendar("1:20:1:456", "14:50:15:2");
        assertTrue(dailyCalendar.toString().indexOf("01:20:01:456 - 14:50:15:002") > 0);
    }
    
    public void testStringInvertTimeRange() {
        DailyCalendar dailyCalendar = new DailyCalendar("1:20", "14:50");
        dailyCalendar.setInvertTimeRange(true);
        assertTrue(dailyCalendar.toString().indexOf("inverted: true") > 0);

        dailyCalendar.setInvertTimeRange(false);
        assertTrue(dailyCalendar.toString().indexOf("inverted: false") > 0);
    }
    
    /**
     * Get the object to serialize when generating serialized file for future
     * tests, and against which to validate deserialized object.
     */
    @Override
    protected Object getTargetObject() {
        DailyCalendar c = new DailyCalendar("01:20:01:456", "14:50:15:002");
        c.setDescription("description");
        c.setInvertTimeRange(true);
        
        return c;
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
        DailyCalendar targetCalendar = (DailyCalendar)target;
        DailyCalendar deserializedCalendar = (DailyCalendar)deserialized;
        
        assertNotNull(deserializedCalendar);
        assertEquals(targetCalendar.getDescription(), deserializedCalendar.getDescription());
        assertTrue(deserializedCalendar.getInvertTimeRange());
        assertNull(deserializedCalendar.getTimeZone());
        assertTrue(deserializedCalendar.toString().indexOf("01:20:01:456 - 14:50:15:002") > 0);
    }
}
