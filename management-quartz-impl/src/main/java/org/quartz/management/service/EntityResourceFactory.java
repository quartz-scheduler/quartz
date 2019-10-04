package org.quartz.management.service;

import java.util.Collection;
import java.util.Set;

import org.quartz.management.resource.SchedulerEntity;

/**
 * and retrieving the associated {@code CacheManagerSampler} objects
 * 
 * @author brandony
 */
public interface EntityResourceFactory {
    interface Locator {
        EntityResourceFactory locateEntityResourceFactory();
    }

    Collection<SchedulerEntity> createSchedulerEntities(Set<String> schedulerNames, Set<String> attributes);

    // Collection<CacheManagerConfigEntity>
    // createCacheManagerConfigEntities(Set<String> cacheManagerNames);
    //
    // Collection<CacheEntity> createCacheEntities(Set<String>
    // cacheManagerNames,
    // Set<String> cacheNames,
    // Set<String> attributes);
    //
    // Collection<CacheConfigEntity> createCacheConfigEntities(Set<String>
    // cacheManagerNames,
    // Set<String> cacheNames);
    //
    // Collection<CacheStatisticSampleEntity>
    // createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
    // Set<String> cacheNames,
    // Set<String> statName);
}
