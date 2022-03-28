import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';
import {FlushBattlecardCacheAction} from './FlushBattlecardCacheAction';

registry.add('callback', 'jahia-battlecard', {
    targets: ['jahiaApp-init:50'],
    callback: async () => {
        await i18next.loadNamespaces('jahia-battlecard');

        registry.add('action', '3dotsFlushBattlecardCacheAction', {
            buttonLabel: 'jahia-battlecard:label.contentActions.3dotsFlushBattlecardCacheAction',
            buttonIcon: window.jahia.moonstone.toIconComponent('Cancel'),
            requiredPermission: 'flushBatteclardCacheAction',
            targets: ['contentActions:99'],
            component: FlushBattlecardCacheAction
        });

        console.log('%c Battle card overrides have been registered', 'color: #3c8cba');
    }
});
