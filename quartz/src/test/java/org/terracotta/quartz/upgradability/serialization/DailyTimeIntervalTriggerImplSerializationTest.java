/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.quartz.upgradability.serialization;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.TimeOfDay;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;

import static org.quartz.DateBuilder.IntervalUnit.MINUTE;
import static org.quartz.Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
import static org.terracotta.quartz.upgradability.serialization.Utilities.expand;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class DailyTimeIntervalTriggerImplSerializationTest {
  
  private static final Comparator<DailyTimeIntervalTriggerImpl> COMPARATOR = new Comparator<DailyTimeIntervalTriggerImpl>() {
    @Override
    public int compare(DailyTimeIntervalTriggerImpl o1, DailyTimeIntervalTriggerImpl o2) {
      return o1.getJobKey().equals(o2.getJobKey())
              && o1.getKey().equals(o2.getKey())
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && (o1.getJobDataMap() == null) == (o2.getJobDataMap() == null)
              && o1.getCalendarName().equals(o2.getCalendarName())
              && o1.getStartTime().equals(o2.getStartTime())
              && o1.getEndTime().equals(o2.getEndTime())
              && nullSafeEquals(o1.getNextFireTime(), o2.getNextFireTime())
              && nullSafeEquals(o1.getPreviousFireTime(), o2.getPreviousFireTime())
              && o1.getRepeatCount() == o2.getRepeatCount()
              && o1.getRepeatInterval() == o2.getRepeatInterval()
              && o1.getRepeatIntervalUnit().equals(o2.getRepeatIntervalUnit())
              && o1.getDaysOfWeek().equals(o2.getDaysOfWeek())
              && o1.getStartTimeOfDay().equals(o2.getStartTimeOfDay())
              && o1.getEndTimeOfDay().equals(o2.getEndTimeOfDay())
              && o1.getTimesTriggered() == o2.getTimesTriggered()
              && nullSafeEquals(o1.getFireInstanceId(), o2.getFireInstanceId())
              && o1.getMisfireInstruction() == o2.getMisfireInstruction()
              && o1.getPriority() == o2.getPriority() ? 0 : -1;
    }
  };
  
  @Test
  public void testConstructed() throws IOException, ClassNotFoundException {
    DailyTimeIntervalTriggerImpl dti = new DailyTimeIntervalTriggerImpl("triggerName", "triggerGroup", "jobName", "jobGroup", new Date(0L), new Date(10000L), new TimeOfDay(10, 45, 30), new TimeOfDay(23, 30, 15), MINUTE,  5);
    dti.setDescription("A Trigger");
    dti.setJobDataMap(new JobDataMap());
    dti.setCalendarName("calendarName");
    dti.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY);
    dti.setPriority(5);
    
    validateSerializedForm(dti, COMPARATOR, expand("serializedforms/DailyTimeIntervalTriggerImplSerializationTest.testConstructed.{?}.ser", "JDK16", "JDK17", "JDK18"));
  }
  
  @Test
  public void testFired() throws IOException, ClassNotFoundException {
    DailyTimeIntervalTriggerImpl dti = new DailyTimeIntervalTriggerImpl("triggerName", "triggerGroup", "jobName", "jobGroup", new Date(0L), new Date(10000L), new TimeOfDay(10, 45, 30), new TimeOfDay(23, 30, 15), MINUTE,  5);
    dti.setDescription("A Trigger");
    dti.setJobDataMap(new JobDataMap());
    dti.setCalendarName("calendarName");
    dti.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY);
    dti.setPriority(5);

    dti.triggered(null);
    
    validateSerializedForm(dti, COMPARATOR, expand("serializedforms/DailyTimeIntervalTriggerImplSerializationTest.testFired.{?}.ser", "JDK16", "JDK17_1", "JDK17_2", "JDK18"));
  }
}
