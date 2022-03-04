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

import static org.hamcrest.CoreMatchers.containsString;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;



/**
 * DefaultSchedulerTest
 */
class DefaultSchedulerTest {

    @Test
    void testAddJobNoTrigger() throws Exception {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("testjob");

        try {
            scheduler.addJob(jobDetail, false);
        } catch (SchedulerException e) {
            MatcherAssert.assertThat(e.getMessage(), containsString("durable"));
        }

        jobDetail.setDurability(true);
        scheduler.addJob(jobDetail, false);
    }
}
