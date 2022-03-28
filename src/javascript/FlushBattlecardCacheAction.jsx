import React from 'react';
import {useTranslation} from 'react-i18next';
import {useNodeChecks} from '@jahia/data-helper';

export const FlushBattlecardCacheAction = ({path, render: Render, ...otherProps}) => {
    const {t} = useTranslation('jahia-battlecard');

    const hasPermission = useNodeChecks({path}, {
        requiredPermission: ['flushBatteclardCacheAction'],
        showOnNodeTypes: ['jcnt:battlecardMountPoint', 'jcnt:battlecard', 'jcnt:battlecardCategory', 'jcnt:battlecardKeyValue']
    });

    if (!hasPermission || hasPermission.loading || !hasPermission.checksResult) {
        return null;
    }

    return <Render {...otherProps}
                   isVisible={hasPermission.checksResult}
                   onClick={async () => {
                       try {
                           const response = await fetch(`${contextJsParameters.contextPath}/modules/graphql`, {
                               method: 'POST',
                               body: JSON.stringify({
                                   query: `query flushBatteclardCache($mountPointNodePath:String!) {
                                              admin {
                                                flushBattlecardCache(mountPointNodePath: $mountPointNodePath)
                                              }
                                            }`,
                                   variables: {mountPointNodePath: path}
                               })
                           });
                           const data = await response.json();
                           if (data.data?.admin?.flushBattlecardCache) {
                               alert(t('label.graphql.flushBattlecardCacheAction.success'));
                           } else {
                               alert(t('label.graphql.flushBattlecardCacheAction.error'));
                           }
                       } catch (e) {
                           alert(t('label.graphql.flushBattlecardCacheAction.error'));
                       }
                   }}/>

};
