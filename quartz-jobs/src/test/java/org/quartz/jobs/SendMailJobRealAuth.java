package org.quartz.jobs;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
