# Jahia Battlecard

This repository is a sample code based on **EDP** (External Data Provider) implementation linked to a specific Google Sheet (see [Google Sheet API](https://developers.google.com/sheets/api/reference/rest)).

> Add JContent screenshot

## Specifications
* What is a battlecard ?
  * A battlecard is a succession of key:values grouped by category
```
[jcnt:battlecardFight] > jnt:content, mix:title, jmix:structuredContent, jmix:editorialContent, jmix:mainResource
 - masterBattlecard (weakreference) mandatory < 'jcnt:battlecard'
 - battlecard2 (weakreference) mandatory < 'jcnt:battlecard'

[jcnt:battlecard] > jnt:content, jmix:list, mix:title, jmix:structuredContent, jmix:editorialContent, jmix:mainResource orderable
 + * (jcnt:battlecardCategory) = jcnt:battlecardCategory

[jcnt:battlecardCategory] > jnt:content, jmix:list, mix:title, jmix:editorialContent orderable
 - isMasterCategory (boolean) = false autocreated
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
We can set up your configuration in the UI Administration > Modules and Extensions > Mount points
```
Credentials=<Google Service Account JSON credentials>
Google Cloud Project ID=<Google Project ID>
Google Spreadsheet ID=<Google Spreadsheet ID>
Google Sheets excluded=<Sheets title excluded from the EDP mountpoint, comma separated without space>
Mount path=<JCR Mount path>
Cron Expression to flush cache=<Cron expression>
```
