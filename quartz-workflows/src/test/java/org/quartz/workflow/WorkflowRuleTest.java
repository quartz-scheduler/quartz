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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("boxing")
public class WorkflowRuleTest {
    @Mock private Scheduler scheduler;
    @Captor ArgumentCaptor<Trigger> triggerCaptor;

    @Test
    public void jobRuleSchedulesJobImmediately() throws Exception {
        final JobKey job = new JobKey("name", "group");

        final JobRule uut = WorkflowRule.with(job);
        RuleParameters startParameters = RuleParameters.startParameters();
        uut.apply(startParameters, x -> scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getStartTime()).isEqualTo(startParameters.getScheduledStartTime());
    }


    @Test
    public void groupJobRuleSchedulesJobImmediately() throws Exception {
        final JobKey job = new JobKey("name", "following");
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.emptySet());
        when(scheduler.getJobKeys(followingGroup)).thenReturn(Collections.singleton(job));

        final WorkflowRule uut = WorkflowRule.when(currentGroup).applying(WorkflowRule.with(followingGroup));
        RuleParameters startParameters = RuleParameters.startParameters();
        uut.apply(startParameters, x -> scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getStartTime()).isEqualTo(startParameters.getScheduledStartTime());
    }

    @Test
    public void jobRuleSchedulesNoJob_whenGroupIsNotDone() throws Exception {
        final JobKey job = new JobKey("name", "group");
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");
        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.singleton(new JobKey("other", "group")));
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        final WorkflowRule uut =  WorkflowRule.when(currentGroup).applying(WorkflowRule.with(job));
        uut.apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler, never()).getJobKeys(followingGroup);
        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    public void groupRuleSchedulesNoJob_whenGroupIsNotDone() throws Exception {
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");

        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.singleton(new JobKey("other", "group")));
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        final WorkflowRule uut = WorkflowRule.when(currentGroup).applying(WorkflowRule.with(followingGroup));
        uut.apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler, never()).getJobKeys(followingGroup);
        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    public void schedulesJobWithDefaultPrority() throws Exception {
        final JobKey job = new JobKey("name", "group");

        final WorkflowRule uut = WorkflowRule.with(job);
        uut.apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getPriority()).isEqualTo(Trigger.DEFAULT_PRIORITY);
    }

    @Test
    public void schedulesJobWithGivenPrority() throws Exception {
        final JobKey job = new JobKey("name", "group");
        final int priority = Trigger.DEFAULT_PRIORITY + 1;

        final WorkflowRule uut = WorkflowRule.with(job).setTriggerPriority(priority);
        uut.apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getPriority()).isEqualTo(priority);
    }
    

    @Test
    public void schedulesGroupJobWithGivenPrority() throws Exception {
        final JobKey job = new JobKey("name", "following");
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.emptySet());
        when(scheduler.getJobKeys(followingGroup)).thenReturn(Collections.singleton(job));
        final int priority = Trigger.DEFAULT_PRIORITY + 1;
        final WorkflowRule uut = WorkflowRule.when(currentGroup)
                .applying(WorkflowRule.with(followingGroup).setTriggerPriority(priority));
        uut.apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getPriority()).isEqualTo(priority);
    }


    @Test
    public void usesSameTriggerKeyForSameJobKey() throws Exception {
        final JobKey job = new JobKey("name", "group");

        final WorkflowRule uut = WorkflowRule.with(job);
        uut.apply(RuleParameters.startParameters(), x -> scheduler);
        uut.apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler, times(2)).scheduleJob(triggerCaptor.capture());
        final List<Trigger> triggers = triggerCaptor.getAllValues();

        assertThat(triggers.get(0).getKey()).isEqualTo(triggers.get(1).getKey());
    }

    @Test
    public void usesDifferentTriggerKeysForDifferentJobKeys() throws Exception {
        WorkflowRule.with(new JobKey("name1", "group")).apply(RuleParameters.startParameters(), x -> scheduler);
        WorkflowRule.with(new JobKey("name2", "group")).apply(RuleParameters.startParameters(), x -> scheduler);

        verify(scheduler, times(2)).scheduleJob(triggerCaptor.capture());
        final List<Trigger> triggers = triggerCaptor.getAllValues();
        assertThat(triggers.get(0).getKey()).isNotEqualTo(triggers.get(1).getKey());
    }


    @Test
    public void ignoresExceptionIfWorkflowStartsTheSameJobTwice() throws Exception {
        final JobKey job = new JobKey("name", "group");
        final WorkflowRule uut = WorkflowRule.with(job);
        when(scheduler.scheduleJob(any())).thenThrow(ObjectAlreadyExistsException.class);

        uut.apply(RuleParameters.startParameters(), x -> scheduler);
    }


    @Test
    public void ignoresExceptionIfWorkflowStartsAlreadyDoneJob() throws Exception {
        final JobKey job = new JobKey("name", "group");
        final WorkflowRule uut = WorkflowRule.with(job);
        when(scheduler.scheduleJob(any())).thenThrow(JobPersistenceException.class);

        uut.apply(RuleParameters.startParameters(), x -> scheduler);
    }


    @Test
    public void rethrowsJobPersistenceExceptionIfJobIsNotDone() throws Exception {
        final JobKey job = new JobKey("name", "group");
        when(scheduler.scheduleJob(any())).thenThrow(JobPersistenceException.class);
        when(scheduler.checkExists(job)).thenReturn(true);

        final WorkflowRule uut = WorkflowRule.with(job);
        assertThatThrownBy(() ->uut.apply(RuleParameters.startParameters(), x -> scheduler)).isInstanceOf(JobPersistenceException.class);
    }
}
