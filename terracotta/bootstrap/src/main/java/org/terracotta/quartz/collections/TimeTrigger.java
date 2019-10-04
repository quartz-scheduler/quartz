/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.quartz.collections;

import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.io.Serializable;
import java.util.Date;

public class TimeTrigger implements Comparable<TimeTrigger>, Serializable {

  private final TriggerKey triggerKey;
  private final Long       nextFireTime;
  private final int        priority;

  TimeTrigger(TriggerKey triggerKey, Date next, int priority) {
    this.triggerKey = triggerKey;
    this.nextFireTime = next == null ? null : next.getTime();
    this.priority = priority;
  }

  TriggerKey getTriggerKey() {
    return triggerKey;
  }

  int getPriority() {
    return priority;
  }

  Date getNextFireTime() {
    return nextFireTime == null ? null : new Date(nextFireTime);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TimeTrigger) {
      TimeTrigger other = (TimeTrigger) obj;
      return triggerKey.equals(other.triggerKey);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return triggerKey.hashCode();
  }

  @Override
  public String toString() {
    return "TimeTrigger [triggerKey=" + triggerKey + ", nextFireTime=" + new Date(nextFireTime) + ", priority="
           + priority + "]";
  }

  @Override
  public int compareTo(TimeTrigger tt2) {
    TimeTrigger tt1 = this;
    return Trigger.TriggerTimeComparator.compare(tt1.getNextFireTime(), tt1.getPriority(), tt1.getTriggerKey(),
                                                 tt2.getNextFireTime(), tt2.getPriority(), tt2.getTriggerKey());
  }

}