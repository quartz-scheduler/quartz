package org.quartz.xml;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.repeatHourlyForever;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.jobs.NoOpJob;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.xml.sax.SAXParseException;

/**
 * Unit test for XMLSchedulingDataProcessor.
 *
 * @author Zemian Deng
 * @author Tomasz Nurkiewicz (QTZ-273)
 */
public class XMLSchedulingDataProcessorTest extends TestCase {
	
	/** QTZ-185
	 * <p>The default XMLSchedulingDataProcessor will setOverWriteExistingData(true), and we want to
	 * test programmatically overriding this value.
	 * 
	 * <p>Note that XMLSchedulingDataProcessor#processFileAndScheduleJobs(Scheduler,boolean) will only
	 * read default "quartz_data.xml" in current working directory. So to test this, we must create
	 * this file. If this file already exist, it will be overwritten! 
	 */
	public void testOverwriteFlag() throws Exception {
		//Prepare a quartz_data.xml in current working directory by copy a test case file.
		File file = new File(XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME);
		copyResourceToFile("/org/quartz/xml/simple-job-trigger.xml", file);
		
		Scheduler scheduler = null;
		try {
			StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-test.properties");
			scheduler = factory.getScheduler();
			
			// Let's setup a fixture job data that we know test is not going modify it.
			JobDetail job = newJob(NoOpJob.class).withIdentity("job1").usingJobData("foo", "dont_chg_me").build();
			Trigger trigger = newTrigger().withIdentity("job1").withSchedule(repeatHourlyForever()).build();
			scheduler.scheduleJob(job, trigger);			
			
			ClassLoadHelper clhelper = new CascadingClassLoadHelper();
			clhelper.initialize();
			XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(clhelper);
			try {
				processor.processFileAndScheduleJobs(scheduler, false);
				fail("OverWriteExisting flag didn't work. We should get Exception when overwrite is set to false.");
			} catch (ObjectAlreadyExistsException e) {
				// This is expected. Do nothing.
			}
			
			// We should still have what we start with.
			assertEquals(1, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("DEFAULT")).size());
			assertEquals(1, scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("DEFAULT")).size());
			
