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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("null")
public class AllScheduledJobsTest {
    AllScheduledJobs uut = new AllScheduledJobs();
    private JobDetail job1;
    private Scheduler schedulerA;
    private JobDetail job2;
    private Scheduler schedulerB;
    
    private static Scheduler schedulerMock(String schedulerName) {
        try {
            Scheduler scheduler = mock(Scheduler.class);
            ListenerManager listenerManager = mock(ListenerManager.class);
            when(scheduler.getSchedulerName()).thenReturn(schedulerName);
            when(scheduler.getListenerManager()).thenReturn(listenerManager);
            return scheduler;
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Before
    public void setup() {
        job1 = JobBuilder.newJob().ofType(Job.class)
                .withIdentity("job1", "jobGroup1").build();
        schedulerA = schedulerMock("A");
        
        job2 = JobBuilder.newJob().ofType(Job.class)
                .withIdentity("job1", "jobGroup1").build();
        schedulerB = schedulerMock("B");

    }
    
    @After
    public void reset() {
        WorkflowJobStarter.INSTANCE.removeAllSchedulers();
    }
    
    @Test
    public void storesJobs() throws Exception {
        uut.register(schedulerA, job1);
        uut.register(schedulerB, job2);

        uut.storeJobs();
        
        verify(schedulerA).scheduleJobs(Collections.singletonMap(job1, Collections.emptySet()), false);
        verify(schedulerB).scheduleJobs(Collections.singletonMap(job2, Collections.emptySet()), false);
    }

    @Test
    public void deletesJobsOnRollback() throws Exception {
        uut.register(schedulerA, job1);
        uut.register(schedulerB, job2);
        
        uut.rollback();
        
        verify(schedulerA).deleteJobs(asList(job1.getKey()));
        verify(schedulerB).deleteJobs(asList(job2.getKey()));
    }

    @Test
    public void forgetsJobsOnCommit() throws Exception {
        uut.register(schedulerA, job1);
        uut.register(schedulerB, job2);
        
        uut.commit();
        
        uut.storeJobs();
        
        verify(schedulerA).scheduleJobs(Collections.emptyMap(), false);
        verify(schedulerB).scheduleJobs(Collections.emptyMap(), false);
    }

    @Test
    public void forgetsJobsOnRollbackt() throws Exception {
        uut.register(schedulerA, job1);
        uut.register(schedulerB, job2);
        
        uut.rollback();
        uut.rollback();
        
        verify(schedulerA).deleteJobs(asList(job1.getKey()));
        verify(schedulerB).deleteJobs(asList(job2.getKey()));
    }

    
    @Test
    public void whenSameJobIsAddedTwice_throwsException() throws Exception {
        uut.register(schedulerA, job1);
        assertThatThrownBy(() ->uut.register(schedulerA, job1))
            .isInstanceOf(ObjectAlreadyExistsException.class)
            .hasMessageContaining(job1.getKey().toString());
    }
}
