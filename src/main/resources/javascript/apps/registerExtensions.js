window.jahia.i18n.loadNamespaces('jahia-battlecard');

window.jahia.uiExtender.registry.add('callback', 'battlecard', {
    targets: ['jahiaApp-init:90'],
    callback: () => {
        window.jahia.uiExtender.registry.add('action', '3dotsFlushBattlecardCacheAction', {
            buttonLabel: 'jahia-battlecard:label.contentActions.3dotsFlushBattlecardCacheAction',
            buttonIcon: window.jahia.moonstone.toIconComponent('Cancel'),
            requiredPermission: 'flushBatteclardCacheAction',
            targets: ['contentActions:99'],
            onClick: ({path}) => {
                fetch(`${contextJsParameters.contextPath}/modules/graphql`, {
                    method: 'POST',
                    body: JSON.stringify({
                        query: `query flushBatteclardCache($mountPointNodePath:String!) {
                      admin {
                        flushBattlecardCache(mountPointNodePath: $mountPointNodePath)
                      }
                    }`,
                        variables: {mountPointNodePath: path}
                    })
                }).then(response => response.json()).then(data => {
                    if (data.data?.admin?.flushBattlecardCache) {
                        alert(window.jahia.i18n.t('jahia-battlecard:label.graphql.flushBattlecardCacheAction.success'));
                    } else {
                        throw new Error();
                    }
                }).catch(() => {
                    alert(window.jahia.i18n.t('jahia-battlecard:label.graphql.flushBattlecardCacheAction.error'));
                });
            }
        });
    }
});
