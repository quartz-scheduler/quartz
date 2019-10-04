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
 * Matches using an NOT operator on another Matcher. 
 *  
 * @author jhouse
 */
public class NotMatcher<T extends Key<?>> implements Matcher<T> {
  
    private static final long serialVersionUID = -2856769076151741391L;

    protected Matcher<T> operand;
    
    protected NotMatcher(Matcher<T> operand) {
        if(operand == null)
            throw new IllegalArgumentException("Non-null operand required!");
        
        this.operand = operand;
    }
    
    /**
     * Create a NotMatcher that reverses the result of the given matcher.
     */
    public static <U extends Key<?>> NotMatcher<U> not(Matcher<U> operand) {
        return new NotMatcher<U>(operand);
    }

    public boolean isMatch(T key) {

        return !operand.isMatch(key);
    }

    public Matcher<T> getOperand() {
        return operand;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operand == null) ? 0 : operand.hashCode());
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
        NotMatcher<?> other = (NotMatcher<?>) obj;
        if (operand == null) {
            if (other.operand != null)
                return false;
        } else if (!operand.equals(other.operand))
            return false;
        return true;
    }
}
