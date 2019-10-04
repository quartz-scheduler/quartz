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

import org.quartz.impl.JobDetailImpl;

import junit.framework.TestCase;

/**
 * Unit test for JobDetail.
 */
public class JobDetailTest extends TestCase {

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

    public void testClone() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("hi");
        
        JobDetail clonedJobDetail = (JobDetail)jobDetail.clone();
        assertEquals(clonedJobDetail, jobDetail);
        
    }

    public void testAnnotationDetection() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("hi");

        jobDetail.setJobClass(SomePersistentJob.class);
        assertTrue("Expecting SomePersistentJob to be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertFalse("Expecting SomePersistentJob to not disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeNonConcurrentJob.class);
        assertFalse("Expecting SomeNonConcurrentJob to not be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertTrue("Expecting SomeNonConcurrentJob to disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeNonConcurrentPersistentJob.class);
        assertTrue("Expecting SomeNonConcurrentPersistentJob to be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertTrue("Expecting SomeNonConcurrentPersistentJob to disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeStatefulJob.class);
        assertTrue("Expecting SomeStatefulJob to be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertTrue("Expecting SomeStatefulJob to disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeExtendedPersistentJob.class);
        assertTrue("Expecting SomeExtendedPersistentJob to be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertFalse("Expecting SomeExtendedPersistentJob to not disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeExtendedNonConcurrentJob.class);
        assertFalse("Expecting SomeExtendedNonConcurrentJob to not be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertTrue("Expecting SomeExtendedNonConcurrentJob to disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeExtendedNonConcurrentPersistentJob.class);
        assertTrue("Expecting SomeExtendedNonConcurrentPersistentJob to be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertTrue("Expecting SomeExtendedNonConcurrentPersistentJob to disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());

        jobDetail.setJobClass(SomeExtendedStatefulJob.class);
        assertTrue("Expecting SomeExtendedStatefulJob to be persistent", jobDetail.isPersistJobDataAfterExecution());
        assertTrue("Expecting SomeExtendedStatefulJob to disallow concurrent execution", jobDetail.isConcurrentExectionDisallowed());
    }
}
