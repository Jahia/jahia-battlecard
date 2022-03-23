package org.jahiacommunity.modules.battlecard.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.IOUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.Patterns;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = GoogleSheetService.class, immediate = true)
public class GoogleSheetService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetService.class);

    private static final String ERROR_MESSAGE = "Service misconfigured";
    private static final String[] SCOPES = {"https://www.googleapis.com/auth/spreadsheets.readonly"};

    private JCRTemplate jcrTemplate;
    private JCRPublicationService jcrPublicationService;
    private JahiaUserManagerService jahiaUserManagerService;

    private String spreadsheetId;
    private String masterSheet;
    private String[] excludedSheets;
    private Sheets service;
    private Spreadsheet spreadsheet;

    @Activate
    private void onActivate(Map<String, ?> configuration) throws PathNotFoundException {
        if (MapUtils.isNotEmpty(configuration) && configuration.containsKey("credentials") && StringUtils.isNotBlank((String) configuration.get("credentials")) && configuration.containsKey("projectId") && StringUtils.isNotBlank((String) configuration.get("projectId")) && configuration.containsKey("spreadsheetId") && StringUtils.isNotBlank((String) configuration.get("spreadsheetId")) && configuration.containsKey("masterSheet") && StringUtils.isNotBlank((String) configuration.get("masterSheet")) && configuration.containsKey("excludedSheets") && StringUtils.isNotBlank((String) configuration.get("excludedSheets")) && configuration.containsKey("siteKey") && StringUtils.isNotBlank((String) configuration.get("siteKey"))) {
            String credentials = (String) Objects.requireNonNull(configuration.get("credentials"));
            String projectId = (String) Objects.requireNonNull(configuration.get("projectId"));
            spreadsheetId = (String) Objects.requireNonNull(configuration.get("spreadsheetId"));
            masterSheet = (String) Objects.requireNonNull(configuration.get("masterSheet"));
            String sheets = (String) configuration.get("excludedSheets");
            String siteKey = (String) Objects.requireNonNull(configuration.get("siteKey"));
            if (mountPoint(siteKey)) {
                if (sheets != null) {
                    sheets = sheets.replaceAll(" ", "");
                    if (sheets.length() == 0) {
                        excludedSheets = new String[0];
                    } else {
                        excludedSheets = Patterns.COMMA.split(sheets);
                    }
                }

                try {
                    ServiceAccountCredentials sourceCredentials = (ServiceAccountCredentials) ServiceAccountCredentials.fromStream(IOUtils.toInputStream(credentials)).createScoped(Arrays.asList(SCOPES));
                    service = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(sourceCredentials)).setApplicationName(projectId).build();
                    spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
                } catch (IOException | GeneralSecurityException e) {
                    logger.error("", e);
                }
            }
        }
    }

    @Reference
    private void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Reference
    private void setJcrPublicationService(JCRPublicationService jcrPublicationService) {
        this.jcrPublicationService = jcrPublicationService;
    }

    @Reference
    private void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    private boolean mountPoint(String siteKey) {
        try {
            return jcrTemplate.doExecuteWithSystemSessionAsUser(jahiaUserManagerService.lookupRootUser().getJahiaUser(), Constants.EDIT_WORKSPACE, Locale.ENGLISH, systemSession -> {
                if (!systemSession.nodeExists("/sites/" + siteKey)) {
                    logger.warn("Site [{}] not found", siteKey);
                    return false;
                }

                JCRNodeWrapper battlecardsNode;
                if (systemSession.nodeExists("/sites/" + siteKey + "/contents/battlecards")) {
                    battlecardsNode = systemSession.getNode("/sites/" + siteKey + "/contents/battlecards");
                } else {
                    battlecardsNode = systemSession.getNode("/sites/" + siteKey + "/contents")
                            .addNode("battlecards", "jnt:contentFolder");
                    battlecardsNode.setProperty(Constants.JCR_TITLE, "Battlecards");
                    battlecardsNode.saveSession();
                    jcrPublicationService.publish(Collections.singletonList(battlecardsNode.getIdentifier()),
                            Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, Collections.emptyList());
                }

                JCRNodeWrapper mountPointNode;
                if (systemSession.nodeExists("/mounts/battlecardMountPoint")) {
                    mountPointNode = systemSession.getNode("/mounts/battlecardMountPoint");
                } else {
                    mountPointNode = systemSession.getNode("/mounts")
                            .addNode("battlecardMountPoint", "jcnt:battlecardMountPoint");
                }
                if (!mountPointNode.hasProperty(JCRMountPointNode.MOUNT_POINT_PROPERTY_NAME) ||
                        !battlecardsNode.getIdentifier().equals(
                                mountPointNode.getProperty(JCRMountPointNode.MOUNT_POINT_PROPERTY_NAME).getNode().getIdentifier())) {
                    mountPointNode.setProperty(JCRMountPointNode.MOUNT_POINT_PROPERTY_NAME, battlecardsNode.getIdentifier());
                    mountPointNode.saveSession();
                }
                return true;
            });
        } catch (RepositoryException e) {
            logger.error("", e);
            return false;
        }
    }

    @Deactivate
    private void onDeactivate() {
        spreadsheet = null;
        service = null;
    }

    private void validateService() throws PathNotFoundException {
        if (service == null || spreadsheet == null || spreadsheet.isEmpty()) {
            logger.warn(ERROR_MESSAGE);
            throw new PathNotFoundException();
        }
    }

    public List<String> getSheets() {
        try {
            validateService();
            return spreadsheet.getSheets().stream().map(sheet -> sheet.getProperties().getTitle())
                    .filter(title -> !Arrays.asList(excludedSheets).contains(title)).collect(Collectors.toCollection(LinkedList::new));
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    public List<List<Object>> getValues(String sheet) {
        logger.info("Sheet: {}", sheet);
        try {
            validateService();
            return service.spreadsheets().values().get(spreadsheetId, sheet).execute().getValues().stream().skip(1)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (PathNotFoundException | IOException e) {
            // Do nothing
            return Collections.emptyList();
        }
    }

    public boolean isMasterSheet(String sheet) {
        try {
            validateService();
        } catch (PathNotFoundException e) {
            return false;
        }
        return masterSheet.equals(sheet);
    }
}
