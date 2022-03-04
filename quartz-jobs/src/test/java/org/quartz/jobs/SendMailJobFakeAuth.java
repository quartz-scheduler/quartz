package org.quartz.jobs;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

import org.hamcrest.MatcherAssert;
import org.subethamail.smtp.auth.LoginFailedException;

public class SendMailJobFakeAuth extends SendMailJobAuthTestBase {
    public SendMailJobFakeAuth() {
        super("fake@host.name", "fakeusername", "fakepassword");
    }
    
    @Override
    public void assertAuthentication() throws Exception {
        MatcherAssert.assertThat(this.jobListener.jobException, notNullValue());
        MatcherAssert.assertThat(this.simpleValidator.error, instanceOf(LoginFailedException.class));
    }

}
