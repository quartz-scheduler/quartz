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

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.Date;

import org.junit.Test;
import org.quartz.TriggerKey;
import org.terracotta.quartz.collections.TimeTrigger;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class TimeTriggerSerializationTest {
  
  private static final Comparator<TimeTrigger> COMPARATOR = new Comparator<TimeTrigger>() {
    @Override
    public int compare(TimeTrigger o1, TimeTrigger o2) {
      return o1.compareTo(o2);
    }
  };
  
  @Test
  public void testSimple() throws Exception {
    //use reflection here to avoid having to publicise the currently package protected constructor
    Constructor<TimeTrigger> cons = TimeTrigger.class.getDeclaredConstructor(TriggerKey.class, Date.class, Integer.TYPE);
    cons.setAccessible(true);
    TimeTrigger tt = cons.newInstance(new TriggerKey("name", "group"), new Date(10), 2);
    validateSerializedForm(tt, COMPARATOR, "serializedforms/TimeTriggerSerializationTest.testSimple.ser");
  }
}
