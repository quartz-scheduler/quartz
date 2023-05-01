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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.TriggerFiredBundle;

public class TestWorkflowFactory {
    public static final String SCHED_A = "A";
    public static final String SCHED_B = "B";

    public static void waitExtraTimeToCheckThatNoOtherJobsAreStarted() throws InterruptedException {
        Thread.sleep(200);
    }

    public static Scheduler schedulerA;

    public static Scheduler schedulerB;

    public final Workflow workflow = new Workflow();
    public final Semaphore semaphore  = new Semaphore(0);

    public final List<JobDetailWithPreviousJobKey> executedTestJobs = Collections.synchronizedList(new ArrayList<>());
    private static ScheduledJobSemaphore schedulerSemaphore;

    public static void startScheduler() throws SchedulerException, IOException {
        Properties configA = loadDefaultSchedulerConfiguration();
        Properties configB = loadDefaultSchedulerConfiguration();


        configA.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, SCHED_A);
        configB.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, SCHED_B);


        schedulerSemaphore = new ScheduledJobSemaphore();
        schedulerA = new StdSchedulerFactory(configA).getScheduler();
        schedulerA.getListenerManager().addSchedulerListener(schedulerSemaphore);
        schedulerB = new StdSchedulerFactory(configB).getScheduler();
        schedulerB.getListenerManager().addSchedulerListener(schedulerSemaphore);
        schedulerA.start();
        schedulerB.start();
    }

    private static Properties loadDefaultSchedulerConfiguration() throws IOException {
        Properties configA = new Properties();
        try(InputStream defaultConfig = SchedulerFactory.class.getResourceAsStream("quartz.properties")){
            configA.load(defaultConfig);
        }
        return configA;
    }

    public static void stopScheduler() throws SchedulerException {
        schedulerA.shutdown();
        schedulerB.shutdown();
    }

    public void setup() throws SchedulerException {
        schedulerA.setJobFactory(this::newTestJob);
        schedulerB.setJobFactory(this::newTestJob);
    }

    public void reset() {
        WorkflowJobStarter.INSTANCE.removeAllSchedulers();
    }

    public Job newTestJob(TriggerFiredBundle bundle, @SuppressWarnings("unused") Scheduler scheduler){
        Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
        if(jobClass.equals(ConnectedTestJob.class))
            return new ConnectedTestJob(semaphore, executedTestJobs);
        if(jobClass.equals(FailingTestJob.class))
            return new FailingTestJob(executedTestJobs);
        try {
            return jobClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void waitUntilConnectedJobsAreDone(final int jobCount) throws InterruptedException {
        assertTrue("jobs done", semaphore.tryAcquire(jobCount, 5, TimeUnit.SECONDS));
    }

    public void waitUntilAllJobsAreUnscheduled() throws InterruptedException {
        schedulerSemaphore.waitUntilAllJobsAreUnscheduled();
    }
}