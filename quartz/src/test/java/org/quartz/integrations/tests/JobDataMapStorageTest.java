/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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
package org.quartz.integrations.tests;


import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

import org.quartz.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

/**
 * Created by zemian on 10/25/16.
 */
public class JobDataMapStorageTest extends QuartzDerbyTestSupport {
    @Test
    void testJobDataMapDirtyFlag() throws Exception {
        JobDetail jobDetail = newJob(HelloJob.class)
                .withIdentity("test")
                .usingJobData("jfoo", "bar")
                .build();

        CronTrigger trigger = newTrigger()
                .withIdentity("test")
                .withSchedule(cronSchedule("0 0 0 * * ?"))
                .usingJobData("tfoo", "bar")
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        JobDetail storedJobDetail = scheduler.getJobDetail(JobKey.jobKey("test"));
        JobDataMap storedJobMap = storedJobDetail.getJobDataMap();
        assertThat(storedJobMap.isDirty(), is(false));

        Trigger storedTrigger = scheduler.getTrigger(triggerKey("test"));
        JobDataMap storedTriggerMap = storedTrigger.getJobDataMap();
        assertThat(storedTriggerMap.isDirty(), is(false));
    }
}
