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
import org.quartz.JobDataMap;

import static org.terracotta.quartz.upgradability.serialization.Utilities.expand;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class JobDataMapSerializationTest {

  private static final Comparator<JobDataMap> COMPARATOR = new Comparator<JobDataMap>() {

    @Override
    public int compare(JobDataMap o1, JobDataMap o2) {
      return o1.equals(o2) && o1.isDirty() == o2.isDirty() && o1.getAllowsTransientData() == o2.getAllowsTransientData() ? 0 : -1;
    }
  };
  
  @Test
  public void testEmptyMap() throws IOException, ClassNotFoundException {
    JobDataMap jdm = new JobDataMap();
    validateSerializedForm(jdm, COMPARATOR, expand("serializedforms/JobDataMapSerializationTest.testEmptyMap.{?}.ser", "JDK16", "JDK17", "JDK18"));
  }
  
  @Test
  public void testEmptyAllowTransientsMap() throws IOException, ClassNotFoundException {
    JobDataMap jdm = new JobDataMap();
    jdm.setAllowsTransientData(true);
    validateSerializedForm(jdm, COMPARATOR, expand("serializedforms/JobDataMapSerializationTest.testEmptyAllowTransientsMap.{?}.ser", "JDK16", "JDK17", "JDK18"));
  }
  
  @Test
  public void testOccupiedDirtyMap() throws IOException, ClassNotFoundException {
    JobDataMap jdm = new JobDataMap();
    jdm.put("foo", "bar");
    validateSerializedForm(jdm, COMPARATOR, "serializedforms/JobDataMapSerializationTest.testOccupiedDirtyMap.ser");
  }
  
  @Test
  public void testOccupiedCleanMap() throws IOException, ClassNotFoundException {
    JobDataMap jdm = new JobDataMap();
    jdm.put("foo", "bar");
    jdm.clearDirtyFlag();
    validateSerializedForm(jdm, COMPARATOR, "serializedforms/JobDataMapSerializationTest.testOccupiedCleanMap.ser");
  }
}
