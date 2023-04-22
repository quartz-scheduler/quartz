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

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class JobAndTriggerFactory{

    public static JobBuilder newJob(String jobName, Class<? extends Job> jobClass) {
        return newJob(jobName, jobClass.getSimpleName(), jobClass);
        
    }

    public static JobBuilder newJob(String jobName, String jobGroup, Class<? extends Job> jobClass) {
        return JobBuilder.newJob(jobClass)
        .withIdentity(jobName, jobGroup)
        .requestRecovery();
    }

    public static TriggerBuilder<? extends Trigger> newTrigger(JobDetail job) {
        return newTrigger(job.getKey());
    }

    public static TriggerBuilder<? extends Trigger> newTrigger(JobKey key) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(key.getName(), key.getGroup())
                .forJob(key)
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().
                            withMisfireHandlingInstructionIgnoreMisfires()
                        );
    }
}