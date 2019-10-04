package org.quartz.management;

import org.quartz.core.QuartzScheduler;
import org.quartz.management.service.EntityResourceFactory;
import org.quartz.management.service.SamplerRepositoryService;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.StandaloneServer;

/**
 * @author Anthony Dahanne
 */
public final class ManagementServerImpl implements ManagementServer {

    private final StandaloneServer standaloneServer;

    private final SamplerRepositoryService samplerRepoSvc;

    public ManagementServerImpl(ManagementRESTServiceConfiguration configuration) {
        String basePackage = "org.quartz.management.resource.services;org.quartz.management.jaxrs";
        String host = configuration.getHost();
        int port = configuration.getPort();

        standaloneServer = new StandaloneServer(null, null, basePackage, host, port, null, false);
        loadEmbeddedAgentServiceLocator();
        this.samplerRepoSvc = ServiceLocator.locate(SamplerRepositoryService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        try {
            standaloneServer.start();
        } catch (Exception e) {
            throw new RuntimeException("error starting management server", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        try {
            standaloneServer.stop();
        } catch (Exception e) {
            throw new RuntimeException("error stopping management server", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(QuartzScheduler managedResource) {
        samplerRepoSvc.register(managedResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(QuartzScheduler managedResource) {
        samplerRepoSvc.unregister(managedResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRegistered() {
        return true;
        // return samplerRepoSvc.hasRegistered();
    }

    private void loadEmbeddedAgentServiceLocator() {
        DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService();
        ServiceLocator locator = new ServiceLocator().loadService(SamplerRepositoryService.class , samplerRepoSvc)
                                                     .loadService(EntityResourceFactory.class, samplerRepoSvc);
        
        ServiceLocator.load(locator);
    }
}
