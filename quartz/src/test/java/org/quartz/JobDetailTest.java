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
import org.quartz.impl.JobDetailImpl;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit test for JobDetail.
 */
public class JobDetailTest  {

    @PersistJobDataAfterExecution
    public class SomePersistentJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
       }
    }

    public class SomeExtendedPersistentJob extends SomePersistentJob {
    }

    @DisallowConcurrentExecution
    public class SomeNonConcurrentJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
        }
    }

    public class SomeExtendedNonConcurrentJob extends SomeNonConcurrentJob {
    }

    @DisallowConcurrentExecution
    @PersistJobDataAfterExecution
    public class SomeNonConcurrentPersistentJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
        }
    }

    public class SomeExtendedNonConcurrentPersistentJob extends SomeNonConcurrentPersistentJob {
    }

    public class SomeStatefulJob implements StatefulJob {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
        }
    }

    public class SomeExtendedStatefulJob extends SomeStatefulJob {
    }

    @Test
    void testClone() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("hi");
        
        JobDetail clonedJobDetail = (JobDetail)jobDetail.clone();
        assertEquals(clonedJobDetail, jobDetail);
        
    }
    @Test
    void testAnnotationDetection() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("hi");

        jobDetail.setJobClass(SomePersistentJob.class);
        assertTrue(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomePersistentJob to be persistent");
        assertFalse(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomePersistentJob to not disallow concurrent execution");

        jobDetail.setJobClass(SomeNonConcurrentJob.class);
        assertFalse(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeNonConcurrentJob to not be persistent");
        assertTrue(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeNonConcurrentJob to disallow concurrent execution");

        jobDetail.setJobClass(SomeNonConcurrentPersistentJob.class);
        assertTrue(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeNonConcurrentPersistentJob to be persistent");
        assertTrue(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeNonConcurrentPersistentJob to disallow concurrent execution");

        jobDetail.setJobClass(SomeStatefulJob.class);
        assertTrue(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeStatefulJob to be persistent");
        assertTrue(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeStatefulJob to disallow concurrent execution");

        jobDetail.setJobClass(SomeExtendedPersistentJob.class);
        assertTrue(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeExtendedPersistentJob to be persistent");
        assertFalse(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeExtendedPersistentJob to not disallow concurrent execution");

        jobDetail.setJobClass(SomeExtendedNonConcurrentJob.class);
        assertFalse(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeExtendedNonConcurrentJob to not be persistent");
        assertTrue(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeExtendedNonConcurrentJob to disallow concurrent execution");

        jobDetail.setJobClass(SomeExtendedNonConcurrentPersistentJob.class);
        assertTrue(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeExtendedNonConcurrentPersistentJob to be persistent");
        assertTrue(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeExtendedNonConcurrentPersistentJob to disallow concurrent execution");

        jobDetail.setJobClass(SomeExtendedStatefulJob.class);
        assertTrue(jobDetail.isPersistJobDataAfterExecution(), "Expecting SomeExtendedStatefulJob to be persistent");
        assertTrue(jobDetail.isConcurrentExecutionDisallowed(), "Expecting SomeExtendedStatefulJob to disallow concurrent execution");
    }
}
