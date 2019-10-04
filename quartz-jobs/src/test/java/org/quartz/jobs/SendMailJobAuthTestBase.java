package org.quartz.jobs;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.jobs.ee.mail.SendMailJob;
import org.subethamail.smtp.auth.PlainAuthenticationHandlerFactory;
import org.subethamail.wiser.Wiser;

public abstract class SendMailJobAuthTestBase {
    private Wiser wiser;
    private Scheduler scheduler;
    protected SimpleValidator simpleValidator;
    protected MyJobListener jobListener;
    private final String sender;
    private final String username;
    private final String password;
    
    public SendMailJobAuthTestBase(String sender, String username, String password) {
        this.sender = sender;
        this.username = username;
        this.password = password;
    }

    @Before
    public void setUp() throws Exception {
        simpleValidator = new SimpleValidator();
        wiser = new Wiser(2500);
        wiser.getServer()
                .setAuthenticationHandlerFactory(new PlainAuthenticationHandlerFactory(
                        simpleValidator));
        wiser.start();
        
        // set up scheduler
        jobListener = new MyJobListener();
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.getListenerManager().addJobListener(jobListener);
    }

    @After
    public void tearDown() throws Exception {
        wiser.stop();
        scheduler.shutdown(true);
    }

    @Test
    public void testWithAuthentication() throws Exception {
        JobDetail job = newJob(SendMailJob.class)
                .withIdentity("job1", "group1").build();

        configureSendMailJob(job);

        Trigger trigger = newTrigger().withIdentity("trigger1", "group1")
                .startNow().build();

        scheduler.scheduleJob(job, trigger);
        scheduler.start();

        jobListener.barrier.await(30, TimeUnit.SECONDS);
        
        assertAuthentication();
    }
    
    public abstract void assertAuthentication() throws Exception;

    protected void configureSendMailJob(JobDetail job) {
        JobDataMap jobData = job.getJobDataMap();
        jobData.put(SendMailJob.PROP_SMTP_HOST, "localhost");
        jobData.put(SendMailJob.PROP_SENDER, sender);
        jobData.put(SendMailJob.PROP_RECIPIENT, "receiver@host.com");
        jobData.put(SendMailJob.PROP_SUBJECT, "test subject");
        jobData.put(SendMailJob.PROP_MESSAGE, "do not reply");
        jobData.put(SendMailJob.PROP_USERNAME, username);
        jobData.put(SendMailJob.PROP_PASSWORD, password);
        jobData.put("mail.smtp.port", "2500");
        jobData.put("mail.smtp.auth", "true");
    }
}
