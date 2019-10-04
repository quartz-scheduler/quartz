package org.quartz.jobs;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.jobs.ee.mail.SendMailJob;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

public class SendMailJobTest {
    private Wiser wiser;
    private Scheduler scheduler;
    private MyJobListener jobListener;

    @Before
    public void setup() throws Exception {
        wiser = new Wiser();
        wiser.setPort(2500);
        wiser.start();
        jobListener = new MyJobListener();
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.getListenerManager().addJobListener(jobListener);
    }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown();
        wiser.stop();
    }

    @Test
    public void testSendMailJobNoAuthentication() throws Exception {
        JobDetail job = newJob(SendMailJob.class)
                .withIdentity("job1", "group1").build();

        JobDataMap jobData = job.getJobDataMap();
        jobData.put(SendMailJob.PROP_SMTP_HOST, "localhost");
        jobData.put(SendMailJob.PROP_SENDER, "sender@host.com");
        jobData.put(SendMailJob.PROP_RECIPIENT, "receiver@host.com");
        jobData.put(SendMailJob.PROP_SUBJECT, "test subject");
        jobData.put(SendMailJob.PROP_MESSAGE, "do not reply");
        jobData.put("mail.smtp.port", "2500");

        SimpleTrigger trigger = newTrigger()
                .withIdentity("trigger1", "group1")
                .startNow()
                .withSchedule(
                        simpleSchedule().withIntervalInSeconds(120)
                                .withRepeatCount(1)).build();

        scheduler.scheduleJob(job, trigger);
        scheduler.start();
        
        jobListener.barrier.await(30, TimeUnit.SECONDS);

        assertThat(wiser.getMessages().size(), equalTo(1));

        WiserMessage message = wiser.getMessages().get(0);
        System.out.println(message);
        System.out.println(message.getMimeMessage().getSubject());
        assertThat(message.getEnvelopeSender(), equalTo("sender@host.com"));
        assertThat(message.getEnvelopeReceiver(), equalTo("receiver@host.com"));
        assertThat(message.getMimeMessage().getSubject(), equalTo("test subject"));
        assertThat(IOUtils.toString(message.getMimeMessage().getInputStream())
                .trim(), equalTo("do not reply"));
    }

    /**
     * replace xxx with your real credentials
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void testRealAccountSendMail() throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.debug", "true");
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("xxx", "xxx");
                    }
                });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("xxx@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("xxx@gmail.com"));
            message.setSubject("Test Subject");
            message.setText("Test message");
            Transport.send(message);
            System.out.println("Sent");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
