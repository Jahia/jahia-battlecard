package org.jahiacommunity.modules.battlecard.service;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRContentUtils;

import java.util.UUID;

public class NodeValue {
    private final String nodename;
    private final String title;

    public NodeValue(String title) {
        this.title = title;
        if (StringUtils.isBlank(title)) {
            nodename = JCRContentUtils.generateNodeName(UUID.randomUUID().toString());
        } else {
            nodename = JCRContentUtils.generateNodeName(title);
        }
    }

    public String getNodename() {
        return nodename;
    }

    public String getTitle() {
        return title;
    }
}
