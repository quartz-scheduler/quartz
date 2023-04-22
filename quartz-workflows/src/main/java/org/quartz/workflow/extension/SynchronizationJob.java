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

package org.quartz.workflow.extension;

import java.util.UUID;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.workflow.JobAndTriggerFactory;

public class SynchronizationJob implements Job {

    public static JobBuilder newJob(String label) {
        return newJob(label, SynchronizationJob.class.getSimpleName());
    }

    public static JobBuilder newJob(String label, String group) {
        String name = label + "/" + UUID.randomUUID().toString();
        return JobAndTriggerFactory.newJob(name, group, SynchronizationJob.class);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {/**/}

}