			job = scheduler.getJobDetail(JobKey.jobKey("job1"));
			String fooValue = job.getJobDataMap().getString("foo");
			assertEquals("dont_chg_me", fooValue);
		} finally {
			// remove test file
			if(file.exists() && !file.delete())
				throw new RuntimeException("Failed to remove test file " + file);
			
			// shutdown scheduler
			if (scheduler != null)
				scheduler.shutdown();
		}
	}
	
	private void copyResourceToFile(String resName, File file) throws IOException {
		// Copy streams
		InputStream inStream = null;
		FileOutputStream outStream = null;
		try {
			// Copy input resource stream to output file.
			inStream = getClass().getResourceAsStream(resName);
			outStream = new FileOutputStream(file);
			
			int BLOCK_SIZE = 1024 * 1024 * 5; // 5 MB
			byte[] buffer = new byte[BLOCK_SIZE];
			int len = -1;
			while ((len = inStream.read(buffer, 0, BLOCK_SIZE)) != -1) {
				outStream.write(buffer, 0, len);
			}
		} finally {
			if (outStream != null)
				outStream.close();
			if (inStream != null)
				inStream.close();
		}
	}

	public void testXmlParserConfiguration() throws Exception {
		Scheduler scheduler = null;
		try {
		  StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-test.properties");
		  scheduler = factory.getScheduler();
		  ClassLoadHelper clhelper = new CascadingClassLoadHelper();
		  clhelper.initialize();
		  XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(clhelper);
		  processor.processFileAndScheduleJobs("org/quartz/xml/bad-job-config.xml", scheduler);
	
	
		  final JobKey jobKey = scheduler.getJobKeys(GroupMatcher.jobGroupEquals("native")).iterator().next();
		  final JobDetail jobDetail = scheduler.getJobDetail(jobKey);
		  final String description = jobDetail.getDescription();
	
	
		  fail("Expected parser configuration to block DOCTYPE. The following was injected into the job description field: " + description);
		} catch (SAXParseException e) {
		  assertTrue(e.getMessage().contains("DOCTYPE is disallowed"));
		} finally {
		  if (scheduler != null)
			scheduler.shutdown();
		}
	}
	
	/** QTZ-187 */
	public void testDirectivesNoOverwriteWithIgnoreDups() throws Exception {
		Scheduler scheduler = null;
		try {
			StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-test.properties");
			scheduler = factory.getScheduler();
			
			// Setup existing job with same names as in xml data.
			JobDetail job = newJob(NoOpJob.class).withIdentity("job1").build();
			Trigger trigger = newTrigger().withIdentity("job1").withSchedule(repeatHourlyForever()).build();
			scheduler.scheduleJob(job, trigger);
			
			job = newJob(NoOpJob.class).withIdentity("job2").build();
			trigger = newTrigger().withIdentity("job2").withSchedule(repeatHourlyForever()).build();
			scheduler.scheduleJob(job, trigger);
			
			// Now load the xml data with directives: overwrite-existing-data=false, ignore-duplicates=true
			ClassLoadHelper clhelper = new CascadingClassLoadHelper();
			clhelper.initialize();
			XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(clhelper);
			processor.processFileAndScheduleJobs("org/quartz/xml/directives_no-overwrite_ignoredups.xml", scheduler);
			assertEquals(2, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("DEFAULT")).size());
			assertEquals(2, scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("DEFAULT")).size());
		} finally {
			if (scheduler != null)
				scheduler.shutdown();
		}
	}
	
	/** QTZ-180 */
	public void testXsdSchemaValidationOnVariousTriggers() throws Exception {
		Scheduler scheduler = null;
		try {
			StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-test.properties");
			scheduler = factory.getScheduler();
			ClassLoadHelper clhelper = new CascadingClassLoadHelper();
			clhelper.initialize();
			XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(clhelper);
			processor.processFileAndScheduleJobs("org/quartz/xml/job-scheduling-data-2.0_trigger-samples.xml", scheduler);
			assertEquals(1, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("DEFAULT")).size());
			assertEquals(35, scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("DEFAULT")).size());
		} finally {
			if (scheduler != null)
				scheduler.shutdown();
		}
	}

   	public void testQTZ327SimpleTriggerNoRepeat() throws Exception {
   		Scheduler scheduler = null;
   		try {
   			StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-test.properties");
   			scheduler = factory.getScheduler();
   			ClassLoadHelper clhelper = new CascadingClassLoadHelper();
   			clhelper.initialize();
   			XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(clhelper);
   			processor.processFileAndScheduleJobs("org/quartz/xml/simple-job-trigger-no-repeat.xml", scheduler);
   			assertEquals(1, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("DEFAULT")).size());
   			assertEquals(1, scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("DEFAULT")).size());
   		} finally {
   			if (scheduler != null)
   				scheduler.shutdown();
   		}
   	}

	private Date dateOfGMT_UTC(int hour, int minute, int second, int dayOfMonth, int month, int year) {
		final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		calendar.set(year, month, dayOfMonth, hour, minute, second);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();

	}
	
	private Date dateOfLocalTime(int hour, int minute, int second, int dayOfMonth, int month, int year) {
		final GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(year, month, dayOfMonth, hour, minute, second);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/** QTZ-273 */
	public void testTimeZones() throws Exception {
		Scheduler scheduler = null;
		try {
			// given
			StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/xml/quartz-test.properties");
			scheduler = factory.getScheduler();
			ClassLoadHelper clhelper = new CascadingClassLoadHelper();
			clhelper.initialize();
			XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(clhelper);

			// when
			processor.processFileAndScheduleJobs("org/quartz/xml/simple-job-trigger-with-timezones.xml", scheduler);

			// then
			Trigger trigger = scheduler.getTrigger(new TriggerKey("job1", "DEFAULT"));
			assertNotNull(trigger);

			assertEquals(dateOfGMT_UTC(18, 0, 0, 1, Calendar.JANUARY, 2012), trigger.getStartTime());
			assertEquals(dateOfGMT_UTC(19, 0, 0, 1, Calendar.JANUARY, 2012), trigger.getEndTime());
			
			
			trigger = scheduler.getTrigger(new TriggerKey("job2", "DEFAULT"));
			assertNotNull(trigger);

			assertEquals(dateOfLocalTime(6, 0, 0, 1, Calendar.JANUARY, 2012), trigger.getStartTime());
			assertEquals(dateOfGMT_UTC(19, 0, 0, 1, Calendar.JANUARY, 2012), trigger.getEndTime());
		} finally {
			if (scheduler != null)
				scheduler.shutdown();
		}
	}

	/** An empty job for testing purpose. */
	public static class MyJob implements Job {
		public void execute(JobExecutionContext context) throws JobExecutionException {
			//
		}
	}
	
	
}
