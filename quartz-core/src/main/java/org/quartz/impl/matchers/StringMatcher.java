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
 * An abstract base class for some types of matchers.
 *  
 * @author jhouse
 */
public abstract class StringMatcher<T extends Key<?>> implements Matcher<T> {
  
    private static final long serialVersionUID = -2757924162611145836L;

    public enum StringOperatorName {

        EQUALS {
            @Override
            public boolean evaluate(final String value, final String compareTo) {
                return value.equals(compareTo);
            }
        },

        STARTS_WITH {
            @Override
            public boolean evaluate(final String value, final String compareTo) {
                return value.startsWith(compareTo);
            }
        },

        ENDS_WITH {
            @Override
            public boolean evaluate(final String value, final String compareTo) {
                return value.endsWith(compareTo);
            }
        },

        CONTAINS {
            @Override
            public boolean evaluate(final String value, final String compareTo) {
                return value.contains(compareTo);
            }
        },

        ANYTHING {
            @Override
            public boolean evaluate(final String value, final String compareTo) {
                return true;
            }
        };

        public abstract boolean evaluate(String value, String compareTo);
    }

    protected String compareTo;
    protected StringOperatorName compareWith;
    
    protected StringMatcher(String compareTo, StringOperatorName compareWith) {
        if(compareTo == null)
            throw new IllegalArgumentException("CompareTo value cannot be null!");
        if(compareWith == null)
            throw new IllegalArgumentException("CompareWith operator cannot be null!");
        
        this.compareTo = compareTo;
        this.compareWith = compareWith;
    }

    protected abstract String getValue(T key);
    
    public boolean isMatch(T key) {

        return compareWith.evaluate(getValue(key), compareTo);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((compareTo == null) ? 0 : compareTo.hashCode());
        result = prime * result
                + ((compareWith == null) ? 0 : compareWith.hashCode());
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
        StringMatcher<?> other = (StringMatcher<?>) obj;
        if (compareTo == null) {
            if (other.compareTo != null)
                return false;
        } else if (!compareTo.equals(other.compareTo))
            return false;
        if (compareWith == null) {
            if (other.compareWith != null)
                return false;
        } else if (!compareWith.equals(other.compareWith))
            return false;
        return true;
    }

    public String getCompareToValue() {
        return compareTo;
    }

    public StringOperatorName getCompareWithOperator() {
        return compareWith;
    }

}
