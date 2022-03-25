package org.jahiacommunity.modules.battlecard.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GoogleSheetService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetService.class);

    private static final String ERROR_MESSAGE = "Service misconfigured";
    private static final String[] SCOPES = {"https://www.googleapis.com/auth/spreadsheets.readonly"};

    private final String spreadsheetId;
    private final String masterSheet;
    private final String[] excludedSheets;
    private final Sheets service;
    private final Spreadsheet spreadsheet;

    public GoogleSheetService(String credentials, String projectId, String spreadsheetId, String masterSheet, String[] excludedSheets) throws GeneralSecurityException, IOException {
        this.spreadsheetId = spreadsheetId;
        this.masterSheet = masterSheet;
        this.excludedSheets = excludedSheets;

        ServiceAccountCredentials sourceCredentials = (ServiceAccountCredentials) ServiceAccountCredentials.fromStream(IOUtils.toInputStream(credentials, StandardCharsets.UTF_8)).createScoped(Arrays.asList(SCOPES));
        service = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(sourceCredentials)).setApplicationName(projectId).build();
        spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
    }

    private void validateService() throws PathNotFoundException {
        if (service == null || spreadsheet == null || spreadsheet.isEmpty()) {
            logger.warn(ERROR_MESSAGE);
            throw new PathNotFoundException();
        }
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
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
