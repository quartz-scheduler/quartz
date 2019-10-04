/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */
 package org.terracotta.quartz.tests.spring;

import org.springframework.context.ApplicationContext;
import org.terracotta.quartz.tests.AbstractStandaloneTest;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class SimpleSpringTest extends AbstractStandaloneTest {

  public SimpleSpringTest(TestConfig testConfig) {
    super(testConfig, SimpleSpringClient1.class, SimpleSpringClient2.class);
    testConfig.getClientConfig().setParallelClients(false);
    timebombTest("2033-01-15");
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> rv = new ArrayList<String>();
    rv.add(TestBaseUtil.jarFor(ApplicationContext.class));
    rv.add(TestBaseUtil.jarFor(org.apache.commons.logging.LogFactory.class));
    return rv;
  }
}
