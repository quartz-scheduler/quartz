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

import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AllScheduledJobs {
    private static Logger LOG = LoggerFactory.getLogger(AllScheduledJobs.class);
    private final Map<String, SchedulerJobs > schedulers;
    private SchedulerJobs malfunctioningScheduler;
    private Scheduler defaultScheduler;

    public AllScheduledJobs() {
        super();
        this.schedulers = new HashMap<>();
        this.defaultScheduler = null;
    }
    
    private Scheduler getScheduler(String schedulerName) throws SchedulerException {
        if(schedulerName == null) {
            if(defaultScheduler == null) 
                throw new SchedulerException("Default scheduler not set");
            return defaultScheduler;
        }
        final SchedulerJobs scheduler = schedulers.get(schedulerName);
        if(scheduler == null)
            throw new SchedulerException("Unknown scheduler " +schedulerName);
        return scheduler.getScheduler();
    }
    
    void register(Scheduler scheduler, JobDetail job) throws SchedulerException {
        final SchedulerJobs jobs = register(scheduler);
        jobs.add(job);
    }

    void register(JobDetail job) throws SchedulerException {
        final SchedulerJobs jobs = schedulers.get(defaultScheduler.getSchedulerName());
        jobs.add(job);
    }

    void setDefaultScheduler(Scheduler scheduler) throws SchedulerException {
        register(scheduler);
        this.defaultScheduler = scheduler;
    }

    SchedulerJobs register(Scheduler scheduler) throws SchedulerException {
        WorkflowJobStarter.INSTANCE.addScheduler(scheduler);
        final String schedulerName = scheduler.getSchedulerName();
        final SchedulerJobs jobs = this.schedulers.computeIfAbsent(schedulerName, x -> new SchedulerJobs(scheduler));
        return jobs;
    }

    void apply(WorkflowRule rule) throws SchedulerException {
        rule.apply(RuleParameters.startParameters(), this::getScheduler);
    }

    void storeJobs() throws SchedulerException {
            for(SchedulerJobs scheduler: schedulers.values()) {
                malfunctioningScheduler = scheduler;
                scheduler.storeJobs();
                malfunctioningScheduler = null;
            }
    }

    void commit() {
        for(SchedulerJobs scheduler: schedulers.values()) {
            scheduler.commit();
        }
    }

    void rollback() {
        for(SchedulerJobs scheduler: schedulers.values()) {
            if(scheduler == malfunctioningScheduler)
                break;
            try {
                scheduler.rollback();
            } catch (SchedulerException e) {
                LOG.error("Can't rollback jobs", e);
            }
        }
    }

}