package org.jahiacommunity.modules.battlecard.edp;

import org.jahia.osgi.BundleUtils;
import org.jahia.services.scheduler.BackgroundJob;
import org.osgi.framework.Constants;
import org.quartz.JobExecutionContext;

public class FlushBattlecardJob extends BackgroundJob {
    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) {
        BattlecardDataSource battlecardDataSource = BundleUtils.getOsgiService(BattlecardDataSource.class,
                "(" + Constants.SERVICE_PID + "=" + jobExecutionContext.getJobDetail().getJobDataMap().get(Constants.SERVICE_PID) + ")");
        if (battlecardDataSource != null) {
            battlecardDataSource.flush();
        }
    }
}
