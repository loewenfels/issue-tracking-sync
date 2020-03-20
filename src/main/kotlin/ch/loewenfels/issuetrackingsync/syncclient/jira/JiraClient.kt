package ch.loewenfels.issuetrackingsync.syncclient.jira

import ch.loewenfels.issuetrackingsync.Attachment
import ch.loewenfels.issuetrackingsync.Comment
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.StateHistory
import ch.loewenfels.issuetrackingsync.SynchronizationAbortedException
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import ch.loewenfels.issuetrackingsync.logger
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.IssueClientException
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication
import com.atlassian.jira.rest.client.api.RestClientException
import com.atlassian.jira.rest.client.api.domain.IssueField
import com.atlassian.jira.rest.client.api.domain.IssueFieldId
import com.atlassian.jira.rest.client.api.domain.TimeTracking
import com.atlassian.jira.rest.client.api.domain.Transition
import com.atlassian.jira.rest.client.api.domain.Version
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput
import com.atlassian.renderer.wysiwyg.converter.DefaultWysiwygConverter
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.io.IOUtils
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject
import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import org.springframework.http.HttpStatus
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * JIRA Java client, see (https://ecosystem.atlassian.net/wiki/spaces/JRJC/overview)
 */
open class JiraClient(private val setup: IssueTrackingApplication) :
    IssueTrackingClient<com.atlassian.jira.rest.client.api.domain.Issue>, Logging {
    private val backReferenceCommentId = "00000000"
    private val jiraRestClient = ExtendedAsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
        URI(setup.endpoint),
        setup.username,
        setup.password
    )

    override fun getProprietaryIssue(issueKey: String): com.atlassian.jira.rest.client.api.domain.Issue? {
        return getJiraIssue(issueKey)
    }

    override fun getProprietaryIssue(
        fieldName: String,
        fieldValue: String
    ): com.atlassian.jira.rest.client.api.domain.Issue? {
        val jql = if (fieldName.startsWith("customfield")) {
            val cfNumber = fieldName.substring(12)
            "cf[$cfNumber] ~ '$fieldValue'"
        } else {
            "$fieldName = '$fieldValue'"
        }
        val foundIssues = jiraRestClient.searchClient.searchJql(jql).claim().issues.toList()
        return when (foundIssues.size) {
            0 -> null
            // reload to get full issue incl. collections such as comments
            1 -> getProprietaryIssue(foundIssues[0].key)
            else -> throw IssueClientException("Query too broad, multiple issues found for $fieldValue")
        }
    }

    override fun getIssue(key: String): Issue? {
        return mapJiraIssue(getJiraIssue(key))
    }

    override fun getIssueFromWebhookBody(body: JsonNode): Issue {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][xxx][xx][X]")
        return Issue(
            body.get("issue")?.get("key")?.asText() ?: "",
            setup.name,
            OffsetDateTime.parse(
                body.get("issue")?.get("fields")?.get("updated")?.asText() ?: "",
                formatter
            ).toLocalDateTime()
        )
    }

    override fun getKey(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): String =
        internalIssue.key

    override fun getIssueUrl(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): String {
        val endpoint =
            if (setup.endpoint.endsWith("/")) setup.endpoint.substring(0, setup.endpoint.length - 1) else setup.endpoint
        return "$endpoint/browse/${internalIssue.key}"
    }

    override fun getLastUpdated(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(internalIssue.updateDate.millis), ZoneId.systemDefault())

    override fun getHtmlValue(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue, fieldName: String) =
        jiraRestClient.getHtmlRenderingRestClient().getRenderedHtml(internalIssue.key, fieldName)

    override fun getValue(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue, fieldName: String): Any? {
        val beanWrapper = BeanWrapperImpl(internalIssue)
        val internalValue = if (beanWrapper.isReadableProperty(fieldName))
            beanWrapper.getPropertyValue(fieldName)
        else
            getCustomFields(internalIssue, fieldName)
        return internalValue?.let { convertFromMetadataId(fieldName, it) }
    }

    override fun setValue(
        internalIssueBuilder: Any,
        issue: Issue,
        fieldName: String,
        value: Any?
    ) {
        logger().debug("Setting value $value on $fieldName")
        convertToMetadataId(fieldName, value)?.let {
            val beanWrapper = BeanWrapperImpl(internalIssueBuilder)
            if (beanWrapper.isWritableProperty(fieldName))
                beanWrapper.setPropertyValue(fieldName, it)
            else if (internalIssueBuilder is IssueInputBuilder) {
                val targetInternalIssue = (issue.proprietaryTargetInstance
                    ?: throw IllegalStateException("Need a target issue for custom fields")) as com.atlassian.jira.rest.client.api.domain.Issue
                if (fieldName == "timeTracking" && value is TimeTracking) {
                    setInternalFieldValue(internalIssueBuilder, IssueFieldId.TIMETRACKING_FIELD.id, value)
                } else if (fieldName == "labels" && value is List<*>) {
                    setInternalFieldValue(internalIssueBuilder, IssueFieldId.LABELS_FIELD.id, value)
                } else if (fieldName == "versions") {
                    // RTC allows only one version (field: foundIn) while Jira awaits a list of versions
                    setInternalFieldValue(internalIssueBuilder, IssueFieldId.AFFECTS_VERSIONS_FIELD.id, mutableListOf(value))
                } else {
                    setInternalFieldValue(internalIssueBuilder, targetInternalIssue, fieldName, it)
                }
            }
        }
    }

    override fun setHtmlValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, htmlString: String) {
        val convertedValue = DefaultWysiwygConverter().convertXHtmlToWikiMarkup(htmlString)
        setValue(internalIssueBuilder, issue, fieldName, convertedValue)
    }

    private fun convertToMetadataId(fieldName: String, value: Any?): Any? {
        return when (fieldName) {
            "priorityId" -> JiraMetadata.getPriorityId(value?.toString() ?: "", jiraRestClient)
            "issueTypeId" -> JiraMetadata.getIssueTypeId(value?.toString() ?: "", jiraRestClient)
            else -> value
        }
    }

    private fun convertFromMetadataId(fieldName: String, value: Any): Any {
        return when {
            "priorityId" == fieldName -> JiraMetadata.getPriorityName(value.toString().toLong(), jiraRestClient)
            "versions" == fieldName -> getFirstVersion(value)
            value is JSONObject -> value.get("value")
            else -> value
        }
    }

    /**
     *  Jira allows multiple affected versions, while RTC allows only one affected version.
     *  Because of this, only the first entry of the List will be used for synchronization.
     */
    private fun getFirstVersion(value: Any) = ((value as List<*>)[0] as Version).name ?: ""

    private fun getJiraIssue(key: String): com.atlassian.jira.rest.client.api.domain.Issue {
        return jiraRestClient.issueClient.getIssue(key).claim()
    }

    override fun createOrUpdateTargetIssue(
        issue: Issue,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        val targetKeyFieldname = issue.keyFieldMapping!!.getTargetFieldname()
        val targetIssueKey = issue.keyFieldMapping!!.getKeyForTargetIssue().toString()
        var targetIssue =
            (issue.proprietaryTargetInstance ?: if (targetIssueKey.isNotEmpty()) getProprietaryIssue(
                targetKeyFieldname,
                targetIssueKey
            ) else null) as com.atlassian.jira.rest.client.api.domain.Issue?
        when {
            targetIssue != null -> {
                updateTargetIssue(targetIssue, issue)
            }
            defaultsForNewIssue != null -> {
                createTargetIssue(defaultsForNewIssue, issue)
            }
            else -> throw SynchronizationAbortedException("No target issue found for $targetIssueKey, and no defaults for creating issue were provided")
        }
    }

    private fun createTargetIssue(
        defaultsForNewIssue: DefaultsForNewIssue,
        issue: Issue
    ): com.atlassian.jira.rest.client.api.domain.Issue {
        val issueType = JiraMetadata.getIssueTypeId(defaultsForNewIssue.issueType, jiraRestClient)
        val issueBuilder = IssueInputBuilder()
            .setIssueTypeId(issueType)
            .setProjectKey(defaultsForNewIssue.project)
        //  here we need to delay custom fields until issue has been created!
        val beanWrapper = BeanWrapperImpl(issueBuilder)
        issue.fieldMappings.filter { beanWrapper.isWritableProperty(it.targetName) }
            .forEach {
                it.setTargetValue(issueBuilder, issue, this)
            }
        defaultsForNewIssue.additionalFields.multiselectFields.forEach {
            val value = ComplexIssueInputFieldValue.with("value", it.value)
            issueBuilder.setFieldValue(it.key, Collections.singletonList(value))
        }
        defaultsForNewIssue.additionalFields.enumerationFields.forEach {
            val value = ComplexIssueInputFieldValue.with("value", it.value)
            issueBuilder.setFieldValue(it.key, value)
        }
        defaultsForNewIssue.additionalFields.simpleTextFields.forEach {
            issueBuilder.setFieldValue(it.key, it.value)
        }
        val basicIssue = jiraRestClient.issueClient.createIssue(issueBuilder.build()).claim()
        logger().info("Created new JIRA issue ${basicIssue.key}")
        issue.workLog.add("Created new JIRA issue ${basicIssue.key}")
        val targetIssue =
            getProprietaryIssue(basicIssue.key) ?: throw IssueClientException("Failed to locate newly created issue")

        updateTargetIssue(targetIssue, issue)
        return targetIssue
    }

    private fun updateTargetIssue(targetIssue: com.atlassian.jira.rest.client.api.domain.Issue, issue: Issue) {
        val issueBuilder = IssueInputBuilder()
        setTargetPropertiesOnSyncIssue(targetIssue, issue)

        issue.fieldMappings.forEach {
            it.setTargetValue(issueBuilder, issue, this)
        }
        logger().info("Updating JIRA issue ${targetIssue.key}")
        val issueInput = issueBuilder.build()
        try {
            jiraRestClient.issueClient.updateIssue(targetIssue.key, issueInput).claim()
        } catch (e: RuntimeException) {
            issue.workLog.add("Failed to update using issue input: $issueInput")
            throw e
        }
    }

    private fun setTargetPropertiesOnSyncIssue(
        targetIssue: com.atlassian.jira.rest.client.api.domain.Issue,
        issue: Issue
    ) {
        issue.proprietaryTargetInstance = targetIssue
        issue.targetKey = getKey(targetIssue)
        issue.targetUrl = getIssueUrl(targetIssue)
    }

    override fun changedIssuesSince(
        lastPollingTimestamp: LocalDateTime,
        batchSize: Int,
        offset: Int
    ): Collection<Issue> {
        val lastPollingTimestampAsString =
            lastPollingTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        var jql = "(updated >= '$lastPollingTimestampAsString' OR created >= '$lastPollingTimestampAsString')"
        setup.project?.let { jql += " AND project = '$it'" }
        setup.pollingJqlFilter?.let { jql += " AND $it" }
        return jiraRestClient.searchClient
            .searchJql("$jql ORDER BY key", batchSize, offset, setOf("*all"))
            .claim()
            .issues
            .map { mapJiraIssue(it) }
            .toList()
    }

    override fun getComments(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): List<Comment> =
        jiraRestClient.getHtmlRenderingRestClient().getHtmlComments(internalIssue.key)
            .plusElement(
                createLinkComment(internalIssue)
            )

    private fun createLinkComment(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): Comment {
        // Because a direct link in the other tracking application to this Jira issue would be nice
        return Comment(
            setup.username,
            toLocalDateTime(internalIssue.creationDate),
            "Jira link: " + setup.endpoint + "/browse/" + internalIssue.key,
            backReferenceCommentId
        )
    }

    override fun addComment(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue, comment: Comment) {
        val convertedValue = DefaultWysiwygConverter().convertXHtmlToWikiMarkup(comment.content)
        val jiraComment = com.atlassian.jira.rest.client.api.domain.Comment.valueOf(convertedValue)
        jiraRestClient.issueClient.addComment(internalIssue.commentsUri, jiraComment).claim()
    }

    override fun getAttachments(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): List<Attachment> =
        internalIssue.attachments?.map { jiraAttachment ->
            val attachmentInputStreamPromise = jiraRestClient.issueClient.getAttachment(jiraAttachment.contentUri)
            attachmentInputStreamPromise.get().use {
                Attachment(
                    jiraAttachment.filename,
                    IOUtils.toByteArray(it)
                )
            }
        } ?: listOf()

    override fun addAttachment(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue, attachment: Attachment) {
        jiraRestClient.issueClient.addAttachment(
            internalIssue.attachmentsUri,
            ByteArrayInputStream(attachment.content),
            attachment.filename
        ).claim()
    }

    override fun getMultiSelectValues(
        internalIssue: com.atlassian.jira.rest.client.api.domain.Issue,
        fieldName: String
    ): List<String> {
        val value = getValue(internalIssue, fieldName) ?: listOf<String>()
        if (value is List<*>) {
            return value.filterIsInstance<String>()
        }
        if (value is JSONObject) {
            return listOf(value["value"].toString())
        }
        val fieldId = internalIssue.getField(fieldName)?.name ?: "no corresponding fieldId"
        throw IllegalArgumentException("The field $fieldName ($fieldId) was expected to return an array, got $value instead. Did you forget to configure the MultiSelectionFieldMapper?")
    }

    override fun getState(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): String {
        return internalIssue.status.name
    }

    override fun getStateHistory(internalIssue: com.atlassian.jira.rest.client.api.domain.Issue): List<StateHistory> {
        return internalIssue.changelog
            ?.flatMap { logEntry ->
                logEntry.items
                    .filter { it.field == "state" }
                    .map { StateHistory(toLocalDateTime(logEntry.created), it.fromString ?: "", it.toString ?: "") }
            } ?: listOf()
    }

    /**
     * JIRA will readily return a list of available transitions on an issue. A [Transition] contains a name,
     * ID, and a `fields` collection holding optional/required fields for the transition.
     */
    override fun setState(
        internalIssue: com.atlassian.jira.rest.client.api.domain.Issue,
        targetState: String
    ) {
        val transition = jiraRestClient.getHtmlRenderingRestClient().getAvailableTransitions(internalIssue.key)
            .filter { it.value == targetState }
            .keys.firstOrNull() ?: throw IllegalArgumentException("Not transition found to state $targetState")
        try {
            jiraRestClient.issueClient.transition(internalIssue, TransitionInput(transition.id)).claim()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to transition issue ${internalIssue.key} to $targetState", e)
        }
    }

    fun verifySetup(): String {
        return jiraRestClient.metadataClient.serverInfo.claim().serverTitle
    }

    fun listFields() {
        jiraRestClient.metadataClient.fields.claim().forEach { f ->
            println("${f.id} / ${f.name} type ${f.fieldType} ${f.schema?.type}")
        }
    }

    private fun mapJiraIssue(jiraIssue: com.atlassian.jira.rest.client.api.domain.Issue): Issue {
        return Issue(
            jiraIssue.key,
            setup.name,
            getLastUpdated(jiraIssue)
        )
    }

    /**
     * More generic method to set values where a simple setter on the [internalIssueBuilder] is not available.
     *
     * (https://developer.atlassian.com/server/jira/platform/rest-apis/) gives some more insight
     */
    private fun setInternalFieldValue(
        internalIssueBuilder: IssueInputBuilder,
        jiraIssue: com.atlassian.jira.rest.client.api.domain.Issue,
        fieldName: String,
        value: Any
    ) {
        val fld = getIssueFieldByNameOrId(jiraIssue, fieldName)
        // you might be tempted to query [metadataClient] directly here. However, JIRA setup allows to map fields
        // to certain projects only, and so finding a field in the metadataClient does NOT mean it is available
        // in the project the issue is assigned to
        when (JiraMetadata.getFieldType(fld.id, jiraRestClient)) {
            // Text custom field
            "string" -> internalIssueBuilder.setFieldValue(fld.id, value.toString())
            "array" -> {
                if (value is List<*>) {
                    val complexValues = value//
                        .map { ComplexIssueInputFieldValue.with("value", it) }
                    internalIssueBuilder.setFieldValue(fld.id, complexValues)
                } else if (value is String) {
                    val complexValue = listOf(value).map { ComplexIssueInputFieldValue.with("value", it) }
                    internalIssueBuilder.setFieldValue(fld.id, complexValue)
                } else {
                    val fieldId = jiraIssue.getField(fieldName)?.name ?: "no corresponding fieldId"
                    throw IllegalArgumentException("The field $fieldName ($fieldId) was expected to receive an array, but was of type ${value::class.simpleName}")
                }
            }
            "option" -> {
                val complexValue = ComplexIssueInputFieldValue.with("value", value.toString())
                setInternalFieldValue(internalIssueBuilder, fld.id, complexValue)
            }
        }
    }

    private fun getCustomFields(
        internalIssue: com.atlassian.jira.rest.client.api.domain.Issue,
        fieldName: String
    ): Any? {
        val field: IssueField = getIssueFieldByNameOrId(internalIssue, fieldName)
        return field.value?.let { getArrayForJsonArrayValue(it) }?.takeIf { !it.isEmpty() } ?: field.value
    }

    private fun getIssueFieldByNameOrId(
        internalIssue: com.atlassian.jira.rest.client.api.domain.Issue,
        fieldName: String
    ): IssueField {
        return internalIssue.getFieldByName(fieldName) ?: internalIssue.getField(fieldName)
        ?: throw IllegalArgumentException("Unknown field $fieldName")
    }

    private fun getArrayForJsonArrayValue(value: Any): MutableList<String> {
        val result: MutableList<String> = mutableListOf()
        if (value is JSONArray) {
            for (i in 0 until value.length()) {
                val jsonObject = value.get(i) as JSONObject
                val fieldValue = jsonObject.getString("value")
                result.add(fieldValue)
            }
        }
        return result
    }

    private fun setInternalFieldValue(
        internalIssueBuilder: IssueInputBuilder,
        internalFieldId: String,
        internalFieldValue: Any
    ) {
        internalIssueBuilder.setFieldValue(internalFieldId, internalFieldValue)
    }

    private fun toLocalDateTime(jodaDateTime: DateTime): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(jodaDateTime.toInstant().millis), ZoneId.systemDefault())

    override fun getTimeValueInMinutes(
        internalIssue: Any,
        fieldName: String
    ): Number {
        return (getValue(internalIssue as com.atlassian.jira.rest.client.api.domain.Issue, fieldName) ?: 0) as Number
    }

    override fun setTimeValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, timeInMinutes: Number?) {
        val timeInInt = timeInMinutes?.toInt() ?: 0
        val timeNullable = if (timeInInt > 0) timeInInt else null
        setValue(internalIssueBuilder, issue, fieldName, timeNullable)
    }

    override fun logException(
        issue: Issue,
        exception: java.lang.Exception,
        notificationObserver: NotificationObserver,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ) {
        val errorMessage = if (exception is RestClientException) {
            val statusCode = exception.statusCode.or(0)
            val responseMessage = HttpStatus.valueOf(statusCode).reasonPhrase
            "Jira: $responseMessage ($statusCode)"
        } else {
            exception.message
        }
        logger().debug(errorMessage)
        notificationObserver.notifyException(issue, Exception(errorMessage), syncActions)
    }
}