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
package org.quartz;

import java.util.Properties;

import org.quartz.impl.StdSchedulerFactory;

public class RAMSchedulerTest extends AbstractSchedulerTest {

    @Override
    protected Scheduler createScheduler(String name, int threadPoolSize) throws SchedulerException {
        Properties config = new Properties();
        config.setProperty("org.quartz.scheduler.instanceName", name + "Scheduler");
        config.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        config.setProperty("org.quartz.threadPool.threadCount", Integer.toString(threadPoolSize));
        config.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        return new StdSchedulerFactory(config).getScheduler();
    }
}
