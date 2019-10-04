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
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;
import org.terracotta.quartz.wrappers.DefaultWrapperFactory;
import org.terracotta.quartz.wrappers.TriggerFacade;
import org.terracotta.quartz.wrappers.TriggerWrapper;
import org.terracotta.quartz.wrappers.WrapperFactory;

import static org.mockito.Mockito.mock;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class TriggerWrapperSerializationTest {
  
  private static final Comparator<TriggerWrapper> COMPARATOR = new Comparator<TriggerWrapper>() {
    @Override
    public int compare(TriggerWrapper o1, TriggerWrapper o2) {
      return o1.jobDisallowsConcurrence() == o2.jobDisallowsConcurrence()
              && nullSafeEquals(o1.getLastTerracotaClientId(), o2.getLastTerracotaClientId())
              && o1.getState() == o2.getState()
              && (o1.getTriggerClone() == null) == (o2.getTriggerClone() == null) ? 0 : -1;
    }
  };

  @Test
  public void testSimple() throws IOException, ClassNotFoundException {
    WrapperFactory factory = new DefaultWrapperFactory();
    TriggerWrapper tw = factory.createTriggerWrapper(new DummyOperableTrigger(), false);
    validateSerializedForm(tw, COMPARATOR, "serializedforms/TriggerWrapperSerializationTest.testSimple.ser");
  }
  
  @Test
  public void testAcquired() throws IOException, ClassNotFoundException {
    WrapperFactory factory = new DefaultWrapperFactory();
    TriggerWrapper tw = factory.createTriggerWrapper(new DummyOperableTrigger(), false);
    tw.setState(TriggerWrapper.TriggerState.ACQUIRED, "client-id", mock(TriggerFacade.class));
    validateSerializedForm(tw, COMPARATOR, "serializedforms/TriggerWrapperSerializationTest.testAcquired.ser");
  }
  
  static class DummyOperableTrigger implements OperableTrigger {

    @Override
    public void triggered(Calendar calendar) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date computeFirstFireTime(Calendar calendar) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CompletedExecutionInstruction executionComplete(JobExecutionContext context, JobExecutionException result) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateAfterMisfire(Calendar cal) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateWithNewCalendar(Calendar cal, long misfireThreshold) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate() throws SchedulerException {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setFireInstanceId(String id) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getFireInstanceId() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNextFireTime(Date nextFireTime) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPreviousFireTime(Date previousFireTime) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setKey(TriggerKey key) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setJobKey(JobKey key) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDescription(String description) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCalendarName(String calendarName) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setJobDataMap(JobDataMap jobDataMap) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPriority(int priority) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setStartTime(Date startTime) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setEndTime(Date endTime) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMisfireInstruction(int misfireInstruction) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TriggerKey getKey() {
      return new TriggerKey("name", "group");
    }

    @Override
    public JobKey getJobKey() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDescription() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getCalendarName() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JobDataMap getJobDataMap() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getPriority() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean mayFireAgain() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date getStartTime() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date getEndTime() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date getNextFireTime() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date getPreviousFireTime() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date getFireTimeAfter(Date afterTime) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Date getFinalFireTime() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMisfireInstruction() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TriggerBuilder<? extends Trigger> getTriggerBuilder() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScheduleBuilder<? extends Trigger> getScheduleBuilder() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int compareTo(Trigger other) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DummyOperableTrigger clone() {
      return new DummyOperableTrigger();
    }
  }
}
