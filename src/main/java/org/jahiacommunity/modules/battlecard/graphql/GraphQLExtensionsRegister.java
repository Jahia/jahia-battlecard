package org.jahiacommunity.modules.battlecard.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Collection;
import java.util.Collections;

@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class GraphQLExtensionsRegister implements DXGraphQLExtensionsProvider {
    @Override
    public Collection<Class<?>> getExtensions() {
        return Collections.singleton(AdminBattlecardCacheEndpoint.class);
    }
}
