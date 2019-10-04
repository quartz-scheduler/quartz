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

import org.quartz.Matcher;
import org.quartz.utils.Key;

/**
 * Matches on the complete key being equal (both name and group). 
 *  
 * @author jhouse
 */
public class KeyMatcher<T extends Key<?>> implements Matcher<T> {
  
    private static final long serialVersionUID = 1230009869074992437L;

    protected T compareTo;
    
    protected KeyMatcher(T compareTo) {
        this.compareTo = compareTo;
    }
    
    /**
     * Create a KeyMatcher that matches Keys that equal the given key. 
     */
    public static <U extends Key<?>> KeyMatcher<U> keyEquals(U compareTo) {
        return new KeyMatcher<U>(compareTo);
    }

    public boolean isMatch(T key) {

        return compareTo.equals(key);
    }

    public T getCompareToValue() {
        return compareTo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((compareTo == null) ? 0 : compareTo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KeyMatcher<?> other = (KeyMatcher<?>) obj;
        if (compareTo == null) {
            if (other.compareTo != null)
                return false;
        } else if (!compareTo.equals(other.compareTo))
            return false;
        return true;
    }
    
}
