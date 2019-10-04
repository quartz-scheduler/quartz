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
import org.quartz.TriggerKey;
import org.quartz.utils.Key;

/**
 * Matches on name (ignores group) property of Keys.
 *  
 * @author jhouse
 */
public class NameMatcher<T extends Key<?>> extends StringMatcher<T> {
  
    private static final long serialVersionUID = -33104959459613480L;

    protected NameMatcher(String compareTo, StringOperatorName compareWith) {
        super(compareTo, compareWith);
    }
    
    /**
     * Create a NameMatcher that matches names equaling the given string.
     */
    public static <T extends Key<?>> NameMatcher<T> nameEquals(String compareTo) {
        return new NameMatcher<T>(compareTo, StringOperatorName.EQUALS);
    }

    /**
     * Create a NameMatcher that matches job names equaling the given string.
     */
    public static NameMatcher<JobKey> jobNameEquals(String compareTo) {
        return NameMatcher.nameEquals(compareTo);
    }
    
    /**
     * Create a NameMatcher that matches trigger names equaling the given string.
     */
    public static NameMatcher<TriggerKey> triggerNameEquals(String compareTo) {
        return NameMatcher.nameEquals(compareTo);
    }
    
    /**
     * Create a NameMatcher that matches names starting with the given string.
     */
    public static <U extends Key<?>> NameMatcher<U> nameStartsWith(String compareTo) {
        return new NameMatcher<U>(compareTo, StringOperatorName.STARTS_WITH);
    }

    /**
     * Create a NameMatcher that matches job names starting with the given string.
     */
    public static NameMatcher<JobKey> jobNameStartsWith(String compareTo) {
        return NameMatcher.nameStartsWith(compareTo);
    }
    
    /**
     * Create a NameMatcher that matches trigger names starting with the given string.
     */
    public static NameMatcher<TriggerKey> triggerNameStartsWith(String compareTo) {
        return NameMatcher.nameStartsWith(compareTo);
    }

    /**
     * Create a NameMatcher that matches names ending with the given string.
     */
    public static <U extends Key<?>> NameMatcher<U> nameEndsWith(String compareTo) {
        return new NameMatcher<U>(compareTo, StringOperatorName.ENDS_WITH);
    }

    /**
     * Create a NameMatcher that matches job names ending with the given string.
     */
    public static NameMatcher<JobKey> jobNameEndsWith(String compareTo) {
        return NameMatcher.nameEndsWith(compareTo);
    }
    
    /**
     * Create a NameMatcher that matches trigger names ending with the given string.
     */
    public static NameMatcher<TriggerKey> triggerNameEndsWith(String compareTo) {
        return NameMatcher.nameEndsWith(compareTo);
    }

    /**
     * Create a NameMatcher that matches names containing the given string.
     */
    public static <U extends Key<?>> NameMatcher<U> nameContains(String compareTo) {
        return new NameMatcher<U>(compareTo, StringOperatorName.CONTAINS);
    }

    /**
     * Create a NameMatcher that matches job names containing the given string.
     */
    public static NameMatcher<JobKey> jobNameContains(String compareTo) {
        return NameMatcher.nameContains(compareTo);
    }
    
    /**
     * Create a NameMatcher that matches trigger names containing the given string.
     */
    public static NameMatcher<TriggerKey> triggerNameContains(String compareTo) {
        return NameMatcher.nameContains(compareTo);
    }
    
    @Override
    protected String getValue(T key) {
        return key.getName();
    }

}
