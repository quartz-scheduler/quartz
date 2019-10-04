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
import java.util.Collections;
import java.util.Comparator;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.impl.JobDetailImpl;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class JobDetailImplSerializationTest {

  private static final Comparator<JobDetailImpl> COMPARATOR = new Comparator<JobDetailImpl>() {

    @Override
    public int compare(JobDetailImpl o1, JobDetailImpl o2) {
      return o1.getName().equals(o2.getName()) && o1.getGroup().equals(o2.getGroup())
              && ((o1.getDescription() == null && o2.getDescription() == null) || o1.getDescription().equals(o2.getDescription()))
              && o1.getJobClass().equals(o2.getJobClass())
              && o1.getJobDataMap().equals(o2.getJobDataMap())
              && o1.isDurable() == o2.isDurable()
              && o1.requestsRecovery() == o2.requestsRecovery() 
              ? 0 : -1;
    }
  };
  
  @Test
  public void testMinimal() throws IOException, ClassNotFoundException {
    JobDetailImpl jdi = new JobDetailImpl();
    jdi.setName("foo");
    jdi.setGroup("bar");
    jdi.setJobClass(Job.class);
    validateSerializedForm(jdi, COMPARATOR, "serializedforms/JobDetailImplSerializationTest.testMinimal.ser");
  }
  
  @Test
  public void testComplex() throws IOException, ClassNotFoundException {
    JobDetailImpl jdi = new JobDetailImpl();
    jdi.setName("foo");
    jdi.setGroup("bar");
    jdi.setJobClass(Job.class);
    
    jdi.setDescription("My really fancy and complicated job");
    jdi.setJobDataMap(new JobDataMap(Collections.singletonMap("foo", "bar")));
    jdi.setDurability(true);
    jdi.setRequestsRecovery(true);
    
    validateSerializedForm(jdi, COMPARATOR, "serializedforms/JobDetailImplSerializationTest.testComplex.ser");
  }
}
