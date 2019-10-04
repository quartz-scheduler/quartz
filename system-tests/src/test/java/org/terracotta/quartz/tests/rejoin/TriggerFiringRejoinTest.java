/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.quartz.tests.rejoin;

import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobPersistenceException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.rejoin.RejoinException;

/**
 *
 * @author cdennis
 */
public class TriggerFiringRejoinTest extends AbstractRejoinTest {
  
  public TriggerFiringRejoinTest(TestConfig testConfig) {
    super(testConfig, Client.class, Client.class);
  }

  public static class Client extends AbstractRejoinClient {
    private static final int                           NUM    = 100;

    private final ToolkitBarrier                       barrier;
    private final Toolkit                              toolkit;

    public Client(String[] args) {
      super(args);

      toolkit = getClusteringToolkit();
      barrier = toolkit.getBarrier("barrier", 2);
    }

    @Override
    protected boolean isStartingScheduler() {
      return false;
    }
    
    @Override
    public void addSchedulerProperties(Properties properties) {
      super.addSchedulerProperties(properties);
      properties.setProperty("org.quartz.threadPool.threadCount", "1");
    }
    
    @Override
    protected void test(Scheduler scheduler) throws Throwable {
      final int ITERATIONS = 5;

      int index = barrier.await();

      for (int cnt = 0; cnt < NUM; cnt++) {
        if (index == 0) {
          String jobName = "myJob" + cnt;
          System.out.println("Scheduling Job: " + jobName);
          JobDetail jobDetail = JobBuilder.newJob(Client.TestJob.class).withIdentity(jobName)
                  .usingJobData("data", 0).storeDurably().requestRecovery().build();

          Trigger trigger = TriggerBuilder
              .newTrigger()
              .withIdentity("triggerName" + cnt)
              .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(5)
                                .withRepeatCount(ITERATIONS - 1)).build();

          scheduler.scheduleJob(jobDetail, trigger);
        }
      }

      barrier.await();

      scheduler.start();

      Thread.sleep(10000);
      
      if (index == 0) {
        initiateRejoin();

        int doneCount = 0;
        while (doneCount != NUM) {
          try {
            ThreadUtil.reallySleep(1000L);

            Set<JobKey> completeJobs = new HashSet<JobKey>();
            Set<JobKey> incompleteJobs = new HashSet<JobKey>();
            for (int i = 0; i < NUM; i++) {
              String jobName = "myJob" + i;
              JobDetail jobDetail = scheduler.getJobDetail(new JobKey(jobName));
              if (jobDetail.getJobDataMap().getInt("data") >= ITERATIONS) {
                completeJobs.add(jobDetail.getKey());
              } else {
                incompleteJobs.add(jobDetail.getKey());
              }
            }

            doneCount = completeJobs.size();
            synchronized (System.out) {
              System.out.println("doneCount: " + doneCount + " incomplete : " + incompleteJobs);
              if (doneCount > 0) {
                for (JobKey jk : incompleteJobs) {
                  for (Trigger t : scheduler.getTriggersOfJob(jk)) {
                    System.out.println(t + " " + scheduler.getTriggerState(t.getKey()));
                  }
                }
              }
            }
          } catch (JobPersistenceException _) {
            //ignore
          }
        }
      }

      while (true) {
        try {
          barrier.await();
          break;
        } catch (RejoinException e) {
          while (true) {
            try {
              barrier.reset();
              break;
            } catch (RejoinException f) {
              //nothing to do
            }
          }
        }
      }

      scheduler.shutdown(true);
    }

    @PersistJobDataAfterExecution
    @DisallowConcurrentExecution
    public static class TestJob implements Job {

      public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        int val = dataMap.getInt("data") + 1;
        dataMap.put("data", val);
        System.out.println("Called:" + context.getJobDetail().getKey() + ": " + val);
      }

      long incrementAndGet(Map<String, ToolkitAtomicLong> map, String key) {
        ToolkitAtomicLong current = map.get(key);
        return current.incrementAndGet();
      }
    }
  }

}
