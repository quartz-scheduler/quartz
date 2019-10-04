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

package org.quartz.utils;

import java.io.Serializable;
import java.util.UUID;


/**
 * <p>
 * Object representing a job or trigger key.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 */
public class Key<T>  implements Serializable, Comparable<Key<T>> {
  
    private static final long serialVersionUID = -7141167957642391350L;

    /**
     * The default group for scheduling entities, with the value "DEFAULT".
     */
    public static final String DEFAULT_GROUP = "DEFAULT";

    private final String name;
    private final String group;
    
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Construct a new key with the given name and group.
     * 
     * @param name
     *          the name
     * @param group
     *          the group
     */
    public Key(String name, String group) {
        if(name == null)
            throw new IllegalArgumentException("Name cannot be null.");
        this.name = name;
        if(group != null)
            this.group = group;
        else
            this.group = DEFAULT_GROUP;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the name portion of the key.
     * </p>
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Get the group portion of the key.
     * </p>
     * 
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * <p>
     * Return the string representation of the key. The format will be:
     * &lt;group&gt;.&lt;name&gt;.
     * </p>
     * 
     * @return the string representation of the key
     */
    @Override
    public String toString() {
        return getGroup() + '.' + getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        @SuppressWarnings("unchecked")
        Key<T> other = (Key<T>) obj;
        if (group == null) {
            if (other.group != null)
                return false;
        } else if (!group.equals(other.group))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public int compareTo(Key<T> o) {
        
        if(group.equals(DEFAULT_GROUP) && !o.group.equals(DEFAULT_GROUP))
            return -1;
        if(!group.equals(DEFAULT_GROUP) && o.group.equals(DEFAULT_GROUP))
            return 1;
            
        int r = group.compareTo(o.getGroup());
        if(r != 0)
            return r;
        
        return name.compareTo(o.getName());
    }
    
    public static String createUniqueName(String group) {
        if(group == null)
            group = DEFAULT_GROUP;
        
        String n1 = UUID.randomUUID().toString();
        String n2 = UUID.nameUUIDFromBytes(group.getBytes()).toString();
        
        return String.format("%s-%s", n2.substring(24), n1);
    }
}
