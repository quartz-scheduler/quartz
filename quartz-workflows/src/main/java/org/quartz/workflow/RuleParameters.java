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

import java.util.Date;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

class RuleParameters {
    static RuleParameters startParameters() {
        return startParameters("start");
    }

    static RuleParameters startParameters(String cause) {
        return new RuleParameters(cause, new Date(), JobData.EMPTY);
    }
    
    private final String cause;
    private final JobData jobData;
    private final Date scheduledStartTime;
    
    private RuleParameters(String cause, Date scheduledStartTime, JobData jobData) {
        super();
        this.cause = cause;
        this.scheduledStartTime = scheduledStartTime;
        this.jobData = jobData;
    }

    static RuleParameters from(JobExecutionContext context) {
        return new RuleParameters(context.getTrigger().getKey().toString(),
                context.getScheduledFireTime(),
                JobData.getJobOutputData(context));
    }
    
    RuleParameters withCause(String cause) {
        return new RuleParameters(cause, scheduledStartTime, jobData);
    }
     
    String getCause() {
        return cause;
    }
    
    Date getScheduledStartTime() {
        return scheduledStartTime;
    }

    void setInput(Trigger trigger) {
        jobData.setInput(trigger);
    }

    @Override
    public String toString() {
        return "RuleParameters [cause=" + cause 
                + ", parameters=" + jobData + "]";
    }

}
