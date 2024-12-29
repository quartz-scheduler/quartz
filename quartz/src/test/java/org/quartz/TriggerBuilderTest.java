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

import static org.junit.jupiter.api.Assertions.*;
import static org.quartz.DateBuilder.evenSecondDateAfterNow;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.TriggerBuilder.newTrigger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;


import org.junit.jupiter.api.Test;
import org.quartz.DateBuilder.IntervalUnit;

/**
 * Test TriggerBuilder functionality
 */
public class TriggerBuilderTest  {


    @SuppressWarnings("deprecation")
    public static class TestStatefulJob implements StatefulJob {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }

    public static class TestJob implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }
    
    @DisallowConcurrentExecution
    @PersistJobDataAfterExecution
    public static class TestAnnotatedJob implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }

/*    @Override
    protected void setUp() throws Exception {
    }*/
    @Test
    void testTriggerBuilder() {
        
        Trigger trigger = newTrigger()
            .build();

        assertNotNull(trigger.getKey().getName(), "Expected non-null trigger name ");
        assertEquals(JobKey.DEFAULT_GROUP, trigger.getKey().getGroup(), "Unexpected trigger group: " + trigger.getKey().getGroup());
        assertNull(trigger.getJobKey(), "Unexpected job key: " + trigger.getJobKey());
        assertNull(trigger.getDescription(), "Unexpected job description: " + trigger.getDescription());
        assertEquals(Trigger.DEFAULT_PRIORITY, trigger.getPriority(), "Unexpected trigger priority: " + trigger.getPriority());
        assertNotNull(trigger.getStartTime(), "Unexpected start-time: " + trigger.getStartTime());
        assertNull(trigger.getEndTime(), "Unexpected end-time: " + trigger.getEndTime());
        
        Date stime = evenSecondDateAfterNow();
        
        trigger = newTrigger()
            .withIdentity("t1")
            .withDescription("my description")
            .withPriority(2)
            .endAt(futureDate(10, IntervalUnit.WEEK))
            .startAt(stime)
            .build();

        assertEquals("t1", trigger.getKey().getName(), "Unexpected trigger name " + trigger.getKey().getName());
        assertEquals(JobKey.DEFAULT_GROUP, trigger.getKey().getGroup(), "Unexpected trigger group: " + trigger.getKey().getGroup());
        assertNull(trigger.getJobKey(), "Unexpected job key: " + trigger.getJobKey());
        assertEquals("my description", trigger.getDescription(), "Unexpected job description: " + trigger.getDescription());
        assertEquals(2, trigger.getPriority(), "Unexpected trigger priority: " + trigger);
        assertEquals(trigger.getStartTime(), stime, "Unexpected start-time: " + trigger.getStartTime());
        assertNotNull(trigger.getEndTime(), "Unexpected end-time: " + trigger.getEndTime());
        
    }
    
    /** QTZ-157 */
    @Test
    void testTriggerBuilderWithEndTimePriorCurrentTime() throws Exception {
    	TriggerBuilder.newTrigger()
                .withIdentity("some trigger name", "some trigger group")
                .forJob("some job name", "some job group")
                .startAt(new Date(System.currentTimeMillis() - 200000000))
                .endAt(new Date(System.currentTimeMillis() - 100000000))
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();
    }

    @Test
    void testTriggerBuilderWithLocalDateTime() throws InterruptedException {
        LocalDateTime localDateTime = LocalDateTime.now().plusSeconds(3);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("triggerTest LocalDateTime", "triggerTest LocalDateTime group")
                .forJob("test job LocalDateTime", "test job LocalDateTime group")
                .startAt(localDateTime, ZoneId.systemDefault())
                .build();
        assertEquals(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), trigger.getStartTime());
        Thread.sleep(5000);
        assertEquals(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), trigger.getFinalFireTime());
    }

    @Test
    void testTriggerBuilderWithZonedDateTime() throws InterruptedException {
        ZonedDateTime zonedDateTime = ZonedDateTime.now().plusSeconds(3);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("triggerTest ", "triggerTest LocalDateTime group")
                .forJob("test job LocalDateTime", "test job LocalDateTime group")
                .startAt(zonedDateTime)
                .build();
        assertEquals(Date.from(zonedDateTime.toInstant()),trigger.getStartTime());
        Thread.sleep(5000);
        assertEquals(Date.from(zonedDateTime.toInstant()),trigger.getFinalFireTime());
    }

}
