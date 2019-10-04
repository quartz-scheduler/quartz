package org.quartz.jobs;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.auth.UsernamePasswordValidator;

class SimpleValidator implements UsernamePasswordValidator {
    public LoginFailedException error;

    @Override
    public void login(String username, String password)
            throws LoginFailedException {
        System.out.println("UsernamePasswordValidator: login username '"
                + username + "' password '" + password + "'");
        try {
            assertThat(username, equalTo("realusername"));
            assertThat(password, equalTo("realpassword"));
        } catch (Throwable e) {
            error = new LoginFailedException(e.getMessage());
            throw error;
        }
    }
}
