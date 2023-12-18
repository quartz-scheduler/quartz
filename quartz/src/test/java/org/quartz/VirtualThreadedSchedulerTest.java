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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.JdbcQuartzTestUtilities;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.simpl.SimpleVirtualThreadPool;
import org.quartz.spi.JobStore;

import java.sql.SQLException;

public class VirtualThreadedSchedulerTest extends AbstractSchedulerTest {

    @BeforeClass
    public static void runOnlyWhenVThreadAvailable(){
        Assume.assumeTrue("Virtual Threading is not available!",SimpleVirtualThreadPool.isVirtualThreadSupported() );
    }

    @Override
    protected Scheduler createScheduler(String name, int threadPoolSize) throws SchedulerException {
        final String schedulerName = name + "VScheduler" ;
        DirectSchedulerFactory.getInstance().createScheduler(schedulerName, "AUTO", new SimpleVirtualThreadPool(), new RAMJobStore());
        return SchedulerRepository.getInstance().lookup(schedulerName);
    }
}
