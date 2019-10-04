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
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.impl.triggers.CronTriggerImpl;

import static org.quartz.Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
import static org.terracotta.quartz.upgradability.serialization.Utilities.expand;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class CronTriggerImplSerializationTest {
  /*
    private CronExpression cronEx = null;
    private Date startTime = null;
    private Date endTime = null;
    private Date nextFireTime = null;
    private Date previousFireTime = null;
    private transient TimeZone timeZone = null;
  */
  private static final Comparator<CronTriggerImpl> COMPARATOR = new Comparator<CronTriggerImpl>() {
    @Override
    public int compare(CronTriggerImpl o1, CronTriggerImpl o2) {
      return o1.getJobKey().equals(o2.getJobKey())
              && o1.getKey().equals(o2.getKey())
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && (o1.getJobDataMap() == null) == (o2.getJobDataMap() == null)
              && o1.getCalendarName().equals(o2.getCalendarName())
              && o1.getStartTime().equals(o2.getStartTime())
              && o1.getEndTime().equals(o2.getEndTime())
              && nullSafeEquals(o1.getNextFireTime(), o2.getNextFireTime())
              && nullSafeEquals(o1.getPreviousFireTime(), o2.getPreviousFireTime())
              && o1.getCronExpression().equals(o2.getCronExpression())
              && nullSafeEquals(o1.getFireInstanceId(), o2.getFireInstanceId())
              && o1.getMisfireInstruction() == o2.getMisfireInstruction()
              && o1.getPriority() == o2.getPriority() ? 0 : -1;
    }
  };
  
  /*
    private CronExpression cronEx = null;
    private Date startTime = null;
    private Date endTime = null;
    private Date nextFireTime = null;
    private Date previousFireTime = null;
    private transient TimeZone timeZone = null;
  */
  @Test
  public void testConstructed() throws IOException, ClassNotFoundException, ParseException {
    CronTriggerImpl cti = new CronTriggerImpl("triggerName", "triggerGroup", "jobName", "jobGroup", new Date(0L), new Date(10000L), "0 0 12 * * ?", new SimplisticTimeZone("Terra Australis"));
    cti.setDescription("A Trigger");
    cti.setJobDataMap(new JobDataMap());
    cti.setCalendarName("calendarName");
    cti.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY);
    cti.setPriority(5);
    
    validateSerializedForm(cti, COMPARATOR, expand("serializedforms/CronTriggerImplSerializationTest.testConstructed.{?}.ser", "JDK16", "JDK17", "JDK18"));
  }
  
  @Test
  public void testFired() throws IOException, ClassNotFoundException, ParseException {
    CronTriggerImpl cti = new CronTriggerImpl("triggerName", "triggerGroup", "jobName", "jobGroup", new Date(0L), new Date(10000L), "0 0 12 * * ?", new SimplisticTimeZone("Terra Australis"));
    cti.setDescription("A Trigger");
    cti.setJobDataMap(new JobDataMap());
    cti.setCalendarName("calendarName");
    cti.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY);
    cti.setPriority(5);

    cti.triggered(null);
    
    validateSerializedForm(cti, COMPARATOR, expand("serializedforms/CronTriggerImplSerializationTest.testFired.{?}.ser", "JDK16", "JDK17", "JDK18"));
    
  }
  
}
