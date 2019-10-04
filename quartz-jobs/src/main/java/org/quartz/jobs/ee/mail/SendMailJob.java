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

package org.quartz.jobs.ee.mail;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * <p>
 * A Job which sends an e-mail with the configured content to the configured
 * recipient.
 * 
 * Arbitrary mail.smtp.xxx settings can be added to job data and they will be
 * passed along the mail session
 * </p>
 * 
 * @author James House
 */
public class SendMailJob implements Job {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * The host name of the smtp server. REQUIRED.
     */
    public static final String PROP_SMTP_HOST = "smtp_host";

    /**
     * The e-mail address to send the mail to. REQUIRED.
     */
    public static final String PROP_RECIPIENT = "recipient";

    /**
     * The e-mail address to cc the mail to. Optional.
     */
    public static final String PROP_CC_RECIPIENT = "cc_recipient";

    /**
     * The e-mail address to claim the mail is from. REQUIRED.
     */
    public static final String PROP_SENDER = "sender";

    /**
     * The e-mail address the message should say to reply to. Optional.
     */
    public static final String PROP_REPLY_TO = "reply_to";

    /**
     * The subject to place on the e-mail. REQUIRED.
     */
    public static final String PROP_SUBJECT = "subject";

    /**
     * The e-mail message body. REQUIRED.
     */
    public static final String PROP_MESSAGE = "message";

    /**
     * The message content type. For example, "text/html". Optional.
     */
    public static final String PROP_CONTENT_TYPE = "content_type";
    
    /**
     * Username for authenticated session. Password must also be set if username is used. Optional.
     */
    public static final String PROP_USERNAME = "username";
    
    /**
     * Password for authenticated session. Optional.
     */
    public static final String PROP_PASSWORD = "password";    

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context)
        throws JobExecutionException {

        JobDataMap data = context.getMergedJobDataMap();

        MailInfo mailInfo = populateMailInfo(data, createMailInfo());
        
        getLog().info("Sending message " + mailInfo);

        try {
            MimeMessage mimeMessage = prepareMimeMessage(mailInfo);
            
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            throw new JobExecutionException("Unable to send mail: " + mailInfo,
                    e, false);
        }

    }

    protected Logger getLog() {
        return log;
    }

    protected MimeMessage prepareMimeMessage(MailInfo mailInfo)
        throws MessagingException {
        Session session = getMailSession(mailInfo);

        MimeMessage mimeMessage = new MimeMessage(session);

        Address[] toAddresses = InternetAddress.parse(mailInfo.getTo());
        mimeMessage.setRecipients(Message.RecipientType.TO, toAddresses);

        if (mailInfo.getCc() != null) {
            Address[] ccAddresses = InternetAddress.parse(mailInfo.getCc());
            mimeMessage.setRecipients(Message.RecipientType.CC, ccAddresses);
        }

        mimeMessage.setFrom(new InternetAddress(mailInfo.getFrom()));
        
        if (mailInfo.getReplyTo() != null) {
            mimeMessage.setReplyTo(new InternetAddress[]{new InternetAddress(mailInfo.getReplyTo())});
        }
        
        mimeMessage.setSubject(mailInfo.getSubject());
        
        mimeMessage.setSentDate(new Date());

        setMimeMessageContent(mimeMessage, mailInfo);

        return mimeMessage;
    }
    
    protected void setMimeMessageContent(MimeMessage mimeMessage, MailInfo mailInfo) 
        throws MessagingException {
        if (mailInfo.getContentType() == null) {
            mimeMessage.setText(mailInfo.getMessage());
        } else {
            mimeMessage.setContent(mailInfo.getMessage(), mailInfo.getContentType());
        }
    }

    protected Session getMailSession(final MailInfo mailInfo) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", mailInfo.getSmtpHost());
        
        // pass along extra smtp settings from users
        Properties extraSettings = mailInfo.getSmtpProperties();
        if (extraSettings != null) {
            properties.putAll(extraSettings);
        }
        
        Authenticator authenticator = null;
        if (mailInfo.getUsername() != null && mailInfo.getPassword() != null) {
            log.info("using username '{}' and password 'xxx'", mailInfo.getUsername());
            authenticator = new Authenticator() { 
                protected PasswordAuthentication getPasswordAuthentication() { 
                    return new PasswordAuthentication(mailInfo.getUsername(), mailInfo.getPassword()); 
                }
            };
        }
        log.debug("Sending mail with properties: {}", properties);
        return Session.getDefaultInstance(properties, authenticator);
    }
    
    protected MailInfo createMailInfo() {
        return new MailInfo();
    }
    
    protected MailInfo populateMailInfo(JobDataMap data, MailInfo mailInfo) {
        // Required parameters
        mailInfo.setSmtpHost(getRequiredParm(data, PROP_SMTP_HOST, "PROP_SMTP_HOST"));
        mailInfo.setTo(getRequiredParm(data, PROP_RECIPIENT, "PROP_RECIPIENT"));
        mailInfo.setFrom(getRequiredParm(data, PROP_SENDER, "PROP_SENDER"));
        mailInfo.setSubject(getRequiredParm(data, PROP_SUBJECT, "PROP_SUBJECT"));
        mailInfo.setMessage(getRequiredParm(data, PROP_MESSAGE, "PROP_MESSAGE"));
        
        // Optional parameters
        mailInfo.setReplyTo(getOptionalParm(data, PROP_REPLY_TO));
        mailInfo.setCc(getOptionalParm(data, PROP_CC_RECIPIENT));
        mailInfo.setContentType(getOptionalParm(data, PROP_CONTENT_TYPE));
        mailInfo.setUsername(getOptionalParm(data, PROP_USERNAME));
        mailInfo.setPassword(getOptionalParm(data, PROP_PASSWORD));
        
        // extra mail.smtp. properties from user
        Properties smtpProperties = new Properties();
        for (String key : data.keySet()) {
            if (key.startsWith("mail.smtp.")) {
                smtpProperties.put(key, data.getString(key));
            }
        }
        if (mailInfo.getSmtpProperties() == null) {
            mailInfo.setSmtpProperties(smtpProperties);
        } else {
            mailInfo.getSmtpProperties().putAll(smtpProperties);
        }

        
        return mailInfo;
    }
    
    
    protected String getRequiredParm(JobDataMap data, String property, String constantName) {
        String value = getOptionalParm(data, property);
        
        if (value == null) {
            throw new IllegalArgumentException(constantName + " not specified.");
        }
        
        return value;
    }
    
    protected String getOptionalParm(JobDataMap data, String property) {
        String value = data.getString(property);
        
        if ((value != null) && (value.trim().length() == 0)) {
            return null;
        }
        
        return value;
    }
    
    protected static class MailInfo {
        private String smtpHost;
        private String to;
        private String from;
        private String subject;
        private String message;
        private String replyTo;
        private String cc;
        private String contentType;
        private String username;
        private String password;
        private Properties smtpProperties;

        @Override
        public String toString() {
            return "'" + getSubject() + "' to: " + getTo();
        }
        
        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        public String getSmtpHost() {
            return smtpHost;
        }

        public void setSmtpHost(String smtpHost) {
            this.smtpHost = smtpHost;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
        
        public Properties getSmtpProperties() {
            return smtpProperties;
        }
        
        public void setSmtpProperties(Properties smtpProperties) {
            this.smtpProperties = smtpProperties;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
