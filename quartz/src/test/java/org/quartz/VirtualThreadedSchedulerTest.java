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
package org.quartz;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleVirtualThreadPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class VirtualThreadedSchedulerTest extends AbstractSchedulerTest {

    @BeforeClass
    public static void runOnlyWhenVThreadAvailable(){
        Assume.assumeTrue("Virtual Threading is not available!",SimpleVirtualThreadPool.isVirtualThreadSupported() );
    }

    @Override
    protected Scheduler createScheduler(String name, int threadPoolSize) throws SchedulerException {
        final String schedulerName = name + "VScheduler" ;
        DirectSchedulerFactory.getInstance().createScheduler(schedulerName, "AUTO", new SimpleVirtualThreadPool(), new RAMJobStore());
        return SchedulerRepository.getInstance().lookup(schedulerName);
    }

    public static class TimeAdjustedJob implements Job {

        Throwable err = null ;

        @Override
        public void execute(JobExecutionContext context) {

            try {
                SchedulerContext schedulerContext = context.getScheduler().getContext();
                schedulerContext.put("JOB_THREAD", Thread.currentThread());
                schedulerContext.put("JOB_INSTANCE", this);
                long wait =  (long) schedulerContext.get("WAIT_TIME");
                Thread.sleep(wait); // job keeps on waiting
            } catch (Throwable e) {
                err = e;
            }
        }
    }
    @Override
    @Test
    public void testShutdownWithoutWaitIsUnclean() throws Exception {
        Scheduler scheduler = createScheduler("testShutdownWithoutWaitIsUnclean", 8);

        scheduler.getContext().put("WAIT_TIME", Long.MAX_VALUE);
        scheduler.start();
        scheduler.addJob(newJob().ofType(TimeAdjustedJob.class).withIdentity("job").storeDurably().build(), false);
        scheduler.scheduleJob(newTrigger().forJob("job").startNow().build());
        while (scheduler.getCurrentlyExecutingJobs().isEmpty()) {
            Thread.sleep(50);
        }
        scheduler.shutdown(false); // try doing it here..
        //At this point, we still would have the job running.. like forever...
        Thread taskThread = (Thread)scheduler.getContext().get("JOB_THREAD");
        Assert.assertTrue(taskThread.isAlive()); // this is what we test
        taskThread.interrupt(); // now we close it out
        Thread.sleep(50);
        TimeAdjustedJob job = (TimeAdjustedJob)scheduler.getContext().get("JOB_INSTANCE");
        Assert.assertTrue( job.err instanceof InterruptedException ); // this implies we have succeeded
    }

    @Override
    @Test
    public void testShutdownWithWaitIsClean() throws Exception {
        final AtomicBoolean shutdown = new AtomicBoolean(false);
        final Scheduler scheduler = createScheduler("testShutdownWithWaitIsClean", 8);
        scheduler.getContext().put("WAIT_TIME", 1500L);
        scheduler.start();
        scheduler.addJob(newJob().ofType(TimeAdjustedJob.class).withIdentity("job").storeDurably().build(), false);
        scheduler.scheduleJob(newTrigger().forJob("job").startNow().build());
        while (scheduler.getCurrentlyExecutingJobs().isEmpty()) {
            Thread.sleep(50);
        }

        Thread t = new Thread(() -> {
            try {
                scheduler.shutdown(true);
                shutdown.set(true);
                //At this point, we still would not have the job running..
                Thread taskThread = (Thread)scheduler.getContext().get("JOB_THREAD");
                assertFalse(taskThread.isAlive());

            } catch (SchedulerException ex) {
                throw new RuntimeException(ex);
            }
        });
        t.start();
        t.join();
        Assert.assertTrue( shutdown.get() );
    }
}
