/*
 * Created on Jan 28, 2021
 *
 * author dimitry
 */
package org.quartz.workflow;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;

interface Schedulers {
    Scheduler byNameOrDefault(String schedulerName) throws SchedulerException;
}