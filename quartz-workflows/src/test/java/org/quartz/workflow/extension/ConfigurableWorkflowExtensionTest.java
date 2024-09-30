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

import static org.quartz.workflow.TestWorkflowFactory.SCHED_A;
import static org.quartz.workflow.TestWorkflowFactory.schedulerA;
import static org.quartz.workflow.TestWorkflowFactory.schedulerB;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.workflow.ConditionalRule;
import org.quartz.workflow.Workflow;
import org.quartz.workflow.WorkflowRule;

import org.quartz.workflow.ConnectedTestJob;
import org.quartz.workflow.JobDetailWithPreviousJobKey;
import org.quartz.workflow.TestWorkflowFactory;


public class ConfigurableWorkflowExtensionTest {
    TestWorkflowFactory testWorkflowFactory = new TestWorkflowFactory();
    private final Workflow initialWorkflow = testWorkflowFactory.workflow;
    private final List<JobDetailWithPreviousJobKey> executedTestJobs = testWorkflowFactory.executedTestJobs;

    @BeforeClass
    public static void startScheduler() throws SchedulerException, IOException {
        TestWorkflowFactory.startScheduler();
    }

    @AfterClass
    public static void stopScheduler() throws SchedulerException {
        TestWorkflowFactory.stopScheduler();
    }

    @Before
    public void setup() throws SchedulerException {
        testWorkflowFactory.setup();
    }

    @After
    public void reset() {
        testWorkflowFactory.reset();
    }

    private void waitUntilConnectedJobsAreDone(final int jobCount) throws InterruptedException {
        testWorkflowFactory.waitUntilConnectedJobsAreDone(jobCount);
    }


    @Test
    public void runsFollowingExtensionJobOnDefaultScheduler_whenJobIsDone() throws Exception {
        initialWorkflow.setDefaultScheduler(schedulerA);
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        initialWorkflow.addJob(job1);
        initialWorkflow.addStartRule(WorkflowRule.with(job1.getKey()));

        ConfigurableWorkflowExtension uut1 = new ConfigurableWorkflowExtension(initialWorkflow, "extension1");
        uut1.scheduleAfter(job1);

        JobDetail followingJob = JobBuilder.newJob().ofType(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();

        uut1.addExtensionJob("label", followingJob);

        initialWorkflow.start();

        waitUntilConnectedJobsAreDone(2);

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob);
    }

    @Test
    public void runsFollowingExtensionJobOnDifferentScheduler_whenJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        initialWorkflow.addJob(schedulerA, job1);
        initialWorkflow.addStartRule(WorkflowRule.with(job1.getKey()).onScheduler(SCHED_A));

        ConfigurableWorkflowExtension uut1 = new ConfigurableWorkflowExtension(initialWorkflow, "extension1");
        uut1.scheduleAfter(job1);

        JobDetail followingJob = JobBuilder.newJob().ofType(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();

        uut1.addExtensionJob("label", schedulerB, followingJob);

        initialWorkflow.start();

        waitUntilConnectedJobsAreDone(2);

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob);
    }


    @Test
    public void runsFollowingExtensionJobsOnDefaultScheduler_whenJobIsDone() throws Exception {
        initialWorkflow.setDefaultScheduler(schedulerA);
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        initialWorkflow.addJob(job1);
        initialWorkflow.addStartRule(WorkflowRule.with(job1.getKey()));

        JobDetail job2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job2", "jobGroup1").build();
        initialWorkflow.addJob(schedulerA, job2);
        initialWorkflow.addStartRule(WorkflowRule.with(job2.getKey()));

        ConfigurableWorkflowExtension uut1 = new ConfigurableWorkflowExtension(initialWorkflow, "extension1");
        uut1.scheduleAfter(job1);
        ConfigurableWorkflowExtension uut2 = new ConfigurableWorkflowExtension(initialWorkflow, "extension2");
        uut2.scheduleAfter(job2);

        JobDetail extension1 = uut1.addSynchronizationJob("followingJob1");
        JobDetail extension2 = uut2.addSynchronizationJob("followingJob1");

        JobDetail followingJob = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        initialWorkflow.addJob(followingJob);

        ConditionalRule whenBothJobsAreDoneRunFollowingJob =
                WorkflowRule.when(extension1)
                .and(WorkflowRule.when(extension2))
                .applying(WorkflowRule.with(followingJob));
        initialWorkflow.addRule(extension1, whenBothJobsAreDoneRunFollowingJob);
        initialWorkflow.addRule(extension2, whenBothJobsAreDoneRunFollowingJob);

        initialWorkflow.start();

        waitUntilConnectedJobsAreDone(3);

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2, followingJob);
    }

    @Test
    public void runsFollowingExtensionJobs_whenJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        initialWorkflow.addJob(schedulerA, job1);
        initialWorkflow.addStartRule(WorkflowRule.with(job1.getKey()).onScheduler(SCHED_A));

        JobDetail job2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job2", "jobGroup1").build();
        initialWorkflow.addJob(schedulerA, job2);
        initialWorkflow.addStartRule(WorkflowRule.with(job2.getKey()).onScheduler(SCHED_A));

        ConfigurableWorkflowExtension uut1 = new ConfigurableWorkflowExtension(initialWorkflow, "extension1");
        uut1.scheduleAfter(job1);
        ConfigurableWorkflowExtension uut2 = new ConfigurableWorkflowExtension(initialWorkflow, "extension2");
        uut2.scheduleAfter(job2);

        JobDetail extension1 = uut1.addSynchronizationJob("followingJob1", schedulerA);
        JobDetail extension2 = uut2.addSynchronizationJob("followingJob1", schedulerA);

        JobDetail followingJob = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        initialWorkflow.addJob(schedulerA, followingJob);

        ConditionalRule whenBothJobsAreDoneRunFollowingJob =
                WorkflowRule.when(extension1)
                .and(WorkflowRule.when(extension2))
                .applying(WorkflowRule.with(followingJob.getKey()));
        initialWorkflow.addRule(extension1, whenBothJobsAreDoneRunFollowingJob);
        initialWorkflow.addRule(extension2, whenBothJobsAreDoneRunFollowingJob);

        initialWorkflow.start();

        waitUntilConnectedJobsAreDone(3);

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2, followingJob);
    }

    @Test
    public void runsFollowingExtensionJob_forFinishedWorkflow() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        initialWorkflow.addJob(schedulerA, job1);
        initialWorkflow.addStartRule(WorkflowRule.with(job1.getKey()).onScheduler(SCHED_A));

        JobDetail followingJob = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();

        Workflow newWorkflow = new Workflow();
        initialWorkflow.start();
        waitUntilConnectedJobsAreDone(1);

        ConfigurableWorkflowExtension uut = new ConfigurableWorkflowExtension(newWorkflow, "extensionId");
        uut.scheduleAtWorkflowStart();
        uut.addExtensionJob("label", schedulerA, followingJob);

        newWorkflow.start();
        waitUntilConnectedJobsAreDone(1);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob);
    }

    @Test
    public void runsFollowingExtensionJob_whenJobTriggerIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        initialWorkflow.addJob(schedulerA, job1);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(job1).startNow().build();
        initialWorkflow.addStartRule(WorkflowRule.with(SCHED_A, trigger));

        JobDetail followingJob = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();

        ConfigurableWorkflowExtension uut = new ConfigurableWorkflowExtension(initialWorkflow, "extensionId");
        uut.scheduleAfter(trigger);
        uut.addExtensionJob("label", schedulerA, followingJob);

        initialWorkflow.start();
        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob);
    }
}
