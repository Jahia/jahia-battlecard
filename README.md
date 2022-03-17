# Jahia Battlecard

This repository is a sample code based on **EDP** (External Data Provider) implementation linked to a specific Google Sheet (see [Google Sheet API](https://developers.google.com/sheets/api/reference/rest)).

> Add JContent screenshot

## Specifications
* What is a battlecard ?
  * A battlecard is a succession of key:values grouped by category
```
[jcnt:battlecard] > jnt:content, jmix:list, mix:title, jmix:structuredContent, jmix:mainResource orderable
- isMasterSheet (boolean) = false autocreated
+ * (jcnt:battlecardCategory) = jcnt:battlecardCategory

[jcnt:battlecardCategory] > jnt:content, jmix:list, mix:title, jmix:editorialContent orderable
+ * (jcnt:battlecardKeyValue) = jcnt:battlecardKeyValue

[jcnt:battlecardKeyValue] > jnt:content, jmix:editorialContent
- key (string) i18n
- value (string) i18n
```
* Where can I find the battlecards ?
  * All battlecards are mounted in a specific content folder under */sites/sytemsite/contents/battlecards* just after the deployment of the module.

* How the cache is managed ?
  * The battlecards are cached eternally.
An UI extension adds a new action in [3dots menu](https://academy.jahia.com/documentation/developer/jahia/8/extending-and-customizing-jahia-ui/customizing-content-editor-forms/extending-content-editor-ui#Add_a_menu_entry_to_the_3_dots_menu_in_header) to flush the specific cache.
> Add JContent screenshot

## How to setup Google API configuration
- Create a [GCP Account](https://console.cloud.google.com/)
- Create a project
- Go to *API & Services* to enable **Google Sheet API**
- Go to *IAM & Administration*
  - Create a service account
  - Download the credentials

## How to setup Battlecard configuration
Update the OSGI configuration: *org.jahiacommunity.modules.battlecard.service.GoogleSheetService.cfg*
```
credentials=<Google Service Account JSON credentials>
projectId=<Google Project ID>
spreadsheetId=<Google Spreadsheet ID>
masterSheet=<Sheet title tagged as master sheet>
excludedSheets=<Sheets title excluded from the EDP mountpoint, comma separated without space>
```
