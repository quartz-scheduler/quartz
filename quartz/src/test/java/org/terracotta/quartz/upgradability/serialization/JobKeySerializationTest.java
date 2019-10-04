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
import org.junit.Test;
import org.quartz.JobKey;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class JobKeySerializationTest {

  @Test
  public void testWithoutGroup() throws IOException, ClassNotFoundException {
    JobKey key = new JobKey("foo");
    validateSerializedForm(key, "serializedforms/JobKeySerializationTest.testWithoutGroup.ser");
  }
  
  @Test
  public void testWithGroup() throws IOException, ClassNotFoundException {
    JobKey key = new JobKey("foo", "bar");
    validateSerializedForm(key, "serializedforms/JobKeySerializationTest.testWithGroup.ser");
  }
}
