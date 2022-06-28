package org.jahiacommunity.modules.battlecard.graphql;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminQuery;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahiacommunity.modules.battlecard.edp.BattlecardDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GraphQLTypeExtension(GqlAdminQuery.class)
@GraphQLDescription("Battlecard extensions")
public class AdminBattlecardCacheEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(AdminBattlecardCacheEndpoint.class);

    @GraphQLField
    @GraphQLDescription("Flush Battlecard cache")
    public static boolean flushBattlecardCache(@GraphQLName("path") @GraphQLNonNull String path) {
        try {
            return BundleUtils.getOsgiService(JCRTemplate.class, null).doExecuteWithSystemSession(systemSession -> {
                if (!systemSession.nodeExists(path)) {
                    return false;
                }
                JCRNodeWrapper battlecardNode = systemSession.getNode(path);
                if (battlecardNode == null) {
                    logger.warn("Node [{}] not found", path);
                    return false;
                }
                BattlecardDataSource battlecardDataSource = (BattlecardDataSource) ((ExternalContentStoreProvider) battlecardNode.getProvider()).getDataSource();
                if (battlecardDataSource == null) {
                    logger.warn("BattlecardDataSource [{}] not found.", path);
                    return false;
                }
                battlecardDataSource.flush();
                return true;
            });
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
    }
}
