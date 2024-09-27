/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
 */
package org.quartz.listeners;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

/**
 * Holds a List of references to JobListener instances and broadcasts all
 * events to them (in order).
 *
 * <p>The broadcasting behavior of this listener to delegate listeners may be
 * more convenient than registering all of the listeners directly with the
 * Scheduler, and provides the flexibility of easily changing which listeners
 * get notified.</p>
 *
 *
 * @see #addListener(org.quartz.JobListener)
 * @see #removeListener(org.quartz.JobListener)
 * @see #removeListener(String)
 *
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastJobListener implements JobListener {

    private String name;
    private List<JobListener> listeners;

    /**
     * Construct an instance with the given name.
     *
     * (Remember to add some delegate listeners!)
     *
     * @param name the name of this instance
     */
    public BroadcastJobListener(String name) {
        if(name == null) {
            throw new IllegalArgumentException("Listener name cannot be null!");
        }
        this.name = name;
        listeners = new LinkedList<JobListener>();
    }

    /**
     * Construct an instance with the given name, and List of listeners.
     *
     * @param name the name of this instance
     * @param listeners the initial List of JobListeners to broadcast to.
     */
    public BroadcastJobListener(String name, List<JobListener> listeners) {
        this(name);
        this.listeners.addAll(listeners);
    }

    public String getName() {
        return name;
    }

    public void addListener(JobListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(JobListener listener) {
        return listeners.remove(listener);
    }

    public boolean removeListener(String listenerName) {
        Iterator<JobListener> itr = listeners.iterator();
        while(itr.hasNext()) {
            JobListener jl = itr.next();
            if(jl.getName().equals(listenerName)) {
                itr.remove();
                return true;
            }
        }
        return false;
    }

    public List<JobListener> getListeners() {
        return java.util.Collections.unmodifiableList(listeners);
    }

    public void jobToBeExecuted(JobExecutionContext context) {

        Iterator<JobListener> itr = listeners.iterator();
        while(itr.hasNext()) {
            JobListener jl = itr.next();
            jl.jobToBeExecuted(context);
        }
    }

    public void jobExecutionVetoed(JobExecutionContext context) {

        Iterator<JobListener> itr = listeners.iterator();
        while(itr.hasNext()) {
            JobListener jl = itr.next();
            jl.jobExecutionVetoed(context);
        }
    }

    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

        Iterator<JobListener> itr = listeners.iterator();
        while(itr.hasNext()) {
            JobListener jl = itr.next();
            jl.jobWasExecuted(context, jobException);
        }
    }

}
