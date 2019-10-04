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
 */
package org.quartz.simpl;

import java.sql.SQLException;
import java.util.Properties;

import junit.framework.TestCase;

import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JdbcQuartzTestUtilities;

/**
 * Unit test for SystemPropertyInstanceIdGenerator.
 */
public class SystemPropertyInstanceIdGeneratorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    System.setProperty(SystemPropertyInstanceIdGenerator.SYSTEM_PROPERTY,
        "foo");
    System.setProperty("blah.blah",
    "goo");
  }

  public void testGetInstanceId() throws Exception {
    SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator();

    String instId = gen.generateInstanceId();

    assertEquals("foo", instId);
  }

  public void testGetInstanceIdWithPrepend() throws Exception {
    SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator();
    gen.setPrepend("1");

    String instId = gen.generateInstanceId();

    assertEquals("1foo", instId);
  }

  public void testGetInstanceIdWithPostpend() throws Exception {
    SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator();
    gen.setPostpend("2");

    String instId = gen.generateInstanceId();

    assertEquals("foo2", instId);
  }

  public void testGetInstanceIdWithPrependAndPostpend() throws Exception {
    SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator();
    gen.setPrepend("1");
    gen.setPostpend("2");

    String instId = gen.generateInstanceId();

    assertEquals("1foo2", instId);
  }

  public void testGetInstanceIdFromCustomSystemProperty() throws Exception {
    SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator();
    gen.setSystemPropertyName("blah.blah");

    String instId = gen.generateInstanceId();

    assertEquals("goo", instId);
  }

  public void testGeneratorThroughSchedulerInstatiation() throws Exception {
    try {
      JdbcQuartzTestUtilities.createDatabase("MeSchedulerDatabase");
    } catch (SQLException e) {
      throw new AssertionError(e);
    }

    Properties config = new Properties();
    config.setProperty("org.quartz.scheduler.instanceName", "MeScheduler");
    config.setProperty("org.quartz.scheduler.instanceId", "AUTO");
    config.setProperty("org.quartz.scheduler.instanceIdGenerator.class", 
        org.quartz.simpl.SystemPropertyInstanceIdGenerator.class.getName());
    config.setProperty("org.quartz.scheduler.instanceIdGenerator.prepend", "1");
    config.setProperty("org.quartz.scheduler.instanceIdGenerator.postpend", "2");
    config.setProperty("org.quartz.scheduler.instanceIdGenerator.systemPropertyName", "blah.blah");
    config.setProperty("org.quartz.threadPool.threadCount", "1");
    config.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    config.setProperty("org.quartz.jobStore.class", org.quartz.impl.jdbcjobstore.JobStoreTX.class.getName());
    config.setProperty("org.quartz.jobStore.isClustered", "true");
    config.setProperty("org.quartz.jobStore.dataSource", "MeSchedulerDatabase");
    
    Scheduler sched = new StdSchedulerFactory(config).getScheduler();    
    
    assertEquals("1goo2", sched.getSchedulerInstanceId());
  }
}
