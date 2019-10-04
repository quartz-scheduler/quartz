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
package org.quartz.impl.matchers;

import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.TriggerKey;
import org.quartz.utils.Key;

/**
 * Matches on the complete key being equal (both name and group). 
 *  
 * @author jhouse
 */
public class EverythingMatcher<T extends Key<?>> implements Matcher<T> {
  
    private static final long serialVersionUID = 202300056681974058L;
    
    protected EverythingMatcher() {
    }
    
    /**
     * Create an EverythingMatcher that matches all jobs.
     */
    public static EverythingMatcher<JobKey> allJobs() {
        return new EverythingMatcher<JobKey>();
    }

    /**
     * Create an EverythingMatcher that matches all triggers.
     */
    public static EverythingMatcher<TriggerKey> allTriggers() {
        return new EverythingMatcher<TriggerKey>();
    }
    
    public boolean isMatch(T key) {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        
        return obj.getClass().equals(getClass());
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    
}
