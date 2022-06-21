package org.jahiacommunity.modules.battlecard.service;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class BattlecardMapper {
    private static final Logger logger = LoggerFactory.getLogger(BattlecardMapper.class);

    private static final String BATTLECARD_NODETYPE = "jcnt:battlecard";
    private static final String BATTLECARDCATEGORY_NODETYPE = "jcnt:battlecardCategory";
    private static final String KEYVALUE_NODETYPE = "jcnt:battlecardKeyValue";
    public static final String KEYVALUE_PREFIX = "keyValue-";
    private static final String KEYVALUE_KEY_PROPERTY = "key";
    private static final String KEYVALUE_VALUE_PROPERTY = "value";
    private static final String ISMASTERCATEGORY_PROPERTY = "isMasterCategory";
    private static final String ISMASTERCATEGORY_DISCRIMINATOR = "*";
    private static final int EDP_COLUMN = 3;

    public static ExternalData mapRootNode(String path) {
        return new ExternalData(path, path, "jnt:contentFolder", Collections.emptyMap());
    }

    public static ExternalData mapBattlecard(String path, Set<String> languages, String title) {
        ExternalData externalData = new ExternalData(path, path, BATTLECARD_NODETYPE, Collections.emptyMap());
        externalData.setI18nProperties(languages.stream().collect(
                Collectors.toMap(language -> language, language -> Collections.singletonMap(Constants.JCR_TITLE, new String[]{title}))));
        return externalData;
    }

    public static ExternalData mapBattlecardCategory(String path, Set<String> languages, String title) {
        ExternalData externalData = new ExternalData(path, path, BATTLECARDCATEGORY_NODETYPE,
                Collections.singletonMap(ISMASTERCATEGORY_PROPERTY,
                        new String[]{Boolean.toString(StringUtils.startsWith(title, ISMASTERCATEGORY_DISCRIMINATOR))}));
        externalData.setI18nProperties(languages.stream().collect(
                Collectors.toMap(language -> language, language -> Collections.singletonMap(Constants.JCR_TITLE, new String[]{
                        StringUtils.startsWith(title, ISMASTERCATEGORY_DISCRIMINATOR) ? StringUtils.substringAfter(title, ISMASTERCATEGORY_DISCRIMINATOR)
                                : title}))));
        return externalData;
    }

    public static ExternalData mapKeyValue(String path, Set<String> languages, String key, String value) {
        ExternalData externalData = new ExternalData(path, path, KEYVALUE_NODETYPE, Collections.emptyMap());
        Map<String, String[]> i18nProperties = new HashMap<>();
        i18nProperties.put(KEYVALUE_KEY_PROPERTY, new String[]{key});
        i18nProperties.put(KEYVALUE_VALUE_PROPERTY, new String[]{value});
        externalData.setI18nProperties(languages.stream().collect(Collectors.toMap(language -> language, language -> i18nProperties)));
        return externalData;
    }

    public static Map<NodeValue, Map<NodeValue, String>> convertRowsToBattleCard(List<List<Object>> rows) {
        Map<NodeValue, Map<NodeValue, String>> data = new LinkedHashMap<>();
        String category = null;
        NodeValue nodeCategory = null;
        String key, value;
        for (List<Object> row : rows) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}}", row);
            }

            if (row.size() > EDP_COLUMN && row.get(EDP_COLUMN) != null && BooleanUtils.toBoolean((String) row.get(EDP_COLUMN))) {
                if (row.get(0) != null && StringUtils.isNotBlank((String) row.get(0))) {
                    category = (String) row.get(0);
                    nodeCategory = new NodeValue(category);
                    data.put(nodeCategory, new LinkedHashMap<>());
                } else if (category == null) {
                    logger.warn("Ignore row: {}", row);
                    break;
                }

                key = (String) row.get(1);
                value = (String) row.get(2);
                if (StringUtils.isNotBlank(key) || StringUtils.isNotBlank(value)) {
                    data.get(nodeCategory).put(new NodeValue(key), value);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Ignore row with empty values: {}", row);
                }
            }
        }
        return data;
    }
}
