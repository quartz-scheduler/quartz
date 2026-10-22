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
 */
package org.quartz.impl.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link CronCalendar}.
 */
public class CronCalendarTest {

    @Test
    void testSetCronExpressionKeepsTimeZone() throws Exception {
        // Not a likely JVM default, so a dropped time zone cannot pass by accident.
        TimeZone chatham = TimeZone.getTimeZone("Pacific/Chatham");
        CronCalendar cal = new CronCalendar(null, "* * 0-7 ? * *", chatham);
        cal.setCronExpression("* * 0-8 ? * *");
        assertEquals(chatham, cal.getTimeZone());
        assertEquals(chatham, cal.getCronExpression().getTimeZone());
    }
}
