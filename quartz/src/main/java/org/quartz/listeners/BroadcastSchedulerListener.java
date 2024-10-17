package org.quartz.listeners;

import java.util.LinkedList;
import java.util.List;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Holds a List of references to SchedulerListener instances and broadcasts all
 * events to them (in order).
 *
 * <p>This may be more convenient than registering all of the listeners
 * directly with the Scheduler, and provides the flexibility of easily changing
 * which listeners get notified.</p>
 *
 * @see #addListener(org.quartz.SchedulerListener)
 * @see #removeListener(org.quartz.SchedulerListener)
 *
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastSchedulerListener implements SchedulerListener {

    private final List<SchedulerListener> listeners;

    public BroadcastSchedulerListener() {
        listeners = new LinkedList<>();
    }

    /**
     * Construct an instance with the given List of listeners.
     *
     * @param listeners the initial List of SchedulerListeners to broadcast to.
     */
    public BroadcastSchedulerListener(List<SchedulerListener> listeners) {
        this();
        this.listeners.addAll(listeners);
    }


    public void addListener(SchedulerListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(SchedulerListener listener) {
        return listeners.remove(listener);
    }

    public List<SchedulerListener> getListeners() {
        return java.util.Collections.unmodifiableList(listeners);
    }

    public void jobAdded(JobDetail jobDetail) {
        for (SchedulerListener l : listeners) {
            l.jobAdded(jobDetail);
        }
    }

    public void jobDeleted(JobKey jobKey) {
        for (SchedulerListener l : listeners) {
            l.jobDeleted(jobKey);
        }
    }
    
    public void jobScheduled(Trigger trigger) {
        for (SchedulerListener l : listeners) {
            l.jobScheduled(trigger);
        }
    }

    public void jobUnscheduled(TriggerKey triggerKey) {
        for (SchedulerListener l : listeners) {
            l.jobUnscheduled(triggerKey);
        }
    }

    public void triggerFinalized(Trigger trigger) {
        for (SchedulerListener l : listeners) {
            l.triggerFinalized(trigger);
        }
    }

    public void triggerPaused(TriggerKey key) {
        for (SchedulerListener l : listeners) {
            l.triggerPaused(key);
        }
    }

    public void triggersPaused(String triggerGroup) {
        for (SchedulerListener l : listeners) {
            l.triggersPaused(triggerGroup);
        }
    }

    public void triggerResumed(TriggerKey key) {
        for (SchedulerListener l : listeners) {
            l.triggerResumed(key);
        }
    }

    public void triggersResumed(String triggerGroup) {
        for (SchedulerListener l : listeners) {
            l.triggersResumed(triggerGroup);
        }
    }
    
    public void schedulingDataCleared() {
        for (SchedulerListener l : listeners) {
            l.schedulingDataCleared();
        }
    }

    
    public void jobPaused(JobKey key) {
        for (SchedulerListener l : listeners) {
            l.jobPaused(key);
        }
    }

    public void jobsPaused(String jobGroup) {
        for (SchedulerListener l : listeners) {
            l.jobsPaused(jobGroup);
        }
    }

    public void jobResumed(JobKey key) {
        for (SchedulerListener l : listeners) {
            l.jobResumed(key);
        }
    }

    public void jobsResumed(String jobGroup) {
        for (SchedulerListener l : listeners) {
            l.jobsResumed(jobGroup);
        }
    }
    
    public void schedulerError(String msg, SchedulerException cause) {
        for (SchedulerListener l : listeners) {
            l.schedulerError(msg, cause);
        }
    }

    public void schedulerStarted() {
        for (SchedulerListener l : listeners) {
            l.schedulerStarted();
        }
    }
    
    public void schedulerStarting() {
        for (SchedulerListener l : listeners) {
            l.schedulerStarting();
        }
    }

    public void schedulerInStandbyMode() {
        for (SchedulerListener l : listeners) {
            l.schedulerInStandbyMode();
        }
    }
    
    public void schedulerShutdown() {
        for (SchedulerListener l : listeners) {
            l.schedulerShutdown();
        }
    }
    
    public void schedulerShuttingdown() {
        for (SchedulerListener l : listeners) {
            l.schedulerShuttingdown();
        }
    }
    
}
