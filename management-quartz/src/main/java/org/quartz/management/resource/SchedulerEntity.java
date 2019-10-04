/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package org.quartz.management.resource;

import java.util.HashMap;
import java.util.Map;

import org.terracotta.management.resource.VersionedEntity;

/**
 * <p>
 * A {@link VersionedEntity} representing a Scheduler resource from the
 * management API.
 * </p>
 * 
 * TODO : could be merged with CacheManagerEntity; ie extending for example a
 * MainEntity.class
 */
public class SchedulerEntity extends VersionedEntity {
    private String name;

    private String agentId;

    private Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the agentId
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * @param agentId
     *            to set
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * 
     * @return the cache manager's attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
