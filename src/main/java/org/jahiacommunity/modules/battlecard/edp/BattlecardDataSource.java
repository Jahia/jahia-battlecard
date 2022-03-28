package org.jahiacommunity.modules.battlecard.edp;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.sites.JahiaSitesService;
import org.jahiacommunity.modules.battlecard.service.BattlecardMapper;
import org.jahiacommunity.modules.battlecard.service.GoogleSheetService;
import org.jahiacommunity.modules.battlecard.service.NodeValue;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

public class BattlecardDataSource implements ExternalDataSource, ExternalDataSource.Initializable {
    private static final Logger logger = LoggerFactory.getLogger(BattlecardDataSource.class);

    private final String mountPointNodeIdentifier;
    private final BattlecardCacheManager battlecardCacheManager;
    private final GoogleSheetService googleSheetService;
    private Set<String> languages;
    private ServiceRegistration<?> serviceRegistration;

    public BattlecardDataSource(String mountPointNodeIdentifier, String mountPointPath, String credentials, String projectId, String spreadsheetId, String masterSheet, String[] excludedSheets) throws GeneralSecurityException, IOException {
        this.mountPointNodeIdentifier = mountPointNodeIdentifier;
        battlecardCacheManager = new BattlecardCacheManager(mountPointPath);
        googleSheetService = new GoogleSheetService(credentials, projectId, spreadsheetId, masterSheet, excludedSheets);
    }

    public void setServiceRegistration(ServiceRegistration<?> serviceRegistration) {
        if (this.serviceRegistration == null) {
            this.serviceRegistration = serviceRegistration;
        } else {
            logger.error("This instance of BattlecardDataSource as already been registered as a service please check your code to see why you are doing another registration.",
                    new RuntimeException("This instance of BattlecardDataSource as already been registered as a service please check your code to see why you are doing another registration."));
        }

        try {
            JahiaSitesService jahiaSitesService = BundleUtils.getOsgiService(JahiaSitesService.class, null);
            languages = BundleUtils.getOsgiService(JCRTemplate.class, null).doExecuteWithSystemSession(systemSession ->
                    jahiaSitesService.getSiteByKey(JahiaSitesService.SYSTEM_SITE_KEY, systemSession).getLanguages());
        } catch (RepositoryException e) {
            logger.error("", e);
            languages = Collections.emptySet();
        }
    }

    public void disconnect() {
        flush();
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
            logger.info("Successfully unregistered BattlecardDataSource Service");
        }
    }

    public void flush() {
        battlecardCacheManager.flush();
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        BattlecardProviderFactory.deleteMountPoint(mountPointNodeIdentifier);
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
                return BattlecardMapper.mapBattlecard(path, languages, sheetTitle, googleSheetService.isMasterSheet(sheetTitle));
            case 3:
                // /<sheet>/<category>
                return BattlecardMapper.mapBattlecardCategory(path, languages, getSheetData(pathes[1]).entrySet().stream()
                        .filter(entry -> pathes[2].equals(entry.getKey().getNodename())).findFirst()
                        .orElseThrow(() -> new PathNotFoundException(path)).getKey().getTitle());
            case 4:
                // /<sheet>/<category>/<keyValue-xxx>
                int index = Integer.parseInt(StringUtils.substringAfter(pathes[3], BattlecardMapper.KEYVALUE_PREFIX));
                Map.Entry<NodeValue, String> keyValue = getSheetData(pathes[1]).entrySet().stream()
                        .filter(entry -> pathes[2].equals(entry.getKey().getNodename())).findFirst()
                        .orElseThrow(() -> new PathNotFoundException(path))
                        .getValue().entrySet().stream()
                        .skip(index).limit(1).findAny()
                        .orElseThrow(() -> new PathNotFoundException(path));
                return BattlecardMapper.mapKeyValue(path, languages, keyValue.getKey().getTitle(), keyValue.getValue());
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
        List<NodeValue> sheets = battlecardCacheManager.getSheets(googleSheetService.getSpreadsheetId() + path);
        if (CollectionUtils.isEmpty(sheets)) {
            sheets = googleSheetService.getSheets().stream().map(NodeValue::new).collect(Collectors.toCollection(LinkedList::new));
            // cache sheets
            battlecardCacheManager.putSheets(googleSheetService.getSpreadsheetId() + path, sheets);
        }
        return sheets;
    }

    private Map<NodeValue, Map<NodeValue, String>> getSheetData(String sheet) {
        Map<NodeValue, Map<NodeValue, String>> data = battlecardCacheManager.getSheetData(googleSheetService.getSpreadsheetId() + sheet);
        if (MapUtils.isEmpty(data)) {
            data = BattlecardMapper.convertRowsToBattleCard(googleSheetService.getValues(sheet));
            // cache sheet data
            battlecardCacheManager.putSheetData(googleSheetService.getSpreadsheetId() + sheet, data);
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
