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

import java.io.Serializable;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

public class JobData {
    private static final String INPUT_DATA = Workflow.WORKFLOW_RULE + "/InputData";
    private static final String OUTPUT_DATA = Workflow.WORKFLOW_RULE + "/OutputData";
    static final JobData EMPTY = new JobData(null);

    public static void setJobOutputData(JobExecutionContext context, Serializable value) {
        JobDataMap map = context.getMergedJobDataMap();
        setParameters(map, OUTPUT_DATA, value);
    }

    public static Object getJobInputData(JobExecutionContext context) {
        return context.getMergedJobDataMap().get(INPUT_DATA);
    }

    static JobData getJobOutputData(JobExecutionContext context) {
        return new JobData(context.getMergedJobDataMap().get(OUTPUT_DATA));
    }

    private static void setParameters(JobDataMap map, String key, Object value) {
        if(value == null)
            map.remove(key);
        else
            map.put(key, value);
    }

    private final Object parameters;

    private JobData(Object parameters) {
        this.parameters = parameters;
    }

    void setInput(Trigger trigger) {
        JobDataMap map = trigger.getJobDataMap();
        setParameters(map, INPUT_DATA, parameters);
    }

}
