/*
 * Copyright 2023 Dimitry Polivaev, Unite
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quartz.workflow;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.workflow.extension.SynchronizationJob;

public class JobAndTriggerFactoryTest {
    
    @Test
    public void createsJobBuilder() throws Exception {
        JobDetail job = JobAndTriggerFactory.newJob("jobName", SynchronizationJob.class).build();
        assertThat(job).isEqualToComparingFieldByFieldRecursively(
                JobBuilder.newJob(SynchronizationJob.class)
                .withIdentity("jobName", SynchronizationJob.class.getSimpleName())
                .requestRecovery().build()
        );
    } 
    
    @Test
    public void createsTrigger_givenJobKey() throws Exception {
        JobKey jobKey = new JobKey("jobName", "jobGroup");
        Trigger trigger = JobAndTriggerFactory.newTrigger(jobKey).build();
        assertThat(trigger).isEqualToComparingFieldByFieldRecursively(
                TriggerBuilder.newTrigger()
                .withIdentity("jobName", "jobGroup")
                .forJob(jobKey)
                .startAt(trigger.getStartTime())
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().
                            withMisfireHandlingInstructionIgnoreMisfires()
                        )
                .build());
    }

    
    @Test
    public void createsTrigger_givenJobDetail() throws Exception {
        JobDetail job = JobBuilder.newJob(SynchronizationJob.class)
                .withIdentity("jobName", "jobGroup").build();
        Trigger trigger = JobAndTriggerFactory.newTrigger(job).build();
        assertThat(trigger).isEqualToComparingFieldByFieldRecursively(
                TriggerBuilder.newTrigger()
                .withIdentity("jobName", "jobGroup")
                .forJob(job)
                .startAt(trigger.getStartTime())
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().
                            withMisfireHandlingInstructionIgnoreMisfires()
                        )
                .build());
    }
}
