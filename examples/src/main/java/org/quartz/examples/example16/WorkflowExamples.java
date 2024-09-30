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
 *
 */

package org.quartz.examples.example16;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.workflow.Workflow;
import org.quartz.workflow.WorkflowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Example will demonstrate how to use Workflows
 *
 * @author Dimitry Polivaev
 */
public class WorkflowExamples {

    public void run() throws Exception {
        Logger log = LoggerFactory.getLogger(WorkflowExamples.class);

        log.info("------- Initializing ----------------------");

        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();


        // Start up the scheduler (nothing can actually run until the
        // scheduler has been started)
        sched.start();

        log.info("------- Started Scheduler -----------------");

        runSimpleWorkflowExample(sched);
        runSimpleTriggerWorkflowExample(sched);
        runComplexWorkflowExample(sched);

        // shut down the scheduler
        log.info("------- Shutting Down ---------------------");
        sched.shutdown(true);
        log.info("------- Shutdown Complete -----------------");
    }

    private void runSimpleWorkflowExample(Scheduler sched) throws Exception {
        Workflow workflow = new Workflow();
        workflow.setDefaultScheduler(sched);

        // Add a job to the workflow, create a JobDetail instance and add it to the desired scheduler:
        JobDetail job1 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(job1);

        // Define a start rule for starting a job when the workflow starts:
        workflow.addStartRule(WorkflowRule.with(job1));

        // Connect jobs using rules, create a WorkflowRule instance and add it to the job that should be followed:
        JobDetail followingJob1 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(followingJob1);
        final WorkflowRule rule = WorkflowRule.with(followingJob1);
        workflow.addRule(job1, rule);

        // Start the workflow:
        workflow.start();

        // Wait until all workflow jobs are done:
        WorkflowJob.waitUntilWorkflowJobsAreDone(2);
    }

    private void runSimpleTriggerWorkflowExample(Scheduler sched) throws Exception {
        Workflow workflow = new Workflow();
        workflow.setDefaultScheduler(sched);

        JobDetail job1 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("job1", "jobGroup1").storeDurably().build();
        workflow.addJob(job1);

        JobDetail followingJob1 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(followingJob1);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger")
                .forJob(job1).startNow().build();
        workflow.addStartRule(WorkflowRule.with(trigger));

        final WorkflowRule rule = WorkflowRule.with(followingJob1);
        workflow.addRule(trigger, rule);

        workflow.start();

        WorkflowJob.waitUntilWorkflowJobsAreDone(2);

        sched.deleteJob(job1.getKey());
    }

    private void runComplexWorkflowExample(Scheduler sched) throws Exception {
        Workflow workflow = new Workflow();
        workflow.setDefaultScheduler(sched);

        // Start 2 jobs at the workflow start
        JobDetail job1 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("job1", "jobGroup1").build();
        workflow.addJob(job1);

        JobDetail job2 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("job2", "jobGroup1").build();
        workflow.addJob(job2);

        workflow.addStartRule(WorkflowRule.with(job1));
        workflow.addStartRule(WorkflowRule.with(job2));

        // Start 2 following jobs after all of them finish

        JobDetail followingJob1 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("followingJob1", "jobGroup2").build();
        workflow.addJob(followingJob1);

        JobDetail followingJob2 = JobBuilder.newJob(WorkflowJob.class)
                .withIdentity("followingJob2", "jobGroup2").build();
        workflow.addJob(followingJob2);

        final WorkflowRule rule =
                WorkflowRule.with(followingJob1)
                .with(WorkflowRule.with(followingJob2))
                .when(WorkflowRule.when(job1))
                .when(WorkflowRule.when(job2));

        workflow.addRule(job1, rule).addRule(job2, rule);

        // Start the workflow:

        workflow.start();

        WorkflowJob.waitUntilWorkflowJobsAreDone(4);
    }

    public static void main(String[] args) throws Exception {

        WorkflowExamples example = new WorkflowExamples();
        example.run();

  }

}
