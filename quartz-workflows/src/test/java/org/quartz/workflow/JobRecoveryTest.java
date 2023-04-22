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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.workflow.extension.SynchronizationJob;


public class JobRecoveryTest {
    private static TriggerBuilder<Trigger> triggerWithRecoveryAttempts(int attempts) {
        return TriggerBuilder.newTrigger().usingJobData(JobRecovery.RECOVERY_ATTEMPTS, attempts);
    }
    
    static private JobExecutionContext mockJobExecutionContext(Class<? extends Job> jobClass,
            Trigger trigger, JobDataMap mergedJobDataMap, Scheduler scheduler) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(
                JobBuilder.newJob(jobClass).build());
        when(context.getScheduler()).thenReturn(scheduler);
        when(context.getMergedJobDataMap()).thenReturn(mergedJobDataMap);
        when(context.getTrigger()).thenReturn(trigger);
        return context;
    }
    static class ConcurrentlyExecutableJob extends SynchronizationJob{/**/}
    
    @DisallowConcurrentExecution
    static class SequentuallyExecutableJob extends SynchronizationJob{/**/}
    
   @Test
   public void schedulesRecoveryFirstAttempt() throws Exception {
       JobKey jobKey = new JobKey("name", "group");
       Class< ? extends Job> jobClass = ConcurrentlyExecutableJob.class;
       Trigger trigger = TriggerBuilder.newTrigger()
               .forJob(jobKey)
               .usingJobData(Workflow.WORKFLOW_RULE, Workflow.WORKFLOW_RULE)
               .build();
       JobDataMap mergedJobDataMap = new JobDataMap();
       Scheduler scheduler = mock(Scheduler.class);
       JobExecutionContext context = mockJobExecutionContext(jobClass, trigger, mergedJobDataMap,
               scheduler);
       JobRecovery uut = new JobRecovery(context, 3, 1000);
       
       Date expectedStartAfter = new Date(System.currentTimeMillis());
       
       uut.start();
       
       Date expectedStartBefore = new Date(System.currentTimeMillis() + 1000);
       
       ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
       
       assertThat(mergedJobDataMap).doesNotContainKey(Workflow.WORKFLOW_RULE);
       verify(scheduler).scheduleJob(triggerCaptor.capture());
       Trigger scheduledTrigger = triggerCaptor.getValue();
       assertThat(scheduledTrigger.getJobKey()).isEqualTo(jobKey);
       assertThat(scheduledTrigger.getStartTime()).isBetween(expectedStartAfter, expectedStartBefore, true, true);
       assertThat(scheduledTrigger.getPriority()).isEqualTo(trigger.getPriority() + 1);
       assertThat(scheduledTrigger.getMisfireInstruction()).isEqualTo(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
       assertThat(scheduledTrigger.getJobDataMap()).containsEntry(JobRecovery.RECOVERY_ATTEMPTS, 2);
   }
   
  @Test
  public void callsResetOnSuccess() throws Exception {
      JobKey jobKey = new JobKey("name", "group");
      Class< ? extends Job> jobClass = ConcurrentlyExecutableJob.class;
      Trigger trigger = TriggerBuilder.newTrigger()
              .forJob(jobKey)
              .usingJobData(Workflow.WORKFLOW_RULE, Workflow.WORKFLOW_RULE)
              .build();
      JobDataMap mergedJobDataMap = new JobDataMap();
      Scheduler scheduler = mock(Scheduler.class);
      JobExecutionContext context = mockJobExecutionContext(jobClass, trigger, mergedJobDataMap,
              scheduler);
      JobRecovery uut = new JobRecovery(context, 3, 1000);
      
      Runnable reset = mock(Runnable.class);
      uut.start(reset);
      
      InOrder inOrder = inOrder(reset, scheduler);
      inOrder.verify(reset).run();
      inOrder.verify(scheduler).scheduleJob(any());
  }


    @Test
    public void schedulesRecoveryNextAttempt() throws Exception {
        JobKey jobKey = new JobKey("name", "group");
        Class< ? extends Job> jobClass = ConcurrentlyExecutableJob.class;
        Trigger trigger = triggerWithRecoveryAttempts(2)
                .forJob(jobKey)
                .usingJobData(Workflow.WORKFLOW_RULE, Workflow.WORKFLOW_RULE)
                .build();
        JobDataMap mergedJobDataMap = new JobDataMap();
        Scheduler scheduler = mock(Scheduler.class);
        JobExecutionContext context = mockJobExecutionContext(jobClass, trigger, mergedJobDataMap,
                scheduler);
        JobRecovery uut = new JobRecovery(context, 3, 1000);
        
        Date expectedStartAfter = new Date(System.currentTimeMillis());
        uut.start();
        Date expectedStartBefore = new Date(System.currentTimeMillis() + 1000);
        
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        
        assertThat(mergedJobDataMap).doesNotContainKey(Workflow.WORKFLOW_RULE);
        verify(scheduler).scheduleJob(triggerCaptor.capture());
        Trigger scheduledTrigger = triggerCaptor.getValue();
        assertThat(scheduledTrigger.getJobKey()).isEqualTo(jobKey);
        assertThat(scheduledTrigger.getStartTime()).isBetween(expectedStartAfter, expectedStartBefore, true, true);
        assertThat(scheduledTrigger.getPriority()).isEqualTo(trigger.getPriority());
        assertThat(scheduledTrigger.getMisfireInstruction()).isEqualTo(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
        assertThat(scheduledTrigger.getJobDataMap()).containsEntry(JobRecovery.RECOVERY_ATTEMPTS, 1);
    }


    @Test
    public void scheduleRecovery_throwsAllRecoveryAttemptsFailedException_ifRecoverAttemptsCounterIsNonPositive() throws Exception {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getTrigger()).thenReturn(triggerWithRecoveryAttempts(0).build());
        when(context.getJobDetail()).thenReturn(
                JobBuilder.newJob(ConcurrentlyExecutableJob.class).build());
        JobRecovery uut = new JobRecovery(context, 3, 1000);
        Runnable reset = mock(Runnable.class);
        assertThatThrownBy(() -> uut.start(reset)).isInstanceOf(AllRecoveryAttemptsFailedException.class);
        verify(reset, never()).run();
    }
    
    
    @Test
    public void sleepsForSequentialJobs() throws Exception {
        JobKey jobKey = new JobKey("name", "group");
        Class< ? extends Job> jobClass = SequentuallyExecutableJob.class;
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobKey)
                .usingJobData(Workflow.WORKFLOW_RULE, Workflow.WORKFLOW_RULE)
                .build();
        JobDataMap mergedJobDataMap = new JobDataMap();
        Scheduler scheduler = mock(Scheduler.class);
        JobExecutionContext context = mockJobExecutionContext(jobClass, trigger, mergedJobDataMap,
                scheduler);
        int delayMillis = 50;
        JobRecovery uut = new JobRecovery(context, 3, delayMillis);
        
        long timeBeforeScheduling = System.currentTimeMillis();
        uut.start();
        assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(timeBeforeScheduling + delayMillis);
    }
    
    @Test
    public void doesNotSleepsForConcurrentJobs() throws Exception {
        JobKey jobKey = new JobKey("name", "group");
        Class< ? extends Job> jobClass = ConcurrentlyExecutableJob.class;
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobKey)
                .usingJobData(Workflow.WORKFLOW_RULE, Workflow.WORKFLOW_RULE)
                .build();
        JobDataMap mergedJobDataMap = new JobDataMap();
        Scheduler scheduler = mock(Scheduler.class);
        JobExecutionContext context = mockJobExecutionContext(jobClass, trigger, mergedJobDataMap,
                scheduler);
        int delayMillis = 50;
        JobRecovery uut = new JobRecovery(context, 3, delayMillis);
        
        long timeBeforeScheduling = System.currentTimeMillis();
        uut.start();
        assertThat(System.currentTimeMillis()).isLessThan(timeBeforeScheduling + delayMillis);
    }

}
