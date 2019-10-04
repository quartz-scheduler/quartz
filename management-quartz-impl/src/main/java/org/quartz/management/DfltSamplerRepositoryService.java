package org.quartz.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.quartz.core.QuartzScheduler;
import org.quartz.management.resource.SchedulerEntity;
import org.quartz.management.service.EntityResourceFactory;
import org.quartz.management.service.SamplerRepositoryService;
import org.terracotta.management.resource.AgentEntity;

public class DfltSamplerRepositoryService implements EntityResourceFactory, SamplerRepositoryService {

    private final Map<String, QuartzScheduler> cacheManagerSamplerRepo = new HashMap<String, QuartzScheduler>();

    @Override
    public Collection<SchedulerEntity> createSchedulerEntities(Set<String> schedulerNames, Set<String> attributes) {
        Collection<SchedulerEntity> schedulerEntities = new ArrayList<SchedulerEntity>();
        if (schedulerNames == null) {
            for (Entry<String, QuartzScheduler> scheduler : cacheManagerSamplerRepo.entrySet()) {
                SchedulerEntity schedulerEntity = new SchedulerEntity();
                schedulerEntity.setName(scheduler.getKey());
                schedulerEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);

                schedulerEntities.add(schedulerEntity);
            }
        }
        return schedulerEntities;
    }

    @Override
    public void register(QuartzScheduler quartzScheduler) {
        this.cacheManagerSamplerRepo.put(quartzScheduler.getSchedulerName(), quartzScheduler);
    }

    @Override
    public void unregister(QuartzScheduler quartzScheduler) {
        this.cacheManagerSamplerRepo.remove(quartzScheduler.getSchedulerName());

    }

    @Override
    public boolean hasRegistered() {
        return !this.cacheManagerSamplerRepo.isEmpty();
    }

}