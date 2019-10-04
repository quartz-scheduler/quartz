/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import junit.framework.TestCase;

/**
 * Test JobBuilder functionality 
 */
public class JobBuilderTest extends TestCase {


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

    @Override
    protected void setUp() throws Exception {
    }

    public void testJobBuilder() throws Exception {
        
        JobDetail job = newJob()
            .ofType(TestJob.class)
            .withIdentity("j1")
            .storeDurably()
            .build();
        
        assertTrue("Unexpected job name: " + job.getKey().getName(), job.getKey().getName().equals("j1"));
        assertTrue("Unexpected job group: " + job.getKey().getGroup(), job.getKey().getGroup().equals(JobKey.DEFAULT_GROUP));
        assertTrue("Unexpected job key: " + job.getKey(), job.getKey().equals(jobKey("j1")));
        assertTrue("Unexpected job description: " + job.getDescription(), job.getDescription() == null);
        assertTrue("Expected isDurable == true ", job.isDurable());
        assertFalse("Expected requestsRecovery == false ", job.requestsRecovery());
        assertFalse("Expected isConcurrentExectionDisallowed == false ", job.isConcurrentExectionDisallowed());
        assertFalse("Expected isPersistJobDataAfterExecution == false ", job.isPersistJobDataAfterExecution());
        assertTrue("Unexpected job class: " + job.getJobClass(), job.getJobClass().equals(TestJob.class));
        
        job = newJob()
            .ofType(TestAnnotatedJob.class)
            .withIdentity("j1")
            .withDescription("my description")
            .storeDurably(true)
            .requestRecovery()
            .build();
        
        assertTrue("Unexpected job description: " + job.getDescription(), job.getDescription().equals("my description"));
        assertTrue("Expected isDurable == true ", job.isDurable());
        assertTrue("Expected requestsRecovery == true ", job.requestsRecovery());
        assertTrue("Expected isConcurrentExectionDisallowed == true ", job.isConcurrentExectionDisallowed());
        assertTrue("Expected isPersistJobDataAfterExecution == true ", job.isPersistJobDataAfterExecution());
        
        job = newJob()
            .ofType(TestStatefulJob.class)
            .withIdentity("j1", "g1")
            .requestRecovery(false)
            .build();
        
        assertTrue("Unexpected job group: " + job.getKey().getName(), job.getKey().getGroup().equals("g1"));
        assertFalse("Expected isDurable == false ", job.isDurable());
        assertFalse("Expected requestsRecovery == false ", job.requestsRecovery());
        assertTrue("Expected isConcurrentExectionDisallowed == true ", job.isConcurrentExectionDisallowed());
        assertTrue("Expected isPersistJobDataAfterExecution == true ", job.isPersistJobDataAfterExecution());
     
    }

}
