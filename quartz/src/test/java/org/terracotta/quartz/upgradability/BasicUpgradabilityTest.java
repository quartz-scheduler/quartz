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
package org.terracotta.quartz.upgradability;

import java.util.Properties;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.quartz.AbstractTerracottaJobStore;
import org.terracotta.quartz.TerracottaJobStore;
import org.terracotta.quartz.collections.TimeTrigger;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import static org.mockito.Mockito.*;
import static org.terracotta.upgradability.interaction.MockToolkitFactoryService.allowNonPersistentInteractions;
import static org.terracotta.upgradability.interaction.MockToolkitFactoryService.mockToolkitFor;

/**
 *
 * @author cdennis
 */
public class BasicUpgradabilityTest {
  
  @Test
  public void testJobStorage() throws Exception {
    ToolkitInternal mock = mockToolkitFor("mocked-not-clustered");

    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/org/quartz/quartz.properties"));
    props.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, TerracottaJobStore.class.getName());
    props.setProperty(AbstractTerracottaJobStore.TC_CONFIGURL_PROP, "mocked-not-clustered");

    SchedulerFactory schedFact = new StdSchedulerFactory(props);
    Scheduler scheduler = schedFact.getScheduler();
    try {
      scheduler.start();
      
      JobDetail jobDetail = JobBuilder.newJob(SomeJob.class).withIdentity("testjob", "testjobgroup").storeDurably().build();
      scheduler.addJob(jobDetail, false);
      
      Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail).withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10)).build();
      scheduler.scheduleJob(trigger);
    } finally {
      scheduler.shutdown();
    }

    verify(mock).getStore(eq("_tc_quartz_jobs|DefaultQuartzScheduler"),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    verify(mock).getStore(eq("_tc_quartz_triggers|DefaultQuartzScheduler"),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    verify(mock).getStore(eq("_tc_quartz_fired_trigger|DefaultQuartzScheduler"),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    verify(mock).getStore(eq("_tc_quartz_calendar_wrapper|DefaultQuartzScheduler"),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    
    verify(mock).getSet("_tc_quartz_grp_names|DefaultQuartzScheduler", String.class);
    verify(mock).getSet("_tc_quartz_grp_paused_names|DefaultQuartzScheduler", String.class);
    verify(mock).getSet("_tc_quartz_blocked_jobs|DefaultQuartzScheduler", JobKey.class);
    verify(mock).getSet("_tc_quartz_grp_names_triggers|DefaultQuartzScheduler", String.class);
    verify(mock).getSet("_tc_quartz_grp_paused_trogger_names|DefaultQuartzScheduler", String.class);
    verify(mock).getSet("_tc_quartz_grp_jobs_testjobgroup|DefaultQuartzScheduler", String.class);
    verify(mock).getSet("_tc_quartz_grp_triggers_DEFAULT|DefaultQuartzScheduler", String.class);

    verify(mock).getSortedSet("_tc_time_trigger_sorted_set|DefaultQuartzScheduler", TimeTrigger.class);
    verify(mock).shutdown();
    
    allowNonPersistentInteractions(mock);
    verifyNoMoreInteractions(mock);
  }
    
  static class SomeJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      //no-op
    }
  }
}
