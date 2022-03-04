package org.quartz.jobs;

import static org.hamcrest.CoreMatchers.nullValue;

import org.hamcrest.MatcherAssert;

public class SendMailJobRealAuth extends SendMailJobAuthTestBase {
    public SendMailJobRealAuth() {
        super("real@host.name", "realusername", "realpassword");
    }

    @Override
    public void assertAuthentication() throws Exception {
        MatcherAssert.assertThat(this.jobListener.jobException, nullValue());
        MatcherAssert.assertThat(this.simpleValidator.error, nullValue());
    }

}
