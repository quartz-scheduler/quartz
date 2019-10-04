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

import org.quartz.Calendar;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

import java.io.Serializable;
import java.util.Date;

public class TriggerWrapper implements Serializable {
  public enum TriggerState {
    WAITING, ACQUIRED, COMPLETE, PAUSED, BLOCKED, PAUSED_BLOCKED, ERROR;
  }

  private final boolean           jobDisallowsConcurrence;

  private volatile String         lastTerracotaClientId = null;
  private volatile TriggerState   state                 = TriggerState.WAITING;

  protected final OperableTrigger trigger;

  protected TriggerWrapper(OperableTrigger trigger, boolean jobDisallowsConcurrence) {
    this.trigger = trigger;
    this.jobDisallowsConcurrence = jobDisallowsConcurrence;

    // TriggerWrapper instances get shared in many collections and the serialized form
    // might be referenced before this wrapper makes it into the "timeTriggers" set
    // DEV-4807
    // serialize(serializer);
  }

  public boolean jobDisallowsConcurrence() {
    return jobDisallowsConcurrence;
  }

  public String getLastTerracotaClientId() {
    return lastTerracotaClientId;
  }

  @Override
  public int hashCode() {
    return getKey().hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "( " + state + ", lastTC=" + lastTerracotaClientId + ", " + trigger + ")";
  }

  public TriggerKey getKey() {
    return trigger.getKey();
  }

  public JobKey getJobKey() {
    return trigger.getJobKey();
  }

  public void setState(TriggerState state, String terracottaId, TriggerFacade triggerFacade) {
    if (terracottaId == null) { throw new NullPointerException(); }

    this.state = state;
    this.lastTerracotaClientId = terracottaId;

    rePut(triggerFacade);
  }

  public TriggerState getState() {
    return state;
  }

  public Date getNextFireTime() {
    return this.trigger.getNextFireTime();
  }

  public int getPriority() {
    return this.trigger.getPriority();
  }

  public boolean mayFireAgain() {
    return this.trigger.mayFireAgain();
  }

  public OperableTrigger getTriggerClone() {
    return (OperableTrigger) this.trigger.clone();
  }

  public void updateWithNewCalendar(Calendar cal, long misfireThreshold, TriggerFacade triggerFacade) {
    this.trigger.updateWithNewCalendar(cal, misfireThreshold);
    rePut(triggerFacade);
  }

  public String getCalendarName() {
    return this.trigger.getCalendarName();
  }

  public int getMisfireInstruction() {
    return this.trigger.getMisfireInstruction();
  }

  public void updateAfterMisfire(Calendar cal, TriggerFacade triggerFacade) {
    this.trigger.updateAfterMisfire(cal);
    rePut(triggerFacade);
  }

  public void setFireInstanceId(String firedInstanceId, TriggerFacade triggerFacade) {
    this.trigger.setFireInstanceId(firedInstanceId);
    rePut(triggerFacade);
  }

  public void triggered(Calendar cal, TriggerFacade triggerFacade) {
    this.trigger.triggered(cal);
    rePut(triggerFacade);
  }

  private void rePut(TriggerFacade triggerFacade) {
    triggerFacade.put(trigger.getKey(), this);
  }

  public boolean isInstanceof(Class clazz) {
    return clazz.isInstance(trigger);
  }
}