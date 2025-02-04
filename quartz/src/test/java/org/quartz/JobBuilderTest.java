/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.quartz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;


/**
 * Test JobBuilder functionality 
 */
public class JobBuilderTest  {


    @SuppressWarnings("deprecation")
    public static class TestStatefulJob implements StatefulJob {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }

    public static class TestJob implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }
    
    @DisallowConcurrentExecution
    @PersistJobDataAfterExecution
    public static class TestAnnotatedJob implements Job {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
        }
    }

 /*   @Override
    protected void setUp() throws Exception {
    }*/

    @Test
    void testJobBuilder() throws Exception {
        
        JobDetail job = newJob()
            .ofType(TestJob.class)
            .withIdentity("j1")
            .storeDurably()
            .build();

        assertEquals("j1", job.getKey().getName(), "Unexpected job name: " + job.getKey().getName());
        assertEquals(JobKey.DEFAULT_GROUP, job.getKey().getGroup(), "Unexpected job group: " + job.getKey().getGroup());
        assertEquals(job.getKey(), jobKey("j1"), "Unexpected job key: " + job.getKey());
        assertNull(job.getDescription(), "Unexpected job description: " + job.getDescription());
        assertTrue(job.isDurable(), "Expected isDurable == true ");
        assertFalse(job.requestsRecovery(), "Expected requestsRecovery == false ");
        assertFalse(job.isConcurrentExecutionDisallowed(), "Expected isConcurrentExecutionDisallowed == false ");
        assertFalse(job.isPersistJobDataAfterExecution(), "Expected isPersistJobDataAfterExecution == false ");
        assertEquals(job.getJobClass(), TestJob.class, "Unexpected job class: " + job.getJobClass());
        
        job = newJob()
            .ofType(TestAnnotatedJob.class)
            .withIdentity("j1")
            .withDescription("my description")
            .storeDurably(true)
            .requestRecovery()
            .build();

        assertEquals("my description", job.getDescription(), "Unexpected job description: " + job.getDescription());
        assertTrue(job.isDurable(), "Expected isDurable == true ");
        assertTrue(job.requestsRecovery(), "Expected requestsRecovery == true ");
        assertTrue(job.isConcurrentExecutionDisallowed(), "Expected isConcurrentExecutionDisallowed == true ");
        assertTrue(job.isPersistJobDataAfterExecution(), "Expected isPersistJobDataAfterExecution == true ");
        
        job = newJob()
            .ofType(TestStatefulJob.class)
            .withIdentity("j1", "g1")
            .requestRecovery(false)
            .build();

        assertEquals("g1", job.getKey().getGroup(), "Unexpected job group: " + job.getKey().getName());
        assertFalse(job.isDurable(), "Expected isDurable == false ");
        assertFalse(job.requestsRecovery(), "Expected requestsRecovery == false ");
        assertTrue(job.isConcurrentExecutionDisallowed(), "Expected isConcurrentExecutionDisallowed == true ");
        assertTrue(job.isPersistJobDataAfterExecution(), "Expected isPersistJobDataAfterExecution == true ");
     
    }

}
