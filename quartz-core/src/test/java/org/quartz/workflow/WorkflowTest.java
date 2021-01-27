/*
 * Created on Jan 26, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;


import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

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
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
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

    final static Trigger startNow = TriggerBuilder.newTrigger().withIdentity("triggerName", "triggerGroup")
            .startNow().build();

    @SuppressWarnings("null")
    static Scheduler scheduler;

    private Workflow workflow;
    private Semaphore semaphore;
    
    private List<JobDetail> capturedDetails;
    @BeforeClass
    public static void startScheduler() throws SchedulerException {
        SchedulerFactory sf = new StdSchedulerFactory();
        scheduler = sf.getScheduler();
        scheduler.start();
    }
    @AfterClass
    public static void stopScheduler() throws SchedulerException {
         scheduler.shutdown();
    }
    
    @Before
    public void setup() throws SchedulerException {
        scheduler.setJobFactory(this::newConnectedTestJob);
        workflow = new Workflow(scheduler);
        semaphore = new Semaphore(0);
        capturedDetails = Collections.synchronizedList(new ArrayList<>());
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
        
        workflow.addJob(job1, rule);
        workflow.addJob(followingJob1);
        
        workflow.startJob(job1.getKey());
        
        waitUntilJobsAreDone(2);
        assertThat(capturedDetails, containsInAnyOrder(job1, followingJob1));
        assertThat(scheduler.getJobGroupNames(), not(contains("jobGroup1")));
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
        
        final WorkflowRule rule = new SingleJobRule(group1Matcher,followingJob1.getKey());
        
        workflow.addJob(job1, rule);
        workflow.addJob(job2, rule);
        workflow.addJob(followingJob1);
        
        workflow.startJobs(group1Matcher);
        
        waitUntilJobsAreDone(3);
        assertThat(capturedDetails, containsInAnyOrder(job1, job2, followingJob1));
        assertThat(scheduler.getJobGroupNames(), not(contains("jobGroup1")));
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
        
        workflow.addJob(job1, rule);
        workflow.addJob(followingJob1);
        workflow.addJob(followingJob2);
        
        workflow.startJob(job1.getKey());
        
        waitUntilJobsAreDone(3);
        assertThat(capturedDetails, containsInAnyOrder(job1, followingJob1, followingJob2));
        assertThat(scheduler.getJobGroupNames(), not(contains("jobGroup1")));
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
        
        final WorkflowRule rule = new GroupRule(group1Matcher,group2Matcher);
        
        workflow.addJob(job1, rule);
        workflow.addJob(job2, rule);
        workflow.addJob(followingJob1);
        workflow.addJob(followingJob2);
        
        workflow.startJobs(group1Matcher);
        
        waitUntilJobsAreDone(4);
        assertThat(capturedDetails, containsInAnyOrder(job1, job2, followingJob1, followingJob2));
        assertThat(scheduler.getJobGroupNames(), not(contains("jobGroup1")));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void rejectsDurableJobs() throws Exception {
        final JobDetail job = JobBuilder.newJob().ofType(ConnectedTestJob.class).storeDurably().build();
        workflow.addJob(job);
    }

  }
