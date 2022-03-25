package org.jahiacommunity.modules.battlecard.graphql;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminQuery;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRTemplate;
import org.jahiacommunity.modules.battlecard.edp.BattlecardDataSource;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GraphQLTypeExtension(GqlAdminQuery.class)
@GraphQLDescription("Battlecard extensions")
public class AdminBattlecardCacheEndpoint {
    public static final Logger logger = LoggerFactory.getLogger(AdminBattlecardCacheEndpoint.class);

    @GraphQLField
    @GraphQLDescription("Flush Battlecard cache")
    public static boolean flushBattlecardCache(@GraphQLName("mountPointNodePath") @GraphQLNonNull String mountPointNodePath) {
        try {
            String mountPointNodeIdentifier = BundleUtils.getOsgiService(JCRTemplate.class, null).doExecuteWithSystemSession(systemSession -> {
                if (systemSession.nodeExists(mountPointNodePath)) {
                    return systemSession.getNode(mountPointNodePath).getIdentifier();
                }
                return null;
            });
            if (mountPointNodeIdentifier == null) {
                logger.warn("Node [{}] not found", mountPointNodePath);
                return false;
            }

            BattlecardDataSource battlecardDataSource = BundleUtils.getOsgiService(BattlecardDataSource.class,
                    "(" + Constants.SERVICE_PID + "=" + mountPointNodeIdentifier + ")");
            if (battlecardDataSource == null) {
                logger.warn("BattlecardDataSource [{}] not found.", mountPointNodePath);
                return false;
            }
            battlecardDataSource.flush();
            return true;
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
    }
}
