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

package org.quartz.simpl;

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * This is class is a simple implementation of a java 21 virtual thread pool, based on the
 * <code>{@link ThreadPool}</code> interface.
 * </p>
 * 
 * <p>
 * <CODE>Runnable</CODE> objects are sent to the pool with the <code>{@link #runInThread(Runnable)}</code>
 * method, which blocks until a <code>Thread</code> becomes available.
 * </p>
 * 
 * <p>
 * The pool has unlimited number of virtual <code>Thread</code>
 * </p>
 * 
 * @author Nabarun Mondal
 *
 */
public class SimpleVirtualThreadPool implements ThreadPool {

    private static final Logger log = LoggerFactory.getLogger( SimpleVirtualThreadPool.class );

    private static <T> T safeCall(Callable<T> callable , String message ){
        try{
            return callable.call();
        }catch (Throwable ex){
            log.warn( message + ex);
            return null;
        }
    }

    private static Method getVirtualExecutorCreator(){
        return safeCall(() -> Executors.class.getMethod("newVirtualThreadPerTaskExecutor"),
                "Runtime does not have support for java 21 virtual threads : ");
    }

    private static final Method SERVICE_CREATOR = getVirtualExecutorCreator();

    public static boolean isVirtualThreadSupported(){
        return SERVICE_CREATOR != null;
    }

    private static ExecutorService getVirtualExecutor(){
        return safeCall( () ->(ExecutorService)SERVICE_CREATOR.invoke(null),
                "Failed to create Virtual Executor Service! Possibly Runtime has no support for Virtual Threads : ");
    }
    private  ExecutorService executorService = null ;

    public SimpleVirtualThreadPool(){
        if ( !isVirtualThreadSupported() ) throw new UnsupportedOperationException( "Virtual Threads are not supported!" );
    }

    @Override
    public boolean runInThread(Runnable runnable) {
        if ( executorService != null ){
            executorService.submit(runnable);
            return true;
        }
        return false;
    }

    @Override
    public int blockForAvailableThreads() {
        return Integer.MAX_VALUE ; // does not block, there is potentially no limit
    }

    @Override
    public void initialize() throws SchedulerConfigException {
        executorService = getVirtualExecutor();
        if ( executorService == null ) throw new SchedulerConfigException("Virtual Threads are not supported!");
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        if ( waitForJobsToComplete ){
            executorService.shutdown();
        } else {
            executorService.shutdownNow();
        }
    }

    @Override
    public int getPoolSize() {
        return Integer.MAX_VALUE ; // because it is unlimited
    }

    @Override
    public void setInstanceId(String schedInstId) {}
    @Override
    public void setInstanceName(String schedName) {}
}
