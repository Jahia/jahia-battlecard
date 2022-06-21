package org.jahiacommunity.modules.battlecard.edp;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;

@Component(service = {BattlecardProviderFactory.class, ProviderFactory.class}, immediate = true)
public class BattlecardProviderFactory implements ProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(BattlecardProviderFactory.class);

    public static final String NODETYPE = "jcnt:battlecardMountPoint";
    private static final String PROPERTY_CREDENTIALS = "credentials";
    private static final String PROPERTY_PROJECTID = "projectId";
    private static final String PROPERTY_SPREADSHEETID = "spreadsheetId";
    private static final String PROPERTY_EXCLUDEDSHEETS = "excludedSheets";
    private static final String PROPERTY_CRONEXPRESSION = "cronExpression";

    private SchedulerService schedulerService;
    private final Map<String, BattlecardDataSource> battlecardDataSources;
    private final Map<String, JobDetail> jobDetails;

    public BattlecardProviderFactory() {
        battlecardDataSources = new HashMap<>();
        jobDetails = new HashMap<>();
    }

    @Reference
    private void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public String getNodeTypeName() {
        return NODETYPE;
    }

    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper jcrNodeWrapper) throws RepositoryException {
        try {
            ExternalContentStoreProvider provider = createExternalDataProviderCacheAndJob(jcrNodeWrapper);
            //Start the provider
            provider.start(true);
            logger.info("Started the provider");
            return provider;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    @Deactivate
    private void onDeactivate() {
        battlecardDataSources.forEach((key, battlecardDataSource) -> battlecardDataSource.disconnect());
        jobDetails.forEach((key, jobDetail) -> stopJobDetail(jobDetail));
    }

    private void stopJobDetail(JobDetail jobDetail) {
        try {
            if (!schedulerService.getAllJobs(jobDetail.getGroup()).isEmpty() && SettingsBean.getInstance().isProcessingServer()) {
                schedulerService.getScheduler().deleteJob(jobDetail.getName(), jobDetail.getGroup());
            }
        } catch (SchedulerException e) {
            logger.error("", e);
        }
    }

    private ExternalContentStoreProvider createExternalDataProviderCacheAndJob(JCRNodeWrapper mountPointNode) throws RepositoryException, SchedulerException, ParseException, GeneralSecurityException, IOException {
        ExternalContentStoreProvider provider = (ExternalContentStoreProvider) SpringContextSingleton.getBean("ExternalStoreProviderPrototype");

        // create datasource
        BattlecardDataSource oldBattlecardDataSource = BundleUtils.getOsgiService(BattlecardDataSource.class,
                "(" + Constants.SERVICE_PID + "=" + mountPointNode.getIdentifier() + ")");
        if (oldBattlecardDataSource != null) {
            oldBattlecardDataSource.disconnect();
        }
        String mountPointPath = mountPointNode.getProperty(JCRMountPointNode.MOUNT_POINT_PROPERTY_NAME).getNode().getPath();
        BattlecardDataSource battlecardDataSource = new BattlecardDataSource(mountPointNode.getIdentifier(), mountPointPath,
                mountPointNode.getProperty(PROPERTY_CREDENTIALS).getString(),
                mountPointNode.getProperty(PROPERTY_PROJECTID).getString(),
                mountPointNode.getProperty(PROPERTY_SPREADSHEETID).getString(),
                Arrays.stream(mountPointNode.getProperty(PROPERTY_EXCLUDEDSHEETS).getValues()).map(value -> {
                    try {
                        return value.getString();
                    } catch (RepositoryException e) {
                        logger.warn("", e);
                        return null;
                    }
                }).filter(Objects::nonNull).toArray(String[]::new));
        battlecardDataSource.setServiceRegistration(FrameworkUtil.getBundle(BattlecardDataSource.class).getBundleContext()
                .registerService(BattlecardDataSource.class.getName(), battlecardDataSource,
                        new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, mountPointNode.getIdentifier()))));
        battlecardDataSources.put(mountPointNode.getIdentifier(), battlecardDataSource);

        // create the flush cache job
        JobDetail jobDetail = BackgroundJob.createJahiaJob("Flush battlecard with a cron Job", FlushBattlecardJob.class);
        jobDetail.getJobDataMap().put(Constants.SERVICE_PID, mountPointNode.getIdentifier());
        if (schedulerService.getAllJobs(jobDetail.getGroup()).isEmpty() && SettingsBean.getInstance().isProcessingServer()) {
            schedulerService.getScheduler().scheduleJob(jobDetail,
                    new CronTrigger("flushBattlecardJob_cronTrigger", jobDetail.getGroup(),
                            mountPointNode.getProperty(PROPERTY_CRONEXPRESSION).getString()));
        }
        jobDetails.put(mountPointNode.getIdentifier(), jobDetail);

        // set the provider
        provider.setKey(mountPointNode.getIdentifier());
        provider.setMountPoint(mountPointPath);
        provider.setDataSource(battlecardDataSource);
        provider.setDynamicallyMounted(true);
        provider.setSessionFactory(JCRSessionFactory.getInstance());

        return provider;
    }

    public static void deleteMountPoint(String mountPointNodeIdentifier) {
        logger.info("Delete or unmount mount point: {}", mountPointNodeIdentifier);
        BattlecardProviderFactory battlecardProviderFactory = BundleUtils.getOsgiService(BattlecardProviderFactory.class, null);
        if (battlecardProviderFactory != null) {
            if (battlecardProviderFactory.battlecardDataSources.containsKey(mountPointNodeIdentifier)) {
                battlecardProviderFactory.battlecardDataSources.get(mountPointNodeIdentifier).disconnect();
                battlecardProviderFactory.battlecardDataSources.remove(mountPointNodeIdentifier);
            }
            if (battlecardProviderFactory.jobDetails.containsKey(mountPointNodeIdentifier)) {
                battlecardProviderFactory.stopJobDetail(battlecardProviderFactory.jobDetails.get(mountPointNodeIdentifier));
                battlecardProviderFactory.jobDetails.remove(mountPointNodeIdentifier);
            }
        }
    }
}
