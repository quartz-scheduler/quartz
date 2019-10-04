/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.integrations.tests;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.core.Is;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.simpl.CascadingClassLoadHelper;

import static org.quartz.impl.StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS;
import static org.quartz.integrations.tests.TrackingJob.SCHEDULED_TIMES_KEY;

public class JobClassNotFoundExceptionErrorsTriggersTest extends QuartzDatabaseTestSupport {

    private static final String BARRIER_KEY = "BARRIER";
    
    public static class BadJob implements Job {

        public void execute(JobExecutionContext context) {
            //no-op
        }
    }

    public static class GoodJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                ((CyclicBarrier) context.getScheduler().getContext().get(BARRIER_KEY)).await(20, TimeUnit.SECONDS);
            } catch (SchedulerException ex) {
                throw new JobExecutionException(ex);
            } catch (InterruptedException ex) {
                throw new JobExecutionException(ex);
            } catch (BrokenBarrierException ex) {
                throw new JobExecutionException(ex);
            } catch (TimeoutException ex) {
                throw new JobExecutionException(ex);
            }
        }
        
    }

    public static class SpecialClassLoadHelper extends CascadingClassLoadHelper {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (BadJob.class.getName().equals(name)) {
                throw new ClassNotFoundException();
            } else {
                return super.loadClass(name);
            }
        }
    }
    
    protected Properties createSchedulerProperties() {
        Properties properties = super.createSchedulerProperties();
        properties.put(PROP_SCHED_CLASS_LOAD_HELPER_CLASS, SpecialClassLoadHelper.class.getName());
        return properties;
    }

    @Test
    public void testJobClassNotFoundDoesntBlock() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        scheduler.getContext().put(BARRIER_KEY, barrier);

        JobDetail goodJob = JobBuilder.newJob(GoodJob.class).withIdentity("good").build();
        JobDetail badJob = JobBuilder.newJob(BadJob.class).withIdentity("bad").build();

        long now = System.currentTimeMillis();
        Trigger goodTrigger = TriggerBuilder.newTrigger().withIdentity("good").forJob(goodJob)
                .startAt(new Date(now + 1))
                .build();
        
        Trigger badTrigger = TriggerBuilder.newTrigger().withIdentity("bad").forJob(badJob)
                .startAt(new Date(now))
                .build();

        Map<JobDetail, Set<? extends Trigger>> toSchedule = new HashMap<JobDetail, Set<? extends Trigger>>();
        toSchedule.put(badJob, Collections.singleton(badTrigger));
        toSchedule.put(goodJob, Collections.singleton(goodTrigger));
        scheduler.scheduleJobs(toSchedule, true);

        barrier.await(20, TimeUnit.SECONDS);
        
        assertThat(scheduler.getTriggerState(badTrigger.getKey()), is(Trigger.TriggerState.ERROR));
    }
}
