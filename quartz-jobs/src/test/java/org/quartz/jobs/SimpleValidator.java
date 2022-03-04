package org.quartz.jobs;

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.MatcherAssert;
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
            MatcherAssert.assertThat(username, equalTo("realusername"));
            MatcherAssert.assertThat(password, equalTo("realpassword"));
        } catch (Throwable e) {
            error = new LoginFailedException(e.getMessage());
            throw error;
        }
    }
}
