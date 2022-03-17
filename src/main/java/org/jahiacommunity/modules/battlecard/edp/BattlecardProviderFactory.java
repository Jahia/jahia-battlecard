package org.jahiacommunity.modules.battlecard.edp;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalProviderInitializerService;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;

@Component(service = ProviderFactory.class, immediate = true)
public class BattlecardProviderFactory implements ProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(BattlecardProviderFactory.class);

    public static final String NODETYPE = "jcnt:battlecardMountPoint";

    private static final List<String> EXTENDABLE_TYPES = Collections.singletonList(NODETYPE);
    private static final List<String> OVERRIDABLE_ITEMS = Collections.singletonList("*.*");
    private static final List<String> NONOVERRIDABLE_ITEMS = Collections.emptyList();

    private ExternalContentStoreProvider provider;
    private BattlecardDataSource battlecardDataSource;
    private BattlecardCacheManager battlecardCacheManager;

    /**
     * Core services
     */
    private JahiaUserManagerService userManagerService;
    private JahiaGroupManagerService groupManagerService;
    private JahiaSitesService sitesService;
    private JCRStoreService jcrStoreService;
    private JCRSessionFactory sessionFactory;
    private ExternalProviderInitializerService externalProviderInitializerService;

    @Reference
    private void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    @Reference
    private void setGroupManagerService(JahiaGroupManagerService groupManagerService) {
        this.groupManagerService = groupManagerService;
    }

    @Reference
    private void setSitesService(JahiaSitesService sitesService) {
        this.sitesService = sitesService;
    }

    @Reference
    private void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    @Reference
    private void setSessionFactory(JCRSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Reference
    private void setExternalProviderInitializerService(ExternalProviderInitializerService externalProviderInitializerService) {
        this.externalProviderInitializerService = externalProviderInitializerService;
    }

    @Reference(service = BattlecardDataSource.class)
    private void setStoreDataSource(BattlecardDataSource battlecardDataSource) {
        this.battlecardDataSource = battlecardDataSource;
    }

    @Reference
    private void setBattlecardCacheManager(BattlecardCacheManager battlecardCacheManager) {
        this.battlecardCacheManager = battlecardCacheManager;
    }

    @Override
    public String getNodeTypeName() {
        return NODETYPE;
    }

    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper jcrNodeWrapper) throws RepositoryException {
        if (provider == null) {
            provider = new ExternalContentStoreProvider();
            provider.setUserManagerService(userManagerService);
            provider.setGroupManagerService(groupManagerService);
            provider.setSitesService(sitesService);
            provider.setService(jcrStoreService);
            provider.setSessionFactory(sessionFactory);
            provider.setExternalProviderInitializerService(externalProviderInitializerService);
        }

        try {
            provider.setKey(jcrNodeWrapper.getIdentifier());
            String path = jcrNodeWrapper.getProperty(JCRMountPointNode.MOUNT_POINT_PROPERTY_NAME).getNode().getPath();
            battlecardCacheManager.setOutputPath(path);
            provider.setMountPoint(path);
            provider.setDataSource(battlecardDataSource);
            provider.setDynamicallyMounted(true);
            provider.setSessionFactory(JCRSessionFactory.getInstance());
            provider.setExtendableTypes(EXTENDABLE_TYPES);
            provider.setOverridableItems(OVERRIDABLE_ITEMS);
            provider.setNonOverridableItems(NONOVERRIDABLE_ITEMS);
            //Start the provider
            provider.start(true);
            logger.info("Started the provider");
        } catch (JahiaInitializationException e) {
            throw new RepositoryException(e);
        }
        return provider;
    }

    @Activate
    private void onActivate() {
        battlecardCacheManager.flush();
    }

    @Deactivate
    private void onDeactivate() {
        provider = null;
    }
}
