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

package org.terracotta.quartz.wrappers;

import org.quartz.TriggerKey;

import java.io.Serializable;
import java.util.Date;

public class FiredTrigger implements Serializable {
  private final String     clientId;
  private final TriggerKey triggerKey;
  private final long       scheduledFireTime;
  private final long       fireTime;

  public FiredTrigger(String clientId, TriggerKey triggerKey, long scheduledFireTime) {
    this(clientId, triggerKey, scheduledFireTime, System.currentTimeMillis());
  }
  
  public FiredTrigger(String clientId, TriggerKey triggerKey, long scheduledFireTime, long now) {
    this.clientId = clientId;
    this.triggerKey = triggerKey;
    this.scheduledFireTime = scheduledFireTime;
    this.fireTime = now;
  }

  public String getClientId() {
    return clientId;
  }

  public TriggerKey getTriggerKey() {
    return triggerKey;
  }

  public long getScheduledFireTime() {
    return scheduledFireTime;
  }
  
  public long getFireTime() {
    return fireTime;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + triggerKey + ", " + getClientId() + ", " + new Date(fireTime) + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
    result = prime * result + (int) (scheduledFireTime ^ (scheduledFireTime >>> 32));
    result = prime * result + (int) (fireTime ^ (fireTime >>> 32));
    result = prime * result + ((triggerKey == null) ? 0 : triggerKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    FiredTrigger other = (FiredTrigger) obj;
    if (clientId == null) {
      if (other.clientId != null) return false;
    } else if (!clientId.equals(other.clientId)) return false;
    if (scheduledFireTime != other.scheduledFireTime) return false;
    if (fireTime != other.fireTime) return false;
    if (triggerKey == null) {
      if (other.triggerKey != null) return false;
    } else if (!triggerKey.equals(other.triggerKey)) return false;
    return true;
  }
}
