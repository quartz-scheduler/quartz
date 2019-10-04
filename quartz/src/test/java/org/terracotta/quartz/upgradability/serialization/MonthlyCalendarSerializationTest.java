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
import java.util.Arrays;
import java.util.Comparator;

import org.junit.Test;
import org.quartz.impl.calendar.HolidayCalendar;
import org.quartz.impl.calendar.MonthlyCalendar;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class MonthlyCalendarSerializationTest {
  
  private static final Comparator<MonthlyCalendar> COMPARATOR = new Comparator<MonthlyCalendar>() {

    @Override
    public int compare(MonthlyCalendar o1, MonthlyCalendar o2) {
      return Arrays.equals(o1.getDaysExcluded(), o2.getDaysExcluded())
              && (o1.getBaseCalendar() != null) == (o2.getBaseCalendar() != null)
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && nullSafeEquals(o1.getTimeZone(), o2.getTimeZone()) ? 0 : -1;
    }
  };

  @Test
  public void testNoDaysExcluded() throws IOException, ClassNotFoundException {
    MonthlyCalendar mc = new MonthlyCalendar();
    mc.setDaysExcluded(new boolean[31]);
    validateSerializedForm(mc, COMPARATOR, "serializedforms/MonthlyCalendarSerializationTest.testNoDaysExcluded.ser");
  }
  
  @Test
  public void testDefaultExcluded() throws IOException, ClassNotFoundException {
    MonthlyCalendar mc = new MonthlyCalendar();
    validateSerializedForm(mc, COMPARATOR, "serializedforms/MonthlyCalendarSerializationTest.testDefaultExcluded.ser");
  }
  
  @Test
  public void testAllDaysExcluded() throws IOException, ClassNotFoundException {
    MonthlyCalendar mc = new MonthlyCalendar();
    boolean[] excludeAll = new boolean[31];
    Arrays.fill(excludeAll, true);
    mc.setDaysExcluded(excludeAll);
    validateSerializedForm(mc, COMPARATOR, "serializedforms/MonthlyCalendarSerializationTest.testAllDaysExcluded.ser");
  }
  
  @Test
  public void testExtendedProperties() throws IOException, ClassNotFoundException {
    MonthlyCalendar mc = new MonthlyCalendar(new HolidayCalendar(), new SimplisticTimeZone("Terra Australis"));
    mc.setDescription("A Calendar");
    validateSerializedForm(mc, COMPARATOR, "serializedforms/MonthlyCalendarSerializationTest.testExtendedProperties.ser");
  }
}
