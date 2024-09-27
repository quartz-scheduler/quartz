/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.quartz.impl;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.helpers.NOPLogger;

public class StdSchedulerFactoryTest {

	@Test
	public void testOverrideSystemProperties() {
	    Properties p = new Properties();
	    p.setProperty("nonsense1", "hello1");
	    p.setProperty("nonsense2", "hello2");
	    System.setProperty("nonsense1", "boo1");
	    String osName = System.getProperty("os.name");
	    Properties q = StdSchedulerFactory.overrideWithSysProps(p, NOPLogger.NOP_LOGGER);
	    assertEquals("boo1", q.get("nonsense1"));
	    assertEquals(osName, q.get("os.name"));
	}
}
