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

import static org.quartz.workflow.TestWorkflowFactory.SCHED_A;
import static org.quartz.workflow.TestWorkflowFactory.SCHED_B;
import static org.quartz.workflow.TestWorkflowFactory.schedulerA;
import static org.quartz.workflow.TestWorkflowFactory.schedulerB;
import static org.quartz.workflow.TestWorkflowFactory.waitExtraTimeToCheckThatNoOtherJobsAreStarted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;

@SuppressWarnings("null")
public class WorkflowTest {
    TestWorkflowFactory testFactory = new TestWorkflowFactory();
    private final Workflow workflow = testFactory.workflow;

    private final List<JobDetailWithPreviousJobKey> executedTestJobs = testFactory.executedTestJobs;

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
        testFactory.setup();
    }

    @After
    public void reset() {
        testFactory.reset();
    }

    private void waitUntilConnectedJobsAreDone(final int jobCount) throws InterruptedException {
        testFactory.waitUntilConnectedJobsAreDone(jobCount);
    }



    private void waitUntilAllJobsAreUnscheduled() throws InterruptedException {
        testFactory.waitUntilAllJobsAreUnscheduled();
    }

    @Test
    public void startJobsDoNotHaveInputParameters() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);
        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));

        workflow.start();

        waitUntilConnectedJobsAreDone(1);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1);
        assertThat(executedTestJobs.get(0).previousJobKey()).isNull();
    }

    @Test
    public void runsFollowingJob_whenJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);
        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);
        final WorkflowRule rule = WorkflowRule.with(followingJob1);
        workflow.addRule(job1, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob1);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void followingJobCanBeAssignedInputParameters() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);
        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);
        final WorkflowRule rule = WorkflowRule.with(followingJob1);
        workflow.addRule(job1, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob1);
        assertThat(executedTestJobs.get(1).previousJobKey()).isEqualTo(job1.getKey());
    }

    @Test
    public void startsTriggerOnWorkflowStart() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").storeDurably().build();
        schedulerA.addJob(job1, false);
        workflow.addScheduler(schedulerA);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(job1).startNow().build();
        workflow.addStartRule(WorkflowRule.with(SCHED_A, trigger));

        workflow.start();

        waitUntilConnectedJobsAreDone(1);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1);
        assertThat(executedTestJobs.get(0).previousJobKey()).isNull();
        assertThat(schedulerA.deleteJob(job1.getKey())).isTrue();
    }

    @Test
    public void runsFollowingJob_whenAllJobsAreDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerA, job2);

        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_A));

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        final WorkflowRule rule =  WorkflowRule.when(group1Matcher)
                .applying(WorkflowRule.with(followingJob1));
        workflow.addRule(job1, rule);
        workflow.addRule(job2, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(3);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.get(2).jobDetail()).isEqualTo(followingJob1);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void runsAllJobsFollowingGroup_whenJobIsDone() throws Exception {
        workflow.setDefaultScheduler(schedulerA);
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(job1)
        .addStartRule(WorkflowRule.with(job1));

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(followingJob2);

        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");
        final WorkflowRule rule = WorkflowRule.with(group2Matcher).onSameScheduler();

        workflow.addRule(job1, rule);


        workflow.start();

        waitUntilConnectedJobsAreDone(3);
        assertThat(executedTestJobs.get(0).jobDetail()).isEqualTo(job1);
        assertThat(executedTestJobs.subList(1, 3)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void firesMultipleRules_whenJobIsDone() throws Exception {
        workflow.setDefaultScheduler(schedulerA);
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(job1)
        .addStartRule(WorkflowRule.with(job1));

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(followingJob2);

        workflow.addRule(job1, WorkflowRule.with(followingJob1))
        .addRule(job1, WorkflowRule.with(followingJob2));

        workflow.start();

        waitUntilConnectedJobsAreDone(3);
        assertThat(executedTestJobs.get(0).jobDetail()).isEqualTo(job1);
        assertThat(executedTestJobs.subList(1, 3)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void runsAllJobsFollowingGroup_whenAllJobsAreDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerA, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob2);

        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_A));

        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");
        final WorkflowRule rule =  WorkflowRule.when(group1Matcher).applying(WorkflowRule.with(group2Matcher));

        workflow.addRule(job1, rule)
        .addRule(job2, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(4);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.subList(2, 4)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void runsSingleJobsInNamedSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerB, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerB, followingJob2);

        final WorkflowRule rule =  WorkflowRule.when(job1).onScheduler(SCHED_A)
                .and(WorkflowRule.when(job2).onScheduler(SCHED_B))
                .applying(WorkflowRule.with(followingJob1).onScheduler(SCHED_A)
                        .with(WorkflowRule.with(followingJob2).onScheduler(SCHED_B)));

        workflow.addRule(job1, rule).addRule(job2, rule);

        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A))
        .start();
        workflow.addStartRule(WorkflowRule.with(job2).onScheduler(SCHED_B))
        .start();

        waitUntilConnectedJobsAreDone(4);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.subList(2, 4)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }


    @Test
    public void runsSingleJobsInSameScheduler() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerA, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob2);

        final WorkflowRule rule =  WorkflowRule.when(job1)
                .and(WorkflowRule.when(job2))
                .applying(WorkflowRule.with(followingJob1)
                        .with(WorkflowRule.with(followingJob2)));

        workflow.addRule(job1, rule).addRule(job2, rule);

        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));
        workflow.start();
        workflow.addStartRule(WorkflowRule.with(job2).onScheduler(SCHED_A));
        workflow.start();

        waitUntilConnectedJobsAreDone(4);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.subList(2, 4)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }


    @Test
    public void runsJobsUsingAnotherCombinedConditionsBuilder() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerA, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob2);

        final WorkflowRule rule =
                WorkflowRule.with(followingJob1)
                .with(WorkflowRule.with(followingJob2))
                .when(WorkflowRule.when(job1))
                .when(WorkflowRule.when(job2));

        workflow.addRule(job1, rule).addRule(job2, rule);

        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));
        workflow.addStartRule(WorkflowRule.with(job2).onScheduler(SCHED_A));
        workflow.start();

        waitUntilConnectedJobsAreDone(4);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.subList(2, 4)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }
    @Test
    public void changesSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);
        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerB, followingJob1);

        final WorkflowRule rule =   WorkflowRule.when(job1).onScheduler(SCHED_A)
                .applying(WorkflowRule.with(followingJob1).onScheduler(SCHED_B));

        workflow.addRule(job1, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob1);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void runsGroupJobsInSpecifiedSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerB, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerB, followingJob2);

        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");

        final WorkflowRule rule =
                WorkflowRule.when(group1Matcher).onScheduler(SCHED_A)
                .and(WorkflowRule.when(group1Matcher).onScheduler(SCHED_B))
                .applying(WorkflowRule.with(group2Matcher).onScheduler(SCHED_A)
                        .with(WorkflowRule.with(group2Matcher).onScheduler(SCHED_B)));

        workflow.addRule(job1, rule);
        workflow.addRule(job2, rule);

        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_A));
        workflow.start();
        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_B));
        workflow.start();

        waitUntilConnectedJobsAreDone(4);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.subList(2, 4)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void runsGroupJobsOnAllSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerB, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerB, followingJob2);

        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");

        final WorkflowRule rule =
                WorkflowRule.when(group1Matcher).onScheduler(SCHED_A)
                .and(WorkflowRule.when(group1Matcher).onScheduler(SCHED_B))
                .applying(WorkflowRule.with(group2Matcher).onAllSchedulers());

        workflow.addRule(job1, rule);
        workflow.addRule(job2, rule);

        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_A));
        workflow.start();
        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_B));
        workflow.start();

        waitUntilConnectedJobsAreDone(4);
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.subList(2, 4)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(followingJob1, followingJob2);
        waitUntilAllJobsAreUnscheduled();
        assertThat(schedulerA.getJobGroupNames()).doesNotContain("jobGroup1", "jobGroup2");
    }

    @Test
    public void runsGroupJobsOnAllOtherSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerB, job2);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(schedulerB, followingJob2);

        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");

        final WorkflowRule rule =
                WorkflowRule.when(group1Matcher).onScheduler(SCHED_A)
                .and(WorkflowRule.when(group1Matcher).onScheduler(SCHED_B))
                .applying(WorkflowRule.with(group2Matcher).onAllSchedulersExcept(SCHED_A));

        workflow.addRule(job1, rule);
        workflow.addRule(job2, rule);

        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_A));
        workflow.start();
        workflow.addStartRule(WorkflowRule.with(group1Matcher).onScheduler(SCHED_B));
        workflow.start();

        waitUntilConnectedJobsAreDone(3);
        waitExtraTimeToCheckThatNoOtherJobsAreStarted();
        assertThat(executedTestJobs.subList(0, 2)).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job1, job2);
        assertThat(executedTestJobs.get(2).jobDetail()).isEqualTo(followingJob2);
        assertThat(schedulerA.deleteJob(followingJob1.getKey())).isTrue();
    }

    @Test
    public void runsFollowingJob_whenTriggerIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").storeDurably().build();
        workflow.addJob(schedulerA, job1);

        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(schedulerA, followingJob1);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(job1).startNow().build();
        workflow.addStartRule(WorkflowRule.with(SCHED_A, trigger));

        final WorkflowRule rule = WorkflowRule.with(followingJob1);
        workflow.addRule(trigger, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob1);

        assertThat(schedulerA.deleteJob(job1.getKey())).isTrue();
    }


    @Test
    public void schedulesFollowingTrigger_whenJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").storeDurably().build();
        workflow.addJob(schedulerA, job1);

        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));


        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2")
                .storeDurably().build();
        workflow.addJob(schedulerA, followingJob1);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(followingJob1).startNow().build();
        final WorkflowRule rule = WorkflowRule.with(trigger);
        workflow.addRule(job1, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob1);
        assertThat(executedTestJobs.get(1).previousJobKey()).isEqualTo(job1.getKey());

        assertThat(schedulerA.deleteJob(job1.getKey())).isTrue();
        assertThat(schedulerA.deleteJob(followingJob1.getKey())).isTrue();
    }


    @Test
    public void schedulesFollowingTriggerOnAnotherScheduler_whenJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").storeDurably().build();
        workflow.addJob(schedulerA, job1);

        workflow.addStartRule(WorkflowRule.with(job1).onScheduler(SCHED_A));


        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2")
                .storeDurably().build();
        workflow.addJob(schedulerB, followingJob1);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(followingJob1).startNow().build();
        final WorkflowRule rule = WorkflowRule.with(SCHED_B, trigger);
        workflow.addRule(job1, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(2);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(job1, followingJob1);

        assertThat(schedulerA.deleteJob(job1.getKey())).isTrue();
        assertThat(schedulerB.deleteJob(followingJob1.getKey())).isTrue();
    }

    @Test
    public void runsFollowingJob_eachTimeWhenTriggerIsDone() throws Exception {
        JobDetail job = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job", "jobGroup1").storeDurably().build();
        workflow.addJob(schedulerA, job);

        JobDetail followingJob = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob", "jobGroup2").storeDurably().build();
        workflow.addJob(schedulerA, followingJob);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(job)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(1)
                        .withIntervalInMilliseconds(1000))
                .build();
        workflow.addStartRule(WorkflowRule.with(SCHED_A, trigger));

        final WorkflowRule rule = WorkflowRule.with(followingJob);
        workflow.addRule(trigger, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(3);
        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactlyInAnyOrder(job, job, followingJob);

        assertThat(schedulerA.deleteJob(job.getKey())).isTrue();
        assertThat(schedulerA.deleteJob(followingJob.getKey())).isTrue();
    }

    @Test
    public void thrownsExceptionIfStartRuleContainsUnknownScheduler() throws Exception {

        final JobKey jobKey = new JobKey("job1", "jobGroup1");
        assertThatThrownBy(() -> workflow.addStartRule(WorkflowRule.with(jobKey).onScheduler(SCHED_A)))
        .isInstanceOf(SchedulerException.class)
        .hasMessageContaining("Unknown scheduler A");
    }


    @Test
    public void thrownsExceptionIfJobRuleContainsUnknownScheduler() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();

        assertThatThrownBy(() -> workflow.addRule(job1, WorkflowRule.with(job1).onScheduler(SCHED_A)))
        .isInstanceOf(SchedulerException.class)
        .hasMessageContaining("Unknown scheduler A");
    }

    @Test
    public void thrownsExceptionIfTriggerRuleContainsUnknownScheduler() throws Exception {
        final Trigger trigger = TriggerBuilder.newTrigger().build();

        assertThatThrownBy(() -> workflow.addRule(trigger, WorkflowRule.with(SCHED_A, trigger)))
        .isInstanceOf(SchedulerException.class)
        .hasMessageContaining("Unknown scheduler A");
    }


    @Test
    public void deletesWorkflowJobs_whenSchedulerExceptionIsThrownOnStart() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerB, job2);

        schedulerB.addJob(job2, false, true);

        assertThatThrownBy(() -> workflow.start()).isInstanceOf(ObjectAlreadyExistsException.class);

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).isEmpty();
        assertThat(schedulerA.checkExists(job1.getKey())).isFalse();
        assertThat(schedulerB.deleteJob(job2.getKey())).isTrue();
    }


    @Test
    public void cancelsStart_whenSchedulerExceptionIsThrownOnStart() throws Exception {
        JobDetail job1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(schedulerA, job1);
        schedulerA.addJob(job1, false, true);

        JobDetail job2 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(schedulerB, job2);


        assertThatThrownBy(() -> workflow.start()).isInstanceOf(ObjectAlreadyExistsException.class);

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).isEmpty();
        assertThat(schedulerA.deleteJob(job1.getKey())).isTrue();
        assertThat(schedulerB.checkExists(job1.getKey())).isFalse();
    }

    @Test
    public void schedulesFollowingTrigger_afterJobRecoveryAttempts() throws Exception {
        JobDetail failingJob = JobBuilder.newJob(FailingTestJob.class)
                .withIdentity("job1", "jobGroup1").storeDurably().build();
        workflow.addJob(schedulerA, failingJob);

        workflow.addStartRule(WorkflowRule.with(failingJob).onScheduler(SCHED_A));


        JobDetail followingJob1 = JobBuilder.newJob(ConnectedTestJob.class)
                .withIdentity("followingJob1", "jobGroup2")
                .storeDurably().build();
        workflow.addJob(schedulerA, followingJob1);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(followingJob1).startNow().build();
        final WorkflowRule rule = WorkflowRule.with(trigger);
        workflow.addRule(failingJob, rule);

        workflow.start();

        waitUntilConnectedJobsAreDone(1);

        waitExtraTimeToCheckThatNoOtherJobsAreStarted();

        assertThat(executedTestJobs).extracting(JobDetailWithPreviousJobKey::jobDetail).containsExactly(failingJob, failingJob, failingJob, followingJob1);

        assertThat(schedulerA.deleteJob(failingJob.getKey())).isTrue();
        assertThat(schedulerA.deleteJob(followingJob1.getKey())).isTrue();
    }

    @Test
    public void runsRunnableGivenAsStartRule() throws Exception {
        Runnable runnable = mock(Runnable.class);
        workflow.addStartRule(runnable);
        workflow.start();

        verify(runnable).run();
    }
}
