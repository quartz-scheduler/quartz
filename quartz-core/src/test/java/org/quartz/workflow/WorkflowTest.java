/*
 * Created on Jan 26, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;


import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.TriggerFiredBundle;

@SuppressWarnings("null")
public class WorkflowTest {
    
    class ConnectedTestJob implements Job{
        @Override
        public void execute(JobExecutionContext context) {
            capturedDetails.add(context.getJobDetail());
            semaphore.release();
        }
    }


    private static Scheduler schedulerA;
    private static Scheduler schedulerB;

    private Workflow workflow;
    private Semaphore semaphore;
    
    private List<JobDetail> capturedDetails;
    @BeforeClass
    public static void startScheduler() throws SchedulerException, IOException {
        Properties configA = loadDefaultSchedulerConfiguration();
        Properties configB = loadDefaultSchedulerConfiguration();
        

        configA.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "A");
        configB.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "B");

        
        schedulerA = new StdSchedulerFactory(configA).getScheduler();
        schedulerB = new StdSchedulerFactory(configB).getScheduler();
        schedulerA.start();
        schedulerB.start();
    }

    private static Properties loadDefaultSchedulerConfiguration() throws IOException {
        Properties configA = new Properties();
        try(InputStream defaultConfig = SchedulerFactory.class.getResourceAsStream("quartz.properties")){
            configA.load(defaultConfig);
        }
        return configA;
    }
    
    @AfterClass
    public static void stopScheduler() throws SchedulerException {
        schedulerA.shutdown();
        schedulerB.shutdown();
    }
    
    @Before
    public void setup() throws SchedulerException {
        schedulerA.setJobFactory(this::newConnectedTestJob);
        schedulerB.setJobFactory(this::newConnectedTestJob);
        workflow = new Workflow();
        semaphore = new Semaphore(0);
        capturedDetails = Collections.synchronizedList(new ArrayList<>());
    }
    
    @After
    public void unregisterWorkflow() throws SchedulerException {
        workflow.remove();
    }
    
    private Job newConnectedTestJob(TriggerFiredBundle bundle, @SuppressWarnings("unused") Scheduler scheduler){
        assertEquals(ConnectedTestJob.class, bundle.getJobDetail().getJobClass());
        return new ConnectedTestJob();
    }

    private void waitUntilJobsAreDone(final int jobCount) throws InterruptedException {
        semaphore.acquire(jobCount);
    }
    
    @Test
    public void runsSingleFollowingJob_whenCurrentJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        JobDetail followingJob1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        
        final WorkflowRule rule = new SingleJobRule(followingJob1.getKey());
        
        workflow.addJob(schedulerA, job1, rule);
        workflow.addJob(schedulerA, followingJob1);
        
        workflow.startJob(schedulerA, job1.getKey());
        
        waitUntilJobsAreDone(2);
        assertThat(capturedDetails, contains(job1, followingJob1));
        assertThat(schedulerA.getJobGroupNames(), not(contains("jobGroup1")));
    }
    
    @Test
    public void runsSingleFollowingJob_whenAllCurrentJobsAreDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        JobDetail job2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job2", "jobGroup1").build();
        JobDetail followingJob1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        
        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        
        final WorkflowRule rule = new ConditionalRule(group1Matcher,followingJob1.getKey());
        
        workflow.addJob(schedulerA, job1, rule);
        workflow.addJob(schedulerA, job2, rule);
        workflow.addJob(schedulerA, followingJob1);
        
        workflow.startJobs(schedulerA, group1Matcher);
        
        waitUntilJobsAreDone(3);
        assertThat(capturedDetails.subList(0, 2), containsInAnyOrder(job1, job2));
        assertThat(capturedDetails.get(2), equalTo(followingJob1));
        assertThat(schedulerA.getJobGroupNames(), not(contains("jobGroup1")));
    }
    
    @Test
    public void runsAllJobsFollowingGroup_whenCurrentJobIsDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        JobDetail followingJob1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        JobDetail followingJob2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob2", "jobGroup2").build();
        
        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");
        
        final WorkflowRule rule = new GroupRule(group2Matcher);
        
        workflow.addJob(schedulerA, job1, rule);
        workflow.addJob(schedulerA, followingJob1);
        workflow.addJob(schedulerA, followingJob2);
        
        workflow.startJob(schedulerA, job1.getKey());
        
        waitUntilJobsAreDone(3);
        assertThat(capturedDetails.get(0), equalTo(job1));
        assertThat(capturedDetails.subList(1, 3), containsInAnyOrder(followingJob1, followingJob2));
        assertThat(schedulerA.getJobGroupNames(), not(contains("jobGroup1")));
    }
    
    @Test
    public void runsAllJobsFollowingGroup_whenAllCurrentJobsAreDone() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        JobDetail job2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job2", "jobGroup1").build();
        JobDetail followingJob1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        JobDetail followingJob2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob2", "jobGroup2").build();
        
        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");
        
        final WorkflowRule rule = new ConditionalRule(group1Matcher,group2Matcher);
        
        workflow.addJob(schedulerA, job1, rule);
        workflow.addJob(schedulerA, job2, rule);
        workflow.addJob(schedulerA, followingJob1);
        workflow.addJob(schedulerA, followingJob2);
        
        workflow.startJobs(schedulerA, group1Matcher);
        
        waitUntilJobsAreDone(4);
        assertThat(capturedDetails.subList(0, 2), containsInAnyOrder(job1, job2));
        assertThat(capturedDetails.subList(2, 4), containsInAnyOrder(followingJob1, followingJob2));
        assertThat(schedulerA.getJobGroupNames(), not(contains("jobGroup1")));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void rejectsDurableJobs() throws Exception {
        final JobDetail job = JobBuilder.newJob().ofType(ConnectedTestJob.class).storeDurably().build();
        workflow.addJob(schedulerA, job);
    }
    @Test
    public void runsSingleJobsInNamedSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        JobDetail job2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job2", "jobGroup1").build();
        JobDetail followingJob1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        JobDetail followingJob2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob2", "jobGroup2").build();
        
        final WorkflowRule rule =  new ConditionalRule(
                WorkflowCondition.with("A", job1.getKey()).with("B", job2.getKey()),
                WorkflowRule.with("A", followingJob1.getKey()).with("B", followingJob2.getKey()));
        
        workflow.addJob(schedulerA, job1, rule);
        workflow.addJob(schedulerB, job2, rule);
        workflow.addJob(schedulerA, followingJob1);
        workflow.addJob(schedulerB, followingJob2);
        
        workflow.startJob(schedulerA, job1.getKey());
        workflow.startJob(schedulerB, job2.getKey());
        
        waitUntilJobsAreDone(4);
        assertThat(capturedDetails.subList(0, 2), containsInAnyOrder(job1, job2));
        assertThat(capturedDetails.subList(2, 4), containsInAnyOrder(followingJob1, followingJob2));
        assertThat(schedulerA.getJobGroupNames(), not(contains("jobGroup1")));
    }

    @Test
    public void runsGroupJobsInMultipleSchedulers() throws Exception {
        JobDetail job1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job1", "jobGroup1").build();
        JobDetail job2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("job2", "jobGroup1").build();
        JobDetail followingJob1 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob1", "jobGroup2").build();
        JobDetail followingJob2 = JobBuilder.newJob().ofType(ConnectedTestJob.class)
        .withIdentity("followingJob2", "jobGroup2").build();
        
        final GroupMatcher<JobKey> group1Matcher = GroupMatcher.groupEquals("jobGroup1");
        final GroupMatcher<JobKey> group2Matcher = GroupMatcher.groupEquals("jobGroup2");
        
        final WorkflowRule rule =  new ConditionalRule(
                WorkflowCondition.with("A", group1Matcher).with("B", group1Matcher),
                WorkflowRule.with("A", group2Matcher).with("B", group2Matcher));
        
        workflow.addJob(schedulerA, job1, rule);
        workflow.addJob(schedulerB, job2, rule);
        workflow.addJob(schedulerA, followingJob1);
        workflow.addJob(schedulerB, followingJob2);
        
        workflow.startJobs(schedulerA, group1Matcher);
        workflow.startJobs(schedulerB, group1Matcher);
        
        waitUntilJobsAreDone(4);
        assertThat(capturedDetails.subList(0, 2), containsInAnyOrder(job1, job2));
        assertThat(capturedDetails.subList(2, 4), containsInAnyOrder(followingJob1, followingJob2));
        assertThat(schedulerA.getJobGroupNames(), not(contains("jobGroup1")));
    }

  }
