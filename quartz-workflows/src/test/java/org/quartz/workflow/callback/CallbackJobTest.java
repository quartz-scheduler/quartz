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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.workflow.JobRecovery;
import org.quartz.workflow.callback.CallbackJob.HttpMethod;

@RunWith(MockitoJUnitRunner.class)
public class CallbackJobTest {
    private static final String CALLBACK_URL = "http://www.example.com";

    @Mock
    private JobRecovery.Factory jobRecoveryBuilder;

    @Mock
    private JobRecovery jobRecoveryMock;
    
    @Mock
    HttpURLConnection connectionMock;
    
    @Mock
    JobExecutionContext context;

    @Mock
    private CallbackJob.Connector connector;
    
    private CallbackJob uut;
    @Before
    public void setup() {
        uut = CallbackJob.forTest(jobRecoveryBuilder, connector);
    }
    
    @Test
    public void executesCallbackSuccesfully_responseCode200() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        when(connectionMock.getResponseCode()).thenReturn(200);
        
        uut.execute(context);
        
        verify(connector).connect(new URL(CALLBACK_URL));
        
        verify(connectionMock).setRequestMethod(HttpMethod.POST.name());
        verify(connectionMock).setInstanceFollowRedirects(false);
        
        verifyZeroInteractions(jobRecoveryBuilder, jobRecoveryMock);
    }

    
    @Test
    public void executesCallback_responseCode500_schedulesRecovery() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        when(connectionMock.getResponseCode()).thenReturn(500);
        when(jobRecoveryBuilder.create(context, CallbackJob.DEFAULT_RECOVERY_ATTEMPTS, CallbackJob.DEFAULT_RECOVERY_DELAY)).thenReturn(jobRecoveryMock);
        
        uut.execute(context);
        
        verify(jobRecoveryBuilder).create(context, CallbackJob.DEFAULT_RECOVERY_ATTEMPTS, CallbackJob.DEFAULT_RECOVERY_DELAY);
        verify(jobRecoveryMock).start();
    }
    
    @Test
    public void executesCallback_responseCode500_noRecoveryAttemptsLeft() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        data.put(CallbackJob.CALLBACK_RECOVERY_ATTEMPTS, 0);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        when(connectionMock.getResponseCode()).thenReturn(500);
        when(jobRecoveryBuilder.create(context, 0, CallbackJob.DEFAULT_RECOVERY_DELAY))
            .thenReturn(jobRecoveryMock);
        doThrow(UnexpectedCallbackResponseException.class).when(jobRecoveryMock).start();
        
        assertThatThrownBy(() -> uut.execute(context)).isInstanceOf(UnexpectedCallbackResponseException.class);
    }
    
    
    @Test
    public void executesCallback_responseCode500_schedulingRecoveryFails() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        when(connectionMock.getResponseCode()).thenReturn(500);
        when(jobRecoveryBuilder.create(context, CallbackJob.DEFAULT_RECOVERY_ATTEMPTS, CallbackJob.DEFAULT_RECOVERY_DELAY)).thenReturn(jobRecoveryMock);
        doThrow(JobExecutionException.class).when(jobRecoveryMock).start();
        
        assertThatThrownBy(() -> uut.execute(context)).isExactlyInstanceOf(JobExecutionException.class);
    }
    
    @Test
    public void executesCallback_responseCode400_noRecoveryRequested() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        when(connectionMock.getResponseCode()).thenReturn(400);
        
        assertThatThrownBy(() -> uut.execute(context)).isInstanceOf(JobExecutionException.class);
        
        verifyZeroInteractions(jobRecoveryBuilder, jobRecoveryMock);
    }

    
    @Test
    public void executesCallback_invalidUrl_noRecoveryRequested() throws Exception {
        JobDataMap data = CallbackJob.with("invalid url", HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        
        assertThatThrownBy(() -> uut.execute(context)).isInstanceOf(JobExecutionException.class);
        
        verifyZeroInteractions(jobRecoveryBuilder, jobRecoveryMock);
    }
    
    @Test
    public void executesCallback_invalidMethod_noRecoveryRequested() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        doThrow(ProtocolException.class).when(connectionMock).setRequestMethod(HttpMethod.POST.name());
        
        assertThatThrownBy(() -> uut.execute(context)).isInstanceOf(JobExecutionException.class);
        
        verifyZeroInteractions(jobRecoveryBuilder, jobRecoveryMock);
    }
    
    @Test
    public void executesCallback_failsOnOpenConnection_noRecoveryRequested() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        
        when(connector.connect(any())).thenThrow(IOException.class);
        
        assertThatThrownBy(() -> uut.execute(context)).isInstanceOf(JobExecutionException.class);
        
        verifyZeroInteractions(jobRecoveryBuilder, jobRecoveryMock);
    }

    
    @Test
    public void executesCallback_failsOnRequest_recoveryRequested() throws Exception {
        JobDataMap data = CallbackJob.with(CALLBACK_URL, HttpMethod.POST).getJobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(connector.connect(new URL(CALLBACK_URL))).thenReturn(connectionMock);
        when(connectionMock.getResponseCode()).thenThrow(IOException.class);
        
        when(jobRecoveryBuilder.create(context, CallbackJob.DEFAULT_RECOVERY_ATTEMPTS, CallbackJob.DEFAULT_RECOVERY_DELAY)).thenReturn(jobRecoveryMock);
        
        uut.execute(context);
        
        verify(jobRecoveryBuilder).create(context, CallbackJob.DEFAULT_RECOVERY_ATTEMPTS, CallbackJob.DEFAULT_RECOVERY_DELAY);
        verify(jobRecoveryMock).start();
        
    }
}
