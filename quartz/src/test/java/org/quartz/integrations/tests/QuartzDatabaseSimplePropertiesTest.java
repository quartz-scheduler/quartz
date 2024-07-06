package org.quartz.integrations.tests;

import java.math.BigDecimal;
import java.util.Properties;

import org.junit.Test;

import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.SimplePropertiesTriggerPersistenceDelegateSupport;
import org.quartz.impl.jdbcjobstore.SimplePropertiesTriggerProperties;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.MutableTrigger;
import org.quartz.spi.OperableTrigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzDatabaseSimplePropertiesTest extends QuartzDatabaseTestSupport {

	@Override
	protected Properties createSchedulerProperties() {
		Properties defaultSchedulerProperties = super.createSchedulerProperties();
		defaultSchedulerProperties.put(
				"org.quartz.jobStore.driverDelegateInitString",
				"triggerPersistenceDelegateClasses=" + TestSimplePropertiesTriggerPersistenceDelegate.class.getName()
		);
		return defaultSchedulerProperties;
	}

	@Test
	public void insertAndLoadProperties() throws SchedulerException {
		TriggerKey triggerKey = new TriggerKey("test");
		TestTriggerImpl trigger = (TestTriggerImpl) newTrigger()
				.withIdentity(triggerKey)
				.withSchedule(new TestTriggerScheduleBuilder())
				.build();

		SimplePropertiesTriggerProperties insertedProperties = new SimplePropertiesTriggerProperties();
		insertedProperties.setString1("");
		insertedProperties.setString2(null);
		insertedProperties.setString3("test-string");
		insertedProperties.setInt1(Integer.MIN_VALUE);
		insertedProperties.setInt2(Integer.MAX_VALUE);
		insertedProperties.setLong1(Long.MIN_VALUE);
		insertedProperties.setLong2(Long.MAX_VALUE);
		insertedProperties.setDecimal1(new BigDecimal("0.0000"));
		insertedProperties.setDecimal2(new BigDecimal("-10.0000"));
		insertedProperties.setBoolean1(true);
		insertedProperties.setBoolean2(false);
		trigger.setAdditionalProperties(insertedProperties);

		JobDetail job = newJob(HelloJob.class).withIdentity("test").build();

		scheduler.scheduleJob(job, trigger);
		TestTriggerImpl loadedTrigger = ((TestTriggerImpl) scheduler.getTrigger(triggerKey));

		SimplePropertiesTriggerProperties loadedProperties = loadedTrigger.getAdditionalProperties();
		assertThat(loadedProperties, samePropertyValuesAs(insertedProperties));
	}

	public static class TestTriggerImpl extends SimpleTriggerImpl {
		private SimplePropertiesTriggerProperties additionalProperties;

		@Override
		public boolean hasAdditionalProperties() {
			return true;
		}

		public SimplePropertiesTriggerProperties getAdditionalProperties() {
			return additionalProperties;
		}

		public void setAdditionalProperties(SimplePropertiesTriggerProperties additionalProperties) {
			this.additionalProperties = additionalProperties;
		}

		@Override
		public ScheduleBuilder<SimpleTrigger> getScheduleBuilder() {
			return new TestTriggerScheduleBuilder();
		}
	}

	public static class TestTriggerScheduleBuilder extends ScheduleBuilder<SimpleTrigger> {

		@Override
		protected MutableTrigger build() {
			return new TestTriggerImpl();
		}
	}

	public static class TestSimplePropertiesTriggerPersistenceDelegate extends
			SimplePropertiesTriggerPersistenceDelegateSupport {

		@Override
		public boolean canHandleTriggerType(OperableTrigger trigger) {
			return trigger instanceof TestTriggerImpl;
		}

		@Override
		public String getHandledTriggerTypeDiscriminator() {
			return "TEST";
		}

		@Override
		protected SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger trigger) {
			return ((TestTriggerImpl) trigger).getAdditionalProperties();
		}

		@Override
		protected TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties properties) {
			return new TriggerPropertyBundle(
					new TestTriggerScheduleBuilder(),
					new String[] {"additionalProperties"},
					new Object[] {properties}
			);
		}
	}
}
