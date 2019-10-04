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
import org.quartz.impl.triggers.SimpleTriggerImpl;

import static org.quartz.Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
import static org.terracotta.quartz.upgradability.serialization.Utilities.expand;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class SimpleTriggerImplSerializationTest {
  
  private static final Comparator<SimpleTriggerImpl> COMPARATOR = new Comparator<SimpleTriggerImpl>() {
    @Override
    public int compare(SimpleTriggerImpl o1, SimpleTriggerImpl o2) {
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
              && o1.getTimesTriggered() == o2.getTimesTriggered()
              && nullSafeEquals(o1.getFireInstanceId(), o2.getFireInstanceId())
              && o1.getMisfireInstruction() == o2.getMisfireInstruction()
              && o1.getPriority() == o2.getPriority() ? 0 : -1;
    }
  };
  
  @Test
  public void testConstructed() throws IOException, ClassNotFoundException {
    SimpleTriggerImpl sti = new SimpleTriggerImpl("triggerName", "triggerGroup", "jobName", "jobGroup", new Date(0L), new Date(10000L), 4, 100L);
    sti.setDescription("A Trigger");
    sti.setJobDataMap(new JobDataMap());
    sti.setCalendarName("calendarName");
    sti.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY);
    sti.setPriority(5);
    
    validateSerializedForm(sti, COMPARATOR, expand("serializedforms/SimpleTriggerImplSerializationTest.testConstructed.{?}.ser", "JDK16", "JDK17", "JDK18"));
  }
  
  @Test
  public void testFired() throws IOException, ClassNotFoundException {
    SimpleTriggerImpl sti = new SimpleTriggerImpl("triggerName", "triggerGroup", "jobName", "jobGroup", new Date(0L), new Date(10000L), 4, 100L);
    sti.setDescription("A Trigger");
    sti.setJobDataMap(new JobDataMap());
    sti.setCalendarName("calendarName");
    sti.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY);
    sti.setPriority(5);

    sti.triggered(null);
    
    validateSerializedForm(sti, COMPARATOR, expand("serializedforms/SimpleTriggerImplSerializationTest.testFired.{?}.ser", "JDK16", "JDK17", "JDK18"));
    
  }
}
