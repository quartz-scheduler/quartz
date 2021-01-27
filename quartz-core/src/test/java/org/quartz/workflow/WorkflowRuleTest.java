/*
 * Created on Jan 27, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
    public void singleJobRuleSchedulesJobImmediately() throws Exception {
        final JobKey job = new JobKey("name", "group");

        final WorkflowRule uut = new SingleJobRule(job);
        uut.startJobsIfReady(scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getStartTime(), lessThanOrEqualTo(new Date()));
    }


    @Test
    public void groupJobRuleSchedulesJobImmediately() throws Exception {
        final JobKey job = new JobKey("name", "following");
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.emptySet());
        when(scheduler.getJobKeys(followingGroup)).thenReturn(Collections.singleton(job));

        final WorkflowRule uut = new GroupRule(currentGroup, followingGroup);
        uut.startJobsIfReady(scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger.getStartTime(), lessThanOrEqualTo(new Date()));
    }

    @Test
    public void singleJobRuleSchedulesNoJob_whenGroupIsNotDone() throws Exception {
        final JobKey job = new JobKey("name", "group");
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");
        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.singleton(new JobKey("other", "group")));
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        final SingleJobRule uut = new SingleJobRule(currentGroup, job);
        uut.startJobsIfReady(scheduler);

        verify(scheduler, never()).getJobKeys(followingGroup);
        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    public void groupRuleSchedulesNoJob_whenGroupIsNotDone() throws Exception {
        GroupMatcher<JobKey> currentGroup = GroupMatcher.groupEquals("group");

        when(scheduler.getJobKeys(currentGroup)).thenReturn(Collections.singleton(new JobKey("other", "group")));
        GroupMatcher<JobKey> followingGroup = GroupMatcher.groupEquals("following");

        final GroupRule uut = new GroupRule(currentGroup, followingGroup);
        uut.startJobsIfReady(scheduler);

        verify(scheduler, never()).getJobKeys(followingGroup);
        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    public void schedulesJobWithDefaultPrority() throws Exception {
        final JobKey job = new JobKey("name", "group");

        final WorkflowRule uut = new SingleJobRule(job);
        uut.startJobs(scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertEquals(Trigger.DEFAULT_PRIORITY, trigger.getPriority());
    }

    @Test
    public void schedulesJobWithGivenPrority() throws Exception {
        final JobKey job = new JobKey("name", "group");
        final int priority = Trigger.DEFAULT_PRIORITY + 1;

        final WorkflowRule uut = new SingleJobRule(job).setTriggerPriority(priority);
        uut.startJobs(scheduler);

        verify(scheduler).scheduleJob(triggerCaptor.capture());
        final Trigger trigger = triggerCaptor.getValue();
        assertEquals(priority, trigger.getPriority());
    }

    @Test
    public void usesSameTriggerKeyForSameJobKey() throws Exception {
        final JobKey job = new JobKey("name", "group");

        final WorkflowRule uut = new SingleJobRule(job).setTriggerPriority(32);
        uut.startJobs(scheduler);
        uut.startJobs(scheduler);

        verify(scheduler, times(2)).scheduleJob(triggerCaptor.capture());
        final List<Trigger> triggers = triggerCaptor.getAllValues();

        assertEquals(triggers.get(0).getKey(), triggers.get(1).getKey());
    }

    @Test
    public void usesDifferentTriggerKeysForDifferentJobKeys() throws Exception {
        new SingleJobRule(new JobKey("name1", "group")).startJobs(scheduler);
        new SingleJobRule(new JobKey("name2", "group")).startJobs(scheduler);

        verify(scheduler, times(2)).scheduleJob(triggerCaptor.capture());
        final List<Trigger> triggers = triggerCaptor.getAllValues();
        assertThat(triggers.get(0).getKey(), not(equalTo(triggers.get(1).getKey())));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void ignoresExceptionIfWorkflowStartsTheSameJobTwice() throws Exception {
        final JobKey job = new JobKey("name", "group");
        final WorkflowRule uut = new SingleJobRule(job);
        when(scheduler.scheduleJob(any())).thenThrow(ObjectAlreadyExistsException.class);

        uut.startJobs(scheduler);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ignoresExceptionIfWorkflowStartsAlreadyDoneJob() throws Exception {
        final JobKey job = new JobKey("name", "group");
        final WorkflowRule uut = new SingleJobRule(job);
        when(scheduler.scheduleJob(any())).thenThrow(JobPersistenceException.class);

        uut.startJobs(scheduler);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = JobPersistenceException.class)
    public void rethrowsJobPersistenceExceptionIfJobIsNotDone() throws Exception {
        final JobKey job = new JobKey("name", "group");
        when(scheduler.scheduleJob(any())).thenThrow(JobPersistenceException.class);
        when(scheduler.checkExists(job)).thenReturn(true);

        final WorkflowRule uut = new SingleJobRule(job);
        uut.startJobs(scheduler);
    }
}
