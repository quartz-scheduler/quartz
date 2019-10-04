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
import org.quartz.impl.calendar.HolidayCalendar;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class HolidayCalendarSerializationTest {
  
  private static final Comparator<HolidayCalendar> COMPARATOR = new Comparator<HolidayCalendar>() {

    @Override
    public int compare(HolidayCalendar o1, HolidayCalendar o2) {
      return o1.getExcludedDates().equals(o2.getExcludedDates())
              && (o1.getBaseCalendar() != null) == (o2.getBaseCalendar() != null)
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && nullSafeEquals(o1.getTimeZone(), o2.getTimeZone()) ? 0 : -1;
    }
  };

  @Test
  public void testNoDaysExcluded() throws IOException, ClassNotFoundException {
    HolidayCalendar hc = new HolidayCalendar(new SimplisticTimeZone("Terra Australis"));
    validateSerializedForm(hc, COMPARATOR, "serializedforms/HolidayCalendarSerializationTest.testNoDaysExcluded.ser");
  }
  
  @Test
  public void testOneDayExcluded() throws IOException, ClassNotFoundException {
    HolidayCalendar hc = new HolidayCalendar(new SimplisticTimeZone("Terra Australis"));
    hc.addExcludedDate(new Date(0));
    validateSerializedForm(hc, COMPARATOR, "serializedforms/HolidayCalendarSerializationTest.testOneDayExcluded.ser");
  }
  
  @Test
  public void testTwoDaysExcluded() throws IOException, ClassNotFoundException {
    HolidayCalendar hc = new HolidayCalendar(new SimplisticTimeZone("Terra Australis"));
    hc.addExcludedDate(new Date(378443700000L));
    hc.addExcludedDate(new Date(0));
    validateSerializedForm(hc, COMPARATOR, "serializedforms/HolidayCalendarSerializationTest.testTwoDaysExcluded.ser");
  }
  
  @Test
  public void testExtendedProperties() throws IOException, ClassNotFoundException {
    HolidayCalendar wc = new HolidayCalendar(new HolidayCalendar(), new SimplisticTimeZone("Terra Australis"));
    wc.setDescription("A Calendar");
    validateSerializedForm(wc, COMPARATOR, "serializedforms/HolidayCalendarSerializationTest.testExtendedProperties.ser");
  }
}
