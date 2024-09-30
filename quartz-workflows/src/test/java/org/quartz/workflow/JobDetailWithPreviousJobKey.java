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

import java.util.Objects;

import org.quartz.JobDetail;
import org.quartz.JobKey;

public class JobDetailWithPreviousJobKey {
    final JobDetail jobDetail;
    final JobKey previousJobKey;
    JobDetailWithPreviousJobKey(JobDetail jobDetail, JobKey previousJobKey) {
        super();
        this.jobDetail = jobDetail;
        this.previousJobKey = previousJobKey;
    }


    public JobDetail jobDetail() {
        return jobDetail;
    }


    Object previousJobKey() {
        return previousJobKey;
    }


    @Override
    public int hashCode() {
        return Objects.hash(jobDetail, previousJobKey);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JobDetailWithPreviousJobKey other = (JobDetailWithPreviousJobKey) obj;
        return Objects.equals(jobDetail, other.jobDetail) && Objects.equals(previousJobKey,
                other.previousJobKey);
    }
    @Override
    public String toString() {
        return "JobDetailWithPreviousJobKey [jobDetail=" + jobDetail +
                ", previousJobKey=" + previousJobKey + "]";
    }
}