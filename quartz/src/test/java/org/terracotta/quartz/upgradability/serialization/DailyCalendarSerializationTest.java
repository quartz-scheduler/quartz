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

import org.junit.Test;
import org.quartz.impl.calendar.DailyCalendar;
import org.quartz.impl.calendar.HolidayCalendar;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class DailyCalendarSerializationTest {
  
  private static final Comparator<DailyCalendar> COMPARATOR = new Comparator<DailyCalendar>() {
    @Override
    public int compare(DailyCalendar o1, DailyCalendar o2) {
      return o1.getTimeRangeStartingTimeInMillis(0) == o2.getTimeRangeStartingTimeInMillis(0)
              && o1.getTimeRangeEndingTimeInMillis(0) == o2.getTimeRangeEndingTimeInMillis(0)
              && o1.getInvertTimeRange() == o2.getInvertTimeRange()
              && (o1.getBaseCalendar() != null) == (o2.getBaseCalendar() != null)
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && nullSafeEquals(o1.getTimeZone(), o2.getTimeZone()) ? 0 : -1;
    }
  };
  
  @Test
  public void testWithoutBaseCalendar() throws IOException, ClassNotFoundException {
    DailyCalendar dc = new DailyCalendar(3, 4, 5, 6, 7, 8, 9, 10);
    validateSerializedForm(dc, COMPARATOR, "serializedforms/DailyCalendarSerializationTest.testWithoutBaseCalendar.ser");
  }

  @Test
  public void testWithBaseCalendar() throws IOException, ClassNotFoundException {
    DailyCalendar dc = new DailyCalendar(new HolidayCalendar(), 3, 4, 5, 6, 7, 8, 9, 10);
    validateSerializedForm(dc, COMPARATOR, "serializedforms/DailyCalendarSerializationTest.testWithBaseCalendar.ser");
  }
  
  @Test
  public void testWithEverything() throws IOException, ClassNotFoundException {
    DailyCalendar dc = new DailyCalendar(new HolidayCalendar(), 3, 4, 5, 6, 7, 8, 9, 10);
    dc.setDescription("A Calendar");
    dc.setTimeZone(new SimplisticTimeZone("Terra Australis"));
    validateSerializedForm(dc, COMPARATOR, "serializedforms/DailyCalendarSerializationTest.testWithEverything.ser");
  }
}
