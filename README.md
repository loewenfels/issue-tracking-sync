[![SonarCloud Status](https://sonarcloud.io/api/project_badges/measure?project=loewenfels_issue-tracking-sync&metric=alert_status)](https://sonarcloud.io/dashboard?id=loewenfels_issue-tracking-sync)

# Synchronizing issues between multiple tracking platforms

This tool synchronizes issues between [Atlassian JIRA](https://www.atlassian.com/software/jira) and [IBM RTC](https://jazz.net/products/rational-team-concert/). 
Synchronisation is configured field-per-field in one JSON file. The tool runs as a small web server to allow for processing of (JIRA) webhook calls. 

## Simple Build
To build the tool you need the IBM RTC Library. You can achieve this with two approaches.
1. Download and install the Library from IBM
2. Add a mvn Repository to your local ~/.gradle/gradle.properties as the following parameter ```repositoryIssueTrackingJars=<URL_TO_MVN_REPO>```.

To build the tool via Docker you have to have a mvn Repository url. Build and start the Docker image with the following commands:
 ```
    docker build --build-arg MVN_REPO=<URL_TO_MVN_REPO> -t issue-tracking-sync .
    docker run -p 8080:8080 issue-tracking-sync
```
This Dockerfile has 3 more build arguments which can get passed in. They are used to change the application.yml and settings.json file.
The build args are:
```
SETTINGSFILE=<PATH_TO_SETTINGSFILE>
SETTINGSTARGET=<PATH_WHERE_SETTINGSFILE_SHOULD_GET_STORED>
APPLICATIONFILE=<PATH_TO_APPLICATIONFILE>
```

## Configuration

The application will look for a file named application.properties or application.yml in a

    * /config 

subdirectory of the current directory.

Place a file in either of the two locations, and define the path to your settings JSON along with application-wide properties:

```
spring:
  security:
    user:
      password: a-better-admin-password

sync:
  settingsLocation: /opt/issue-tracking-sync/config/settings.json
  pollingCron: 0 0/15 0 * * ?
  notificationChannelProperties:
    - classname: ch.loewenfels.issuetrackingsync.notification.SlackChannel
      endpoint: https://hooks.slack.com/services/mywebhookthingy
      username: The sync tool
      subject: jira2rtc
      avatar: ":cyclone:"
```

Beware that setting username, subject and avatar only works on legacy Slack webhooks.

Finally, define the settings.json. An example file can be found [here](https://github.com/loewenfels/issue-tracking-sync/blob/master/src/test/resources/settings.json).

### settings.json

Optionally, set a `earliestSyncDate` in the format `yyyy-MM-ddTHH:mm:ss`. If provided, polling will initially seek for 
issues updated after `earliestSyncDate`. If nothing is defined, polling will start with application start.

#### trackingApplications

Define the basic issue tracking applications in use in the section `trackingApplications` of the settings.json 

```json
{
  "trackingApplications": [
    {
      "name": "JIRA",
      "className": "ch.loewenfels.issuetrackingsync.syncclient.jira.JiraClient",
      "username": "foobar",
      "password": "mysecret",
      "endpoint": "http://localhost:8080/jira",
      "polling": false
    },
    {
      "name": "RTC",
      "className": "ch.loewenfels.issuetrackingsync.syncclient.rtc.RtcClient",
      "username": "rtcfoobar",
      "password": "anothersecret",
      "endpoint": "http://localhost:8081/rtc",
      "project": "Issues Löwenfels (RTC)"
    }
  ]
}
```

The currently defined client implementations are:
- `ch.loewenfels.issuetrackingsync.syncclient.jira.JiraClient`
- `ch.loewenfels.issuetrackingsync.syncclient.rtc.RtcClient`


#### fieldMappingDefinitions

A field mapping definition works on the smallest possible synchronization level, typically a single field.
The `fieldMappingDefinitions` allows for a quite generic approach to synchronizing simple fields.

Fields available in RTC

| Property | Read as | Write as |
| -------- | ------- | -------- |
| archived | archived | - |
| category | category | category |
| com.ibm.team.apt.attribute.acceptance | com.ibm.team.apt.attribute.acceptance | com.ibm.team.apt.attribute.acceptance |
| com.ibm.team.apt.attribute.complexity | com.ibm.team.apt.attribute.complexity | com.ibm.team.apt.attribute.complexity |
| com.ibm.team.apt.estimate.maximal | com.ibm.team.apt.estimate.maximal | com.ibm.team.apt.estimate.maximal |
| com.ibm.team.apt.estimate.minimal | com.ibm.team.apt.estimate.minimal | com.ibm.team.apt.estimate.minimal |
| com.ibm.team.rtc.attribute.affectedTeams | com.ibm.team.rtc.attribute.affectedTeams | com.ibm.team.rtc.attribute.affectedTeams |
| com.ibm.team.rtc.attribute.impact | com.ibm.team.rtc.attribute.impact | com.ibm.team.rtc.attribute.impact |
| contextId | contextId | contextId |
| correctedEstimate | correctedEstimate | correctedEstimate |
| creationDate | creationDate | - |
| creator | creator | - |
| description | HTMLDescription.plainText <br/> HTMLDescription (_using HtmlToWikiFieldMapper_) | description |
| dueDate | dueDate | dueDate |
| duration | duration | duration |
| foundIn | foundIn | foundIn |
| id | id | - |
| internalApprovalDescriptors | internalApprovalDescriptors | internalApprovalDescriptors |
| internalApprovals | internalApprovals | internalApprovals |
| internalComments | internalComments | internalComments |
| internalPriority | internalPriority | internalPriority |
| internalResolution | internalResolution | internalResolution |
| internalSequenceValue | internalSequenceValue | internalSequenceValue |
| internalSeverity | internalSeverity | internalSeverity |
| internalState | internalState | internalState |
| internalStateTransitions | internalStateTransitions | internalStateTransitions |
| internalSubscriptions | internalSubscriptions | internalSubscriptions |
| internalTags | internalTags | internalTags |
| modified | modified | - |
| modifiedBy | modifiedBy | - |
| owner | owner | owner |
| projectArea | projectArea | - |
| resolutionDate | resolutionDate | - |
| resolver | resolver | resolver |
| startDate | startDate | - |
| summary | HTMLSummary.plainText <br/> HTMLSummary (_using HtmlToWikiFieldMapper_) | summary |
| target | target | target |
| timeSpent | timeSpent | timeSpent |
| correctedEstimate | correctedEstimate | correctedEstimate |
| duration | duration | duration |
| workItemType | workItemType | - |

Additionally on RTC, custom fields can be read/written using the internal FQN (eg. `ch.loewenfels.team.workitem.attribute.defectdescription`).

Fields available in JIRA:

| Property | Read as | Write as |
| -------- | ------- | -------- |
| affectedVersionsNames | affectedVersionsNames | affectedVersionsNames |
| assignee | assignee | assignee |
| assigneeName | assigneeName | assigneeName |
| description | description | description |
| dueDate | dueDate | dueDate |
| fixVersionsNames | fixVersionsNames | fixVersionsNames |
| priority.name | priority.name | priority.name |
| priority.id | priority.id | priority.id |
| priorityId | priorityId | priorityId |
| reporter | reporter | reporter |
| reporterName | reporterName | reporterName |
| resolution.id | resolution.id | resolution.id |
| resolution.name | resolution.name | resolution.name |
| summary | summary | summary |
| status.id | status.id | status.id |
| status.name | status.name | status.name |
| creationDate | creationDate | creationDate |
| timeTracking.originalEstimateMinutes | timeTracking.originalEstimateMinutes | timeTracking.originalEstimateMinutes |
| timeTracking.remainingEstimateMinutes | timeTracking.remainingEstimateMinutes | timeTracking.remainingEstimateMinutes |
| timeTracking.timeSpentMinutes | timeTracking.timeSpentMinutes | timeTracking.timeSpentMinutes |

Additionally on JIRA, custom field can be read/written using the internal name (like `customfield_123456`) or the display name 
(like `Customer reference`).

##### Field mappers

The default field mapper is `ch.loewenfels.issuetrackingsync.executor.fields.DirectFieldMapper`, which attempts to
read the property `sourceName` and write it to `targetName`.

`ch.loewenfels.issuetrackingsync.executor.fields.HtmlToWikiFieldMapper` is useful for rich text fields which allow for
markup in JIRA, and/or HTML in RTC (eg. RTC 'description'). 

`ch.loewenfels.issuetrackingsync.executor.fields.CompoundStringFieldMapper` extends `HtmlToWikiFieldMapper` and can be 
used to map multiple text fields onto a single text field, and split it back. This mapper expects `associations` for 
each field definition except one (which will hold "the rest").

To merge multiple fields:
```json
{
  "sourceName": "ch.loewenfels.team.workitem.attribute.requirement,ch.loewenfels.team.workitem.attribute.defectdescription,ch.loewenfels.team.workitem.attribute.expected.conduct",
  "targetName": "description",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.CompoundStringFieldMapper",
  "associations": {
    "ch.loewenfels.team.workitem.attribute.defectdescription": "<h4>Error description</h4>",
    "ch.loewenfels.team.workitem.attribute.expected.conduct": "<h4>Expected behaviour</h4>"
  }
}
```
To split into multiple fields:
```json
{
  "sourceName": "description",
  "targetName": "ch.loewenfels.team.workitem.attribute.requirement,ch.loewenfels.team.workitem.attribute.defectdescription,ch.loewenfels.team.workitem.attribute.expected.conduct",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.CompoundStringFieldMapper",
  "associations": {
    "ch.loewenfels.team.workitem.attribute.defectdescription": "<h4>Error description</h4>",
    "ch.loewenfels.team.workitem.attribute.expected.conduct": "<h4>Expected behaviour</h4>"
  }
}
```

Note that the separator associations are in HTML, not in JIRA wiki markup. This is due to the mapper extending HtmlToWikiFieldMapper,
so it works internally with HTML. 

The `ch.loewenfels.issuetrackingsync.executor.fields.LinkToIssueFieldMapper` disregards the 'sourceName' and provides a 
link to the source issue. It can be written to any 'targetName'.

The `ch.loewenfels.issuetrackingsync.executor.fields.PriorityAndSeverityFieldMapper` is a slightly more complicated mapper,
as JIRA typically knows 1 priority field, while RTC has a priority and severity. This mapper uses the `associations` 
as a matrix to map between these fields:
```json
{
  "sourceName": "priority,severity",
  "targetName": "priorityId",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.PriorityAndSeverityFieldMapper",
  "associations": {
    "P1 - Critical,S1 - Minor": "Normal",
    "P2 - Critical,S1 - Major": "High",
    "P3 - Critical,S1 - Urgent": "Blocker"
  }
}
```

The above example maps RTC priority and severity to JIRA priority. The opposite way would be:

 ```json
{
  "sourceName": "priority.name",
  "targetName": "internalPriority,internalSeverity",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.PriorityAndSeverityFieldMapper",
  "associations": {
    "Klein": "P4 - Niedrig,S1 - Geringfügig",
    "Normal": "P3 - Mittel,S2 - Normal",
    "Hoch": "P1 - Kritisch,S3 - Bedeutend",
    "Zwingend": "P1 - Kritisch,S4 - Kritisch",
    "Blocker": "P2 - Hoch,S5 - Blockierend",
    "*,*": "P3 - Mittel,S2 - Normal"
  }
}
 ```

For multi selection fields use the `ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper`. 
This mapper uses the `associations` as a matrix to map between these fields. The following example shows a mapping from RTC to Jira:
```json
{
  "sourceName": "cantons",
  "targetName": "customfield_11342",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper",
  "associations": {
    "Luzern": "LU",
    "Uri": "UR",
    "Basel-Stadt": "BS"
  }
}
```
The opposite way, from Jira to RTC, would be as follows:
```json
{
  "sourceName": "customfield_11342",
  "targetName": "cantons",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper",
  "associations": {
    "LU": "Luzern",
    "UR": "Uri",
    "BS": "Basel-Stadt"
  }
}
```

To enhance this mapper with additional values, which are only present at one client-side, use the optional `additionalAssociations` matrix.
Please note that this option will only work with uni-directional unique keys! 
In the following example the keys "BS" and "BL" will be mapped to "Basel":

```json
{
  "sourceName": "customfield_11342",
  "targetName": "cantons",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper",
  "associations": {
    "LU": "Luzern",
    "UR": "Uri",
    "BS": "Basel"
  },
  "additionalAssociations": {
    "BL": "Basel"
  }
}
```

The following will not work, because the key "BS" is not unique:
```json
{
  "sourceName": "customfield_11342",
  "targetName": "cantons",
  "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper",
  "associations": {
    "LU": "Luzern",
    "UR": "Uri",
    "BS": "Basel"
  },
  "additionalAssociations": {
    "BS": "Basel-Stadt",
    "BL": "Basel"
  }
}
```

The `ch.loewenfels.issuetrackingsync.executor.fields.SingleSelectionFieldMapper` is similar like the above 
MultiSelectionFieldMapper: it also uses an `associations` matrix for mapping single selection fields between both clients.
The `association` matrix shall have the same structure as the above MultiSelectionFieldMapper.


The `ch.loewenfels.issuetrackingsync.executor.fields.StatusFieldMapper` synchronizes state transitions, assuming that 
source and target client systems have a rough association between their resp. workflows

1. Gets the current state of the source issue (`getValue`)
2. Gets the state changelog for the source issue (`getValue`)

Then, in `setValue` the source's changelog is used to
1. Locate the changelog entry which has a state matching the target issue's current state (using  the fields association map to translate source states to target states)
2. For each changelog entry later than the one found in (1), retrieve the target state, seek a transition leading to that state, and transition the target issue 

There are a few twists
* JRJC easily provides available transitions for an issue via `jiraRestClient.issueClient.getTransitions(myIssue)`, but this contains no information on the target state of the transition. Thus, the `AsynchronousHtmlRenderingRestClient` uses a JSON response from the API's /transitions response to find a suitable transition for a target state
* If a source state changelog entry contains a state unknown in the target system, synchronization fails
* If the target system has multiple transitions for a single one on the source side, the field association can define a comma-separated list of target states

##### Re-using associations

When defining multiple, similar sync-actions, the same field mappers and esp. the same `associations` are often used. 
To re-use such `associations`, define `common` mappings in the settings.json. It is good practice to indicate the
direction of the mapping.

```json
{
  "common": {
    "cantonsFromThemToUs": {
      "Unterwalden": "NW,OW",
      "Appenzell": "AI,AR",
      "AI": "AI",
      "AR": "AR",
      ...
    }
  },
```

The common entries can then be used by setting a **#common** association pointing to the entry name.

```json
  {
    "sourceName": "cantons",
    "targetName": "customfield_11342",
    "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper",
    "associations": {"#common": "cantons"}
  },
  {
    "sourceName": "customfield_11342",
    "targetName": "cantons",
    "mapperClassname": "ch.loewenfels.issuetrackingsync.executor.fields.MultiSelectionFieldMapper",
    "associations": {"#common": "cantons->reversed"}
  }
}
```

By adding `->reversed` to the entry, the map is reversed.

#### actionDefinitions

An action definition represents a synchronization sequence, similar to a macro. Typically, multiple field mappers are
combined to define an action. Multiple actions can then be stringed together to form a synchronization flow. By defining
actions separately, they can be re-used in multiple flows. 

```json
{
  "actionDefinitions": [
    {
      "name": "SimpleFieldsRtcToJira",
      "classname": "ch.loewenfels.issuetrackingsync.executor.SimpleSynchronizationAction",
      "fieldMappingDefinitions": [
        {
          "sourceName": "summary",
          "targetName": "title"
        }
      ]
    },
    {
      "name": "SynchronizeComments",
      "classname": "ch.loewenfels.issuetrackingsync.executor.actions.CommentsSynchronizationAction",
      "additionalProperties": {
        "preComment": "on ${time} ${date} someone wrote the following text: \n",
        "postComment": "\n ${author} was the author"
      }
    }
  ]
}
```

- `ch.loewenfels.issuetrackingsync.executor.SimpleSynchronizationAction` reads a list of `fieldMappingDefinitions` (see below)
- `ch.loewenfels.issuetrackingsync.executor.actions.CommentsSynchronizationAction` adds all comments present in the source 
  client but missing on the target (equality is based on source comment text being found in target comment text or vice versa)
- `ch.loewenfels.issuetrackingsync.executor.actions.AttachmentsSynchronizationAction` adds all attachments present in the source 
  client but missing on the target (equality is based on content hash)
- **Experimental feature**: `ch.loewenfels.issuetrackingsync.executor.actions.StateSynchronizationAction` updates the state 
  of the given Issue/WorkItem. Master is the client with the newest changes. Currently only Jira-Issues will be synchronized. 
  RTC WorkItems will be synchronized in future releases.

Hint: if comment visibility is an issue, make sure the users defined in `trackingApplications` have access only to the 
comments which should by synchronized.

#### syncFlowDefinitions

Individual synchronization streams are defined as "flows". Apart from defining the source and target application
(referring to the trackingApplication#name), the configuration *can* define a filter class, and *must* define a
list of action references. 

```json
{
  "syncFlowDefinitions": [
    {
      "name": "Sync changes from RTC to JIRA",
      "source": "RTC",
      "target": "JIRA",
      "filterClassname": "ch.loewenfels.issuetrackingsync.custom.UnclosedChangeFilter",
      "keyFieldMappingDefinition": {
        "sourceName": "id",
        "targetName": "custom_field_12044"
      },
      "writeBackFieldMappingDefinition": {
        "sourceName": "key",
        "targetName": "ch.loewenfels.team.workitem.attribute.external_refid"
      },
      "defaultsForNewIssue": {
        "issueType": "change",
        "project": "TST",
        "category": ""
      },
      "actions": [
        "SimpleFieldsRtcToJira",
        "SynchronizeComments"
      ]
    }
  ]
}
```

The mandatory `keyFieldMappingDefinition` is used to load an issue, and thus has no issue or project context. As JIRA
allows for multiple custom fields to have identical names (in different projects), the field names here 
**must be defined by their internal name**.

The optional `writeBackFieldMappingDefinition` allows to define a write-back of the target key to the source issue.
In the example above, an RTC issue is synchronized to JIRA, but the JIRA `key` is written back to the RTC issue
in field `ch.loewenfels.team.workitem.attribute.external_refid`.

The optional `defaultsForNewIssue` defines defaults for new issues. If missing, and no target issue is found using
`keyFieldMappingDefinition`, synchronization will abort.

Finally, the list of `actions` refers to the `name` attribute of the [actionDefinitions](#actionDefinitions).

### Processing queue

Individual synchronization requests are processed in a queue (backed by ActiveMQ) with concurrency of one. This ensures 
that no two synchronization threads might affect the same items (configure in application.yml).

See the IssuePoller as an entry point, which runs based on a CRON expression. Found issues are then run through
the SynchronizationFlowFactory, and for all issues with a matching flow, a SyncRequest is produced on the queue.

### Processing a single issue

From a SyncRequest, an issue is derived and the matching SynchronizationFlow is retrieved from the 
SynchronizationFlowFactory. As described in [syncFlowDefinitions](#syncflowdefinitions), a SynchronizationFlow can
define an issue filter, and must define a collection of [actionDefinitions](#actionDefinitions). 

1. Load the source issue along with the key (=unique identifier) mapping. This step also verifies that the 
   "last updated" timestamp of the synchronization request matches that of the loaded issue (if not, a 
   SynchronizationAbortedException is thrown).
2. Locate a fitting sync flow, applying filters where defined.
3. If one (1) sync flow is found, call that flow with the loaded issue.
    1. If the flow cannot locate a target issue, and the flow doesn't define `defaultsForNewIssue`, a 
       SynchronizationAbortedException is thrown.

### Custom classes

The `settings.json` works with class FQNs, which must be present on the classpath, but not necessarily in this
project. If custom field mappers, filters etc. are needed, they can be provided in a separate JAR. If those implementations
might be of value to others, add them to this project in the `ch.loewenfels.issuetrackingsync.custom` package.   

### Controllers

The `DebugController` offers resources to access debug-relevant information from the tool while it is running.
Under `foobar.com/config` (while `foobar.com` is the issue-tracking-sync-tool) the `settings.json` can be accessed. 
To ensure privacy, the default implementation masks passwords with asterisks.
Under `foobar.com/log` the log file of the tool can be downloaded. 
This resource provides also a date-parameter for accessing past log files. The date-parameter has the following pattern: 
`yyyy-MM-dd`. Hence, to access the log file from February 18<sup>th</sup> 2020, the URL would be `foobar.com/log2020-02-18`

## For contributors

This project aims at allowing for simple issue synchronization using a purely configurative approach, while also embracing 
proprietary extensions. The latter can be defined using class FQNs in the settings.json, and making sure the application
finds the class on the classpath (but possibly outside of this project's fat JAR).