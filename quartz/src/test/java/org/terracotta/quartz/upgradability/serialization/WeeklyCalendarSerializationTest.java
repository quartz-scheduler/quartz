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
import org.quartz.impl.calendar.WeeklyCalendar;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class WeeklyCalendarSerializationTest {
  
  private static final Comparator<WeeklyCalendar> COMPARATOR = new Comparator<WeeklyCalendar>() {
    @Override
    public int compare(WeeklyCalendar o1, WeeklyCalendar o2) {
      return Arrays.equals(o1.getDaysExcluded(), o2.getDaysExcluded())
              && (o1.getBaseCalendar() != null) == (o2.getBaseCalendar() != null)
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && nullSafeEquals(o1.getTimeZone(), o2.getTimeZone()) ? 0 : -1;
    }
  };
  
  @Test
  public void testNoDaysExcluded() throws IOException, ClassNotFoundException {
    WeeklyCalendar wc = new WeeklyCalendar();
    wc.setDaysExcluded(new boolean[8]);
    validateSerializedForm(wc, COMPARATOR, "serializedforms/WeeklyCalendarSerializationTest.testNoDaysExcluded.ser");
  }
  
  @Test
  public void testDefaultExcluded() throws IOException, ClassNotFoundException {
    WeeklyCalendar wc = new WeeklyCalendar();
    validateSerializedForm(wc, COMPARATOR, "serializedforms/WeeklyCalendarSerializationTest.testDefaultExcluded.ser");
  }
  
  @Test
  public void testAllDaysExcluded() throws IOException, ClassNotFoundException {
    WeeklyCalendar wc = new WeeklyCalendar();
    boolean[] excludeAll = new boolean[8];
    Arrays.fill(excludeAll, true);
    wc.setDaysExcluded(excludeAll);
    validateSerializedForm(wc, COMPARATOR, "serializedforms/WeeklyCalendarSerializationTest.testAllDaysExcluded.ser");
  }
  
  @Test
  public void testExtendedProperties() throws IOException, ClassNotFoundException {
    WeeklyCalendar wc = new WeeklyCalendar(new HolidayCalendar(), new SimplisticTimeZone("Terra Australis"));
    wc.setDescription("A Calendar");
    validateSerializedForm(wc, COMPARATOR, "serializedforms/WeeklyCalendarSerializationTest.testExtendedProperties.ser");
  }
}
