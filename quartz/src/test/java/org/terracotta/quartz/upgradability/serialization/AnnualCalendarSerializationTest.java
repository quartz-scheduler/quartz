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
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

import org.junit.Test;
import org.quartz.impl.calendar.AnnualCalendar;
import org.quartz.impl.calendar.HolidayCalendar;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.MONTH;
import static org.terracotta.quartz.upgradability.serialization.Utilities.expand;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.nullSafeEquals;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class AnnualCalendarSerializationTest {
  
  private static final Comparator<AnnualCalendar> COMPARATOR = new Comparator<AnnualCalendar>() {

    @Override
    public int compare(AnnualCalendar o1, AnnualCalendar o2) {
      return o1.getDaysExcluded().equals(o2.getDaysExcluded()) 
              && nullSafeEquals(o1.getDescription(), o2.getDescription())
              && nullSafeEquals(o1.getTimeZone(), o2.getTimeZone())
              && ((o1.getBaseCalendar() == null) == (o2.getBaseCalendar() == null))
              ? 0 : -1;
    }
  };

  @Test
  public void testNoBaseSimple() throws IOException, ClassNotFoundException {
    AnnualCalendar ac = new AnnualCalendar();
    validateSerializedForm(ac, COMPARATOR, expand("serializedforms/AnnualCalendarSerializationTest.testNoBaseSimple.{?}.ser", "JDK16", "JDK17"));
  }

  @Test
  public void testNoBaseComplex() throws IOException, ClassNotFoundException {
    AnnualCalendar ac = new AnnualCalendar();
    Calendar exclude = Calendar.getInstance(new SimplisticTimeZone("Atlantis"), Locale.ROOT);
    exclude.clear();
    exclude.set(MONTH, DECEMBER);
    exclude.set(DAY_OF_MONTH, 29);
    ac.setDayExcluded(exclude, true);
    ac.setTimeZone(new SimplisticTimeZone("Terra Australis"));
    ac.setDescription("Annual Calendar");
    validateSerializedForm(ac, COMPARATOR, expand("serializedforms/AnnualCalendarSerializationTest.testNoBaseComplex.{?}.ser", "JDK16", "JDK17"));
  }

  @Test
  public void testBaseCalendarAndComplex() throws IOException, ClassNotFoundException {
    AnnualCalendar ac = new AnnualCalendar(new HolidayCalendar());
    Calendar exclude = Calendar.getInstance(new SimplisticTimeZone("Atlantis"), Locale.ROOT);
    exclude.clear();
    exclude.set(MONTH, DECEMBER);
    exclude.set(DAY_OF_MONTH, 29);
    ac.setDayExcluded(exclude, true);
    ac.setTimeZone(new SimplisticTimeZone("Terra Australis"));
    ac.setDescription("Annual Calendar");
    validateSerializedForm(ac, COMPARATOR, expand("serializedforms/AnnualCalendarSerializationTest.testBaseCalendarAndComplex.{?}.ser", "JDK16", "JDK17"));
  }  
}
