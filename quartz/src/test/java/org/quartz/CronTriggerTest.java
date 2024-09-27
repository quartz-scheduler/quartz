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

import java.text.ParseException;

import org.quartz.impl.triggers.CronTriggerImpl;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for CronTrigger.
 */
public class CronTriggerTest extends SerializationTestSupport {

    private static final String[] VERSIONS = new String[] {"2.0"};

    /**
     * Get the Quartz versions for which we should verify
     * serialization backwards compatibility.
     */
    @Override
    protected String[] getVersions() {
        return VERSIONS;
    }
    
    /**
     * Get the object to serialize when generating serialized file for future
     * tests, and against which to validate deserialized object.
     */
    @Override
    protected Object getTargetObject() throws Exception {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("A", "B");
        
        CronTriggerImpl t = new CronTriggerImpl();
        t.setName("test");
        t.setGroup("testGroup");
        t.setCronExpression("0 0 12 * * ?");
        t.setCalendarName("MyCalendar");
        t.setDescription("CronTriggerDesc");
        t.setJobDataMap(jobDataMap);

        return t;
    }
    
    /**
     * Verify that the target object and the object we just deserialized 
     * match.
     */
    @Override
    protected void verifyMatch(Object target, Object deserialized) {
        CronTriggerImpl targetCronTrigger = (CronTriggerImpl)target;
        CronTriggerImpl deserializedCronTrigger = (CronTriggerImpl)deserialized;

        assertNotNull(deserializedCronTrigger);
        assertEquals(targetCronTrigger.getName(), deserializedCronTrigger.getName());
        assertEquals(targetCronTrigger.getGroup(), deserializedCronTrigger.getGroup());
        assertEquals(targetCronTrigger.getJobName(), deserializedCronTrigger.getJobName());
        assertEquals(targetCronTrigger.getJobGroup(), deserializedCronTrigger.getJobGroup());
//        assertEquals(targetCronTrigger.getStartTime(), deserializedCronTrigger.getStartTime());
        assertEquals(targetCronTrigger.getEndTime(), deserializedCronTrigger.getEndTime());
        assertEquals(targetCronTrigger.getCalendarName(), deserializedCronTrigger.getCalendarName());
        assertEquals(targetCronTrigger.getDescription(), deserializedCronTrigger.getDescription());
        assertEquals(targetCronTrigger.getJobDataMap(), deserializedCronTrigger.getJobDataMap());
        assertEquals(targetCronTrigger.getCronExpression(), deserializedCronTrigger.getCronExpression());
    }
        
    
    public void testClone() throws ParseException {
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setName("test");
        trigger.setGroup("testGroup");
        trigger.setCronExpression("0 0 12 * * ?");
        CronTrigger trigger2 = (CronTrigger) trigger.clone();

        assertEquals( "Cloning failed", trigger, trigger2 );

        // equals() doesn't test the cron expression
        assertEquals( "Cloning failed for the cron expression", 
                      "0 0 12 * * ?", trigger2.getCronExpression()
                    );
    }

    // http://jira.opensymphony.com/browse/QUARTZ-558
    public void testQuartz558() throws ParseException {
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setName("test");
        trigger.setGroup("testGroup");
        CronTrigger trigger2 = (CronTrigger) trigger.clone();

        assertEquals( "Cloning failed", trigger, trigger2 );
    }

    public void testMisfireInstructionValidity() throws ParseException {
        CronTriggerImpl trigger = new CronTriggerImpl();

        try {
            trigger.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
            trigger.setMisfireInstruction(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY);
            trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
            trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        }
        catch(Exception e) {
            fail("Unexpected exception while setting misfire instruction.");
        }
        
        try {
            trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING + 1);
            
            fail("Expected exception while setting invalid misfire instruction but did not get it.");
        }
        catch(Exception e) {
        }
    }

    public void testMisfireInstructionInDerivedBuilder() throws ParseException {
        for (int policy : asList(
                Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                Trigger.MISFIRE_INSTRUCTION_SMART_POLICY,
                CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING,
                CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW)
        ) {
            CronTriggerImpl trigger = new CronTriggerImpl();
            trigger.setCronExpression("0 0 12 * * ?");
            trigger.setMisfireInstruction(policy);
            assertThat(trigger.getMisfireInstruction(), is(policy));

            CronTrigger copy = trigger.getTriggerBuilder().build();
            assertThat(copy.getMisfireInstruction(), is(policy));
        }
    }

    public void testUndefinedMisfireInstructionInDerivedBuilder() throws ParseException {
        CronTriggerImpl trigger = new CronTriggerImpl() {
            @Override
            public int getMisfireInstruction() {
                return 12345;
            }
        };
        trigger.setCronExpression("0 0 12 * * ?");
        try {
            trigger.setMisfireInstruction(12345);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("The misfire instruction code is invalid for this type of trigger."));
        }

        CronTrigger copy = trigger.getTriggerBuilder().build();
        assertThat(copy.getMisfireInstruction(), is(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY));
    }

    // execute with version number to generate a new version's serialized form
    public static void main(String[] args) throws Exception {
        new CronTriggerTest().writeJobDataFile("2.0");
    }

}
