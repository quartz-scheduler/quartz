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

import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

class FailingTestJob implements Job{
    private final List<JobDetailWithPreviousJobKey> executedTestJobs;
    
    FailingTestJob(List<JobDetailWithPreviousJobKey> executedTestJobs) {
        super();
        this.executedTestJobs = executedTestJobs;
    }

    @Override
    public void execute(JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        executedTestJobs.add(new JobDetailWithPreviousJobKey(jobDetail, (JobKey) JobData.getJobInputData(context)));
        try {
            new JobRecovery(context, 2, 0).start();
        }
        catch(AllRecoveryAttemptsFailedException e) {/**/}
        catch(JobExecutionException e) {
            fail("scheduler exception", e);
        }
    }
}