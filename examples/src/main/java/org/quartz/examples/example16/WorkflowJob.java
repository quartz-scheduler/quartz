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
 *
 */

package org.quartz.examples.example16;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.workflow.JobData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is just a simple job that says "Hello" to the world.
 * </p>
 *
 * @author Dimitry Polivaev
 */
public class WorkflowJob implements Job {
    private static Logger _log = LoggerFactory.getLogger(WorkflowJob.class);

    private static final Semaphore semaphore  = new Semaphore(0);
    static void waitUntilWorkflowJobsAreDone(int count) throws InterruptedException {
        WorkflowJob.semaphore.acquire(count);
    }

    /**
     * <p>
     * Empty constructor for job initilization
     * </p>
     * <p>
     * Quartz requires a public empty constructor so that the
     * scheduler can instantiate the class whenever it needs.
     * </p>
     */
    public WorkflowJob() {
    }

    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a
     * <code>{@link org.quartz.Trigger}</code> fires that is associated with
     * the <code>Job</code>.
     * </p>
     *
     * @throws JobExecutionException
     *             if there is an exception while executing the job.
     */
    @Override
    public void execute(JobExecutionContext context)
        throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();

        // Say Hello to the World and display the date/time
        _log.info("Hello World! from " + jobKey + ", " + new Date());

        // Access and log job input data
        Object jobInputData = JobData.getJobInputData(context);
        if(jobInputData != null) {
            _log.info("Job input data: " + jobInputData);
        }

        // Save and log job output data
        HashMap<String, Serializable> jobOutputData = new HashMap<>();
        jobOutputData.put("lastJobKey", context.getJobDetail().getKey());
        jobOutputData.put("finishedJobCounter", Integer.valueOf(semaphore.availablePermits() + 1));
        _log.info("Job output data: " + jobOutputData);
        JobData.setJobOutputData(context,  jobOutputData);

        semaphore.release(1);
    }

}
