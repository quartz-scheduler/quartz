/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.quartz.tests;

import com.tc.test.config.model.TestConfig;

import java.util.List;
import java.util.concurrent.CyclicBarrier;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 *
 * @author cdennis
 */
public class PersistJobDataAfterExecutionTest extends AbstractStandaloneTest {

  private static final int NODE_COUNT = 2;

  public PersistJobDataAfterExecutionTest(TestConfig testConfig) {
    super(testConfig, PersistJobData1.class, PersistJobData2.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  public static class PersistJobData1 extends ClientBase {

    public PersistJobData1(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new PersistJobData1(args).run();
    }

    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      
      scheduler.setJobFactory(new JobFactory() {

        @Override
        public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
          JobDetail jobDetail = bundle.getJobDetail();
          Class<? extends Job> jobClass = jobDetail.getJobClass();
          try {
            return jobClass.getConstructor(CyclicBarrier.class).newInstance(localBarrier);
          } catch (Exception e) {
            throw new SchedulerException(e);
          }
        }
      });

      
      JobDetail jobDetail = newJob(TestJob.class).withIdentity("job").build();
      jobDetail.getJobDataMap().put("id", "job");
      jobDetail.getJobDataMap().put(TestJob.JOB_PROP_NAME, 0);
      jobDetail.requestsRecovery();

      Trigger trigger = newTrigger().withIdentity("trigger")
              .withSchedule(simpleSchedule()
              .withIntervalInSeconds(2)
              .withRepeatCount(1))
              .build();
      scheduler.scheduleJob(jobDetail, trigger);
    
      // start node1's scheduler first to let it run some jobs, then switch to node2
      JobDetail lastRunningJob = null;
      List<JobExecutionContext> contextList = null;

      scheduler.start();
      localBarrier.await();
      contextList = scheduler.getCurrentlyExecutingJobs();
      Assert.assertEquals(contextList.size(), 1);
      lastRunningJob = contextList.get(0).getJobDetail();
      localBarrier.await();
      scheduler.shutdown(true);
    
      SchedulerMetaData metaData = scheduler.getMetaData();
      int numJobsExecuted = metaData.getNumberOfJobsExecuted();
      int finalCount = lastRunningJob.getJobDataMap().getIntValue(TestJob.JOB_PROP_NAME);

      Assert.assertThat(numJobsExecuted, Is.is(1));
      Assert.assertThat(finalCount, Is.is(1));
    }
    
    protected boolean isStartingScheduler() {
      return false;
    }
  }
  
  public static class PersistJobData2 extends ClientBase {

    public PersistJobData2(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new PersistJobData1(args).run();
    }

    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      
      scheduler.setJobFactory(new JobFactory() {

        @Override
        public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
          JobDetail jobDetail = bundle.getJobDetail();
          Class<? extends Job> jobClass = jobDetail.getJobClass();
          try {
            return jobClass.getConstructor(CyclicBarrier.class).newInstance(localBarrier);
          } catch (Exception e) {
            throw new SchedulerException(e);
          }
        }
      });

      // start node1's scheduler first to let it run some jobs, then switch to node2
      JobDetail lastRunningJob = null;
      List<JobExecutionContext> contextList = null;
      scheduler.start();
      localBarrier.await();
      contextList = scheduler.getCurrentlyExecutingJobs();
      Assert.assertEquals(contextList.size(), 1);
      lastRunningJob = contextList.get(0).getJobDetail();
      localBarrier.await();
      scheduler.shutdown(true);
    
      SchedulerMetaData metaData = scheduler.getMetaData();
      int numJobsExecuted = metaData.getNumberOfJobsExecuted();
      int finalCount = lastRunningJob.getJobDataMap().getIntValue(TestJob.JOB_PROP_NAME);

      Assert.assertThat(numJobsExecuted, Is.is(1));
      Assert.assertThat(finalCount, Is.is(2));
    }
    
    protected boolean isStartingScheduler() {
      return false;
    }
  }
  
  @PersistJobDataAfterExecution
  public static class TestJob implements Job {

    public static final String JOB_PROP_NAME = "counter";
    
    private final CyclicBarrier jobBarrier;
    
    public TestJob(CyclicBarrier jobBarrier) {
      this.jobBarrier = jobBarrier;
    }
    
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        int outputCount = data.getIntValue(JOB_PROP_NAME) + 1;
        data.put(JOB_PROP_NAME, outputCount);
        jobBarrier.await();
        jobBarrier.await();
      } catch (Exception e) {
        throw new JobExecutionException(e);
      }
    }
  }
}
