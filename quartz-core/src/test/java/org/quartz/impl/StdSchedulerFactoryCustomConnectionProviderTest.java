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
package org.quartz.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * TestCase to verify StdSchedulerFactory initializes correctly a custom ConnectionProvider
 * 
 * @author adahanne
 *
 */

public class StdSchedulerFactoryCustomConnectionProviderTest {

	@Test
	public void loadAndInitializeCustomConnectionProviderTest() throws SchedulerException, InterruptedException {
		StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/properties/quartzCustomConnectionProvider.properties");
		Scheduler scheduler = factory.getScheduler();
		try{
			scheduler.start();
		} catch(Exception e){
			//the mock connection provider throws a MockSQLException
			assertEquals("org.quartz.impl.MockSQLException",e.getCause().getCause().getClass().getName());
		}
		assertEquals("setCustomProperty(customValue)",MockConnectionProvider.methodsCalled.get(0));
		assertEquals("initialize",MockConnectionProvider.methodsCalled.get(1));
		assertEquals("getConnection",MockConnectionProvider.methodsCalled.get(2));
	}
	

}
