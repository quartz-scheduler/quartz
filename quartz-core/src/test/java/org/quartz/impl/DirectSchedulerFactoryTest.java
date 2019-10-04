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
package org.quartz.impl;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.quartz.Scheduler;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadPool;

public class DirectSchedulerFactoryTest extends TestCase {
    public void testPlugins() throws Exception {
        final StringBuffer result = new StringBuffer();
        
        SchedulerPlugin testPlugin = new SchedulerPlugin() {
            public void initialize(String name, org.quartz.Scheduler scheduler, ClassLoadHelper classLoadHelper) throws org.quartz.SchedulerException {
                result.append(name).append("|").append(scheduler.getSchedulerName());
            };
            public void start() {
                result.append("|start");
            };
            public void shutdown() {
                result.append("|shutdown");
            };
        };
        
        ThreadPool threadPool = new SimpleThreadPool(1, 5);
        threadPool.initialize();
        DirectSchedulerFactory.getInstance().createScheduler(
                "MyScheduler", "Instance1", threadPool,
                new RAMJobStore(), Collections.singletonMap("TestPlugin", testPlugin), 
                null, -1, 0, 0, false, null);
        
        Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler("MyScheduler");
        scheduler.start();
        scheduler.shutdown();
        
        assertEquals("TestPlugin|MyScheduler|start|shutdown", result.toString());
    }

    public void testThreadName() throws Throwable {
        DirectSchedulerFactory.getInstance().createVolatileScheduler(4);
        Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler();
        QuartzScheduler qs = getField(scheduler, "sched");
        QuartzSchedulerResources qsr = getField(qs, "resources");
        ThreadPool tp = qsr.getThreadPool();
        List<?> list = getField(tp,"workers");
        Object workerThread = list.get(0);
        String workerThreadName = workerThread.toString();
        assertFalse(workerThreadName.contains("null"));
        assertTrue(workerThreadName.contains(scheduler.getSchedulerName()));

    }

    <T> T getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        T result = (T)field.get(obj);
        return result;
    }
}
