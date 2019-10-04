package org.quartz.management.service;

import org.quartz.core.QuartzScheduler;

/**
 * A service for registering {@link QuartzScheduler}s for sampling.
 * 
 * @author brandony
 */
public interface SamplerRepositoryService {
    interface Locator {
        SamplerRepositoryService locateSamplerRepositoryService();
    }

    /**
     * Register a {@link QuartzScheduler} for sampling.
     * 
     * @param quartzScheduler
     *            to register
     */
    void register(QuartzScheduler quartzScheduler);

    void unregister(QuartzScheduler quartzScheduler);

    boolean hasRegistered();
}
