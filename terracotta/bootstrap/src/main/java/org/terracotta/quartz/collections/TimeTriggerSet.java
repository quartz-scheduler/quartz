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

package org.terracotta.quartz.collections;

import org.quartz.TriggerKey;
import org.terracotta.quartz.wrappers.TriggerWrapper;
import org.terracotta.toolkit.collections.ToolkitSortedSet;

import java.util.Iterator;

public class TimeTriggerSet {
  private final ToolkitSortedSet<TimeTrigger> timeTriggers;

  public TimeTriggerSet(ToolkitSortedSet<TimeTrigger> timeTriggers) {
    this.timeTriggers = timeTriggers;
  }

  public boolean add(TriggerWrapper wrapper) {
    TimeTrigger timeTrigger = new TimeTrigger(wrapper.getKey(), wrapper.getNextFireTime(), wrapper.getPriority());
    return timeTriggers.add(timeTrigger);
  }

  public boolean remove(TriggerWrapper wrapper) {
    TimeTrigger timeTrigger = new TimeTrigger(wrapper.getKey(), wrapper.getNextFireTime(), wrapper.getPriority());
    boolean result = timeTriggers.remove(timeTrigger);
    return result;
  }

  public TriggerKey removeFirst() {
    Iterator<TimeTrigger> iter = timeTriggers.iterator();
    TimeTrigger tt = null;
    if (iter.hasNext()) {
      tt = iter.next();
      iter.remove();
    }
    return tt == null ? null : tt.getTriggerKey();
  }

  public boolean isDestroyed() {
    return timeTriggers.isDestroyed();
  }
  
  public void destroy() {
    timeTriggers.destroy();
  }

  public int size() {
    return timeTriggers.size();
  }
}
