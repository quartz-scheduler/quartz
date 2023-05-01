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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

class SchedulerJobs {
    private final Scheduler scheduler;
    private Map<JobDetail, Set<? extends Trigger>> jobs;
    SchedulerJobs(Scheduler scheduler) {
        super();
        this.scheduler = scheduler;
        this.jobs = new HashMap<>();
    }
    Scheduler getScheduler() {
        return scheduler;
    }
    
    void add(JobDetail job) throws SchedulerException {
        if(jobs.containsKey(job))
            throw new ObjectAlreadyExistsException(job);
        jobs.put(job, Collections.emptySet());
    }

    void storeJobs() throws SchedulerException {
        scheduler.scheduleJobs(jobs, false);
    }
    
    void rollback() throws SchedulerException {
        final List<JobKey> keys = jobs.keySet().stream()
                .map(JobDetail::getKey).collect(Collectors.toList());
        scheduler.deleteJobs(keys);
        jobs.clear();
    }
    
    public void commit() {
        jobs.clear();
    }
    
}
