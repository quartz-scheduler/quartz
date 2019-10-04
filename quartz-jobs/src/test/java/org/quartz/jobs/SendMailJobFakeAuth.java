package org.quartz.jobs;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.subethamail.smtp.auth.LoginFailedException;

public class SendMailJobFakeAuth extends SendMailJobAuthTestBase {
    public SendMailJobFakeAuth() {
        super("fake@host.name", "fakeusername", "fakepassword");
    }
    
    @Override
    public void assertAuthentication() throws Exception {
        assertThat(this.jobListener.jobException, notNullValue());
        assertThat(this.simpleValidator.error, instanceOf(LoginFailedException.class));
    }

}
