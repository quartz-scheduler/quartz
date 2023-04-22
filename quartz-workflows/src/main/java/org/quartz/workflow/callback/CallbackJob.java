/*
 * Copyright 2023 Dimitry Polivaev, Unite
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quartz.workflow.callback;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.workflow.AllRecoveryAttemptsFailedException;
import org.quartz.workflow.JobRecovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallbackJob implements Job {

    interface Connector {
        URLConnection connect(URL url) throws IOException;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackJob.class);
    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }
    private static final int SERVICE_UNAVAILABLE = 503;
    public static final int DEFAULT_RECOVERY_ATTEMPTS = 30;
    public static final long DEFAULT_RECOVERY_DELAY = 1000*60*5;
    private static String formatCall(String method, URL url) {
        return method != null ?  method + " " + url : url.toString();
    }

    static CallbackJob forTest(JobRecovery.Factory jobRecoveryBuilder, Connector connect) {
        return new CallbackJob(jobRecoveryBuilder, connect);
    }

    private final JobRecovery.Factory jobRecoveryBuilder;
    private final Connector connector;

    public CallbackJob() {
        this(JobRecovery::new, URL::openConnection);
    }

    private CallbackJob(JobRecovery.Factory jobRecoveryBuilder, Connector connector) {
        super();
        this.jobRecoveryBuilder = jobRecoveryBuilder;
        this.connector = connector;
    }

    //** @VisibleForTesting
    static String CALLBACK_URL = CallbackJob.class.getSimpleName() + ".CallbackURL";

    //** @VisibleForTesting
    static String CALLBACK_HTTP_METHOD = CallbackJob.class.getSimpleName() + ".CallbackHttpMethod";

    //** @VisibleForTesting
    static String CALLBACK_RECOVERY_ATTEMPTS = CallbackJob.class.getSimpleName() + ".CallbackRecoveryAttempts";

    //** @VisibleForTesting
    static String CALLBACK_RECOVERY_DELAY = CallbackJob.class.getSimpleName() + ".CallbackRecoveryDelay";

    public static JobDetail with(String callbackUrl, HttpMethod method) {
        return with(callbackUrl, method, DEFAULT_RECOVERY_ATTEMPTS, DEFAULT_RECOVERY_DELAY);
    }

    public static JobDetail with(String callbackUrl, HttpMethod method, int recoveryAttempts, long recoveryDelay) {
        Objects.requireNonNull(callbackUrl);
        JobBuilder builder = JobBuilder.newJob(CallbackJob.class)
                .usingJobData(CALLBACK_URL, callbackUrl);
        if(method != null)
            builder.usingJobData(CALLBACK_HTTP_METHOD, method.name());
        return builder
                .usingJobData(CALLBACK_RECOVERY_ATTEMPTS, recoveryAttempts)
                .usingJobData(CALLBACK_RECOVERY_DELAY, recoveryDelay)
                .requestRecovery()
                .build();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        URL url = getCallbackUrl(data);
        String method = data.getString(CALLBACK_HTTP_METHOD);
        int responseCode = call(url, method);
        if(responseCode >= 500) {
            try {
                int recoveryAttempts = data.getInt(CALLBACK_RECOVERY_ATTEMPTS);
                long recoveryDelay = data.getLong(CALLBACK_RECOVERY_DELAY);

                LOGGER.warn("Request another callback attempt"
                        + " " + method + " " + url
                        +  " after recieving http status code " + responseCode
                        + " in " + recoveryDelay/1000 + " seconds");
                jobRecoveryBuilder.create(context, recoveryAttempts, recoveryDelay).start();
            } catch (AllRecoveryAttemptsFailedException e) {
                LOGGER.warn("All recovery attempts failed", e);
                throw new UnexpectedCallbackResponseException(formatCall(method, url), responseCode);
            }
        } else if (responseCode >= 300)
            throw new UnexpectedCallbackResponseException(formatCall(method, url), responseCode);
    }

    private int call(URL url, String method) throws JobExecutionException {
        HttpURLConnection connection;
        try {
            LOGGER.info("Callback to " + url);
            connection = (HttpURLConnection) connector.connect(url);
        } catch (IOException e) {
            throw new JobExecutionException(e);
        }
        if(method != null)
            try {
                connection.setRequestMethod(method);
                connection.setInstanceFollowRedirects(HttpMethod.GET.name().equals(method));
            } catch (ProtocolException e) {
                throw new JobExecutionException(e);
            }
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            return SERVICE_UNAVAILABLE;
        }
    }

    private URL getCallbackUrl(JobDataMap data) throws JobExecutionException {
        String callbackUrlSpec = data.getString(CALLBACK_URL);
        Objects.requireNonNull(callbackUrlSpec);
        try {
            return new URL(callbackUrlSpec);
        } catch (MalformedURLException e) {
            throw new JobExecutionException(e);
        }
    }

}
