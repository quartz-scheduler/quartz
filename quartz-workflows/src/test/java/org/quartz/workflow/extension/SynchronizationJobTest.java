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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.workflow.JobAndTriggerFactory;

public class SynchronizationJobTest {
@Test
public void createsSynchronizationJob() throws Exception {
    JobBuilder jobBuilder = SynchronizationJob.newJob("label");
    String jobName = jobBuilder.build().getKey().getName();
    assertThat(jobName).startsWith("label/");
    assertThat(jobBuilder)
        .isEqualToComparingFieldByFieldRecursively(
                JobAndTriggerFactory.newJob(jobName, SynchronizationJob.class));
}
}
