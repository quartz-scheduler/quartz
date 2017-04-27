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

package org.quartz;

import org.quartz.utils.Key;

/**
 *
 * Uniquely identifies a {@link Trigger}.
 * 一个触发器的唯一识别标识
 * <p>Keys are composed of both a name and group, and the name must be unique
 *
 * key被一个名称和组组合,名称必须在组内唯一.乳沟一个名称被制定一个默认组,名称将会被使用
 * within the group.  If only a name is specified then the default group
 * name will be used.</p> 
 *
 * quartz提供了构造者风格的api来构造调度相关的实例,通过领域语言,领域语言能
 * <p>Quartz provides a builder-style API for constructing scheduling-related
 * entities via a Domain-Specific Language (DSL).  The DSL can best be
 * utilized through the usage of static imports of the methods on the classes
 * <code>TriggerBuilder</code>, <code>JobBuilder</code>, 
 * <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> 
 * and the various <code>ScheduleBuilder</code> implementations.</p>
 * 
 * <p>Client code can then use the DSL to write code such as this:</p>
 * <pre>
 *         JobDetail job = newJob(MyJob.class)
 *             .withIdentity("myJob")
 *             .build();
 *             
 *         Trigger trigger = newTrigger() 
 *             .withIdentity(triggerKey("myTrigger", "myTriggerGroup"))
 *             .withSchedule(simpleSchedule()
 *                 .withIntervalInHours(1)
 *                 .repeatForever())
 *             .startAt(futureDate(10, MINUTES))
 *             .build();
 *         
 *         scheduler.scheduleJob(job, trigger);
 * <pre>
 *  
 * 
 * @see Trigger
 * @see Key#DEFAULT_GROUP
 */
public final class TriggerKey extends Key<TriggerKey> {
  
    private static final long serialVersionUID = 8070357886703449660L;

    public TriggerKey(String name) {
        super(name, null);
    }

    public TriggerKey(String name, String group) {
        super(name, group);
    }

    public static TriggerKey triggerKey(String name) {
        return new TriggerKey(name, null);
    }
    
    public static TriggerKey triggerKey(String name, String group) {
        return new TriggerKey(name, group);
    }
    
}
