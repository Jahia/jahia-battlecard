package org.jahiacommunity.modules.battlecard.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminQuery;
import org.jahia.osgi.BundleUtils;
import org.jahiacommunity.modules.battlecard.edp.BattlecardCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GraphQLTypeExtension(GqlAdminQuery.class)
@GraphQLDescription("Battlecard extensions")
public class AdminBattlecardCacheEndpoint {
    public static final Logger logger = LoggerFactory.getLogger(AdminBattlecardCacheEndpoint.class);

    @GraphQLField
    @GraphQLDescription("Flush Battlecard cache")
    public static boolean flushBattlecardCache() {
        try {
            BundleUtils.getOsgiService(BattlecardCacheManager.class, null).flush();
            return true;
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
    }
}
