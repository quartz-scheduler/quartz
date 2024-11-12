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
 * 
 */

package org.quartz;

import java.io.Serializable;

import org.quartz.utils.Key;

/**
 * Matchers can be used in various {@link Scheduler} API methods to 
 * select the entities that should be operated upon.
 *  
 * @author jhouse
 * @since 2.0
 */
public interface Matcher<T extends Key<?>> extends Serializable {

    boolean isMatch(T key);
 
    int hashCode();

    boolean equals(Object obj);
}
