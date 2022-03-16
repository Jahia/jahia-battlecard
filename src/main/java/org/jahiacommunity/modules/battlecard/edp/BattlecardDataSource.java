package org.jahiacommunity.modules.battlecard.edp;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.services.sites.JahiaSitesService;
import org.jahiacommunity.modules.battlecard.service.BattlecardMapper;
import org.jahiacommunity.modules.battlecard.service.GoogleSheetService;
import org.jahiacommunity.modules.battlecard.service.NodeValue;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = {BattlecardDataSource.class, ExternalDataSource.class}, immediate = true)
public class BattlecardDataSource implements ExternalDataSource {
    private static final Logger logger = LoggerFactory.getLogger(BattlecardDataSource.class);

    private BattlecardCacheManager battlecardCacheManager;
    private GoogleSheetService googleSheetService;
    private JahiaSitesService jahiaSitesService;
    private Set<String> languages;

    @Reference
    private void setBattlecardCacheManager(BattlecardCacheManager battlecardCacheManager) {
        this.battlecardCacheManager = battlecardCacheManager;
    }

    @Reference
    private void setGoogleSheetService(GoogleSheetService googleSheetService) {
        this.googleSheetService = googleSheetService;
    }

    @Reference
    private void setJahiaSitesService(JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }

    @Activate
    private void onActivate() {
        try {
            languages = jahiaSitesService.getSiteByKey(JahiaSitesService.SYSTEM_SITE_KEY).getLanguages();
        } catch (JahiaException e) {
            logger.warn("", e);
        }
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        if (identifier.startsWith("/")) {
            try {
                return getItemByPath(identifier);
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(identifier, e);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        if (!path.startsWith("/")) {
            throw new PathNotFoundException(path);
        }
        if ("/".equals(path)) {
            return BattlecardMapper.mapRootNode(path);
        }
        String[] pathes = path.split("/");
        switch (pathes.length) {
            case 2:
                // /<sheet>
                String sheetTitle = getSheets(path).stream().filter(sheet -> pathes[1].equals(sheet.getNodename())).findFirst()
                        .orElseThrow(() -> new PathNotFoundException(path))
                        .getTitle();
                return BattlecardMapper.mapBattlecard(path, languages, sheetTitle, googleSheetService.isJahiaSheet(sheetTitle));
            case 3:
                // /<sheet>/<category>
                return BattlecardMapper.mapBattlecardItem(path, languages, getSheetData(pathes[1]).entrySet().stream()
                        .filter(entry -> pathes[2].equals(entry.getKey().getNodename())).findFirst()
                        .orElseThrow(() -> new PathNotFoundException(path)).getKey().getTitle());
            case 4:
                // /<sheet>/<category>/<keyValue-xxx>
                int index = Integer.parseInt(StringUtils.substringAfter(pathes[3], BattlecardMapper.KEYVALUE_PREFIX));
                Map.Entry<String, String> keyValue = getSheetData(pathes[1]).entrySet().stream()
                        .filter(entry -> pathes[2].equals(entry.getKey().getNodename())).findFirst()
                        .orElseThrow(() -> new PathNotFoundException(path))
                        .getValue().entrySet().stream()
                        .skip(index).limit(1).findAny()
                        .orElseThrow(() -> new PathNotFoundException(path));
                return BattlecardMapper.mapKeyValue(path, languages, keyValue.getKey(), keyValue.getValue());
        }
        throw new PathNotFoundException(path);
    }

    @Override
    public List<String> getChildren(String path) throws PathNotFoundException {
        if (!path.startsWith("/")) {
            throw new PathNotFoundException(path);
        }

        if ("/".equals(path)) {
            return Collections.unmodifiableList((List<? extends String>) getSheets(path).stream()
                    .map(NodeValue::getNodename).collect(Collectors.toCollection(LinkedList::new)));
        }

        String[] pathes = path.split("/");
        if (pathes.length == 2) {
            // path: /<sheet>
            // return sheet category
            return Collections.unmodifiableList(
                    (List<? extends String>) getSheetData(pathes[1]).keySet().stream().map(NodeValue::getNodename)
                            .collect(Collectors.toCollection(LinkedList::new)));
        } else if (pathes.length == 3) {
            // path: /<sheet>/<category>
            // return sheet category key,value pair
            int[] index = {0};
            return getSheetData(pathes[1]).entrySet().stream()
                    .filter(entry -> pathes[2].equals(entry.getKey().getNodename())).findFirst()
                    .orElseThrow(() -> new PathNotFoundException(path))
                    .getValue().keySet().stream().map(key -> BattlecardMapper.KEYVALUE_PREFIX.concat(String.valueOf(index[0]++)))
                    .collect(Collectors.toCollection(LinkedList::new));
        }
        return Collections.emptyList();
    }

    private List<NodeValue> getSheets(String path) {
        List<NodeValue> sheets = battlecardCacheManager.getSheets(path);
        if (CollectionUtils.isEmpty(sheets)) {
            sheets = googleSheetService.getSheets().stream().map(NodeValue::new).collect(Collectors.toCollection(LinkedList::new));
            // cache sheets
            battlecardCacheManager.putSheets(path, sheets);
        }
        return sheets;
    }

    private Map<NodeValue, Map<String, String>> getSheetData(String sheet) {
        Map<NodeValue, Map<String, String>> data = battlecardCacheManager.getSheetData(sheet);
        if (MapUtils.isEmpty(data)) {
            data = BattlecardMapper.convertRowsToBattleCard(googleSheetService.getValues(sheet)).entrySet().stream()
                    .collect(Collectors.toMap(entry -> new NodeValue(entry.getKey()),
                            Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            // cache sheet data
            battlecardCacheManager.putSheetData(sheet, data);
        }
        return data;
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Collections.singleton(BattlecardProviderFactory.NODETYPE);
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String s) {
        return true;
    }
}
