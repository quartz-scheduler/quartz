package org.quartz.integrations.tests;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.StdRowLockSemaphore;

public class StdRowLockSemaphoreTest extends QuartzDatabaseTestSupport {
  static volatile boolean myLockInvoked = false;
  static volatile int maxRetry = -1;
  static volatile long retryPeriod = -1;
  static CountDownLatch latch = new CountDownLatch(1);

  public static class MyLock extends StdRowLockSemaphore {
    @Override
    protected void executeSQL(Connection conn, String lockName, String expandedSQL,
        String expandedInsertSQL) throws LockException {
      myLockInvoked = true;
      maxRetry = getMaxRetry();
      retryPeriod = getRetryPeriod();
      super.executeSQL(conn, lockName, expandedSQL, expandedInsertSQL);
      latch.countDown();
    }
  }

  @Override
  public void initSchedulerBeforeTest() throws Exception {
    // Override to use initSchedulerBeforeTest(Properties) instead.
  }

  public void initSchedulerBeforeTest(Properties properties) throws Exception {
    SchedulerFactory sf = new StdSchedulerFactory(properties);
    scheduler = sf.getScheduler();
    afterSchedulerInit();
  }

  Properties createDefaultProperties() {
    Properties props = super.createSchedulerProperties();
    props.setProperty("org.quartz.jobStore.lockHandler.class", "org.quartz.integrations.tests.StdRowLockSemaphoreTest$MyLock");
    props.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");
    return props;
  }

  Properties createMyLockProperties() {
    Properties props = super.createSchedulerProperties();
    props.setProperty("org.quartz.jobStore.lockHandler.class", "org.quartz.integrations.tests.StdRowLockSemaphoreTest$MyLock");
    props.setProperty("org.quartz.jobStore.lockHandler.maxRetry", "7");
    props.setProperty("org.quartz.jobStore.lockHandler.retryPeriod", "3000");
    props.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");
    return props;
  }

  @Test
  public void testDefaultStdRowLockSemaphore() throws Exception {
    initSchedulerBeforeTest(createDefaultProperties());

    JobDetail job1 = JobBuilder.newJob(HelloJob.class).withIdentity("job1").
        build();

    HashSet<Trigger> triggers = new HashSet<>();
    triggers.add(TriggerBuilder.newTrigger().forJob(job1)
        .build());

    scheduler.scheduleJob(job1, triggers, true);

    latch.await(1L, TimeUnit.MINUTES);

    Assert.assertThat(myLockInvoked, Matchers.is(true));
    Assert.assertThat(maxRetry, Matchers.is(3));
    Assert.assertThat(retryPeriod, Matchers.is(1000L));
  }

  @Test
  public void testCustomStdRowLockSemaphore() throws Exception {
    initSchedulerBeforeTest(createMyLockProperties());

    JobDetail job1 = JobBuilder.newJob(HelloJob.class).withIdentity("job1").
            build();

    HashSet<Trigger> triggers = new HashSet<>();
    triggers.add(TriggerBuilder.newTrigger().forJob(job1)
        .build());

    scheduler.scheduleJob(job1, triggers, true);

    latch.await(1L, TimeUnit.MINUTES);

    Assert.assertThat(myLockInvoked, Matchers.is(true));
    Assert.assertThat(maxRetry, Matchers.is(7));
    Assert.assertThat(retryPeriod, Matchers.is(3000L));
  }
}
