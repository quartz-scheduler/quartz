package org.quartz.jobs;

import org.junit.Ignore;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@Ignore
public class SendMailJobRealAuth extends SendMailJobAuthTestBase {
    public SendMailJobRealAuth() {
        super("real@host.name", "realusername", "realpassword");
    }

    @Override
    public void assertAuthentication() throws Exception {
        assertThat(this.jobListener.jobException, nullValue());
        assertThat(this.simpleValidator.error, nullValue());
    }

}
