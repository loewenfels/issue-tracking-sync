package ch.loewenfels.issuetrackingsync.syncclient.rtc

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
import com.fasterxml.jackson.databind.JsonNode
import com.ibm.team.foundation.common.text.XMLString
import com.ibm.team.process.client.IProcessClientService
import com.ibm.team.process.common.IIteration
import com.ibm.team.process.common.IIterationHandle
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.repository.client.IItemManager
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.client.TeamPlatform
import com.ibm.team.repository.common.IAuditableHandle
import com.ibm.team.repository.common.IContent
import com.ibm.team.repository.common.IContributor
import com.ibm.team.repository.common.IContributorHandle
import com.ibm.team.repository.common.TeamRepositoryException
import com.ibm.team.workitem.client.IAuditableClient
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.client.WorkItemWorkingCopy
import com.ibm.team.workitem.common.IAuditableCommon
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.expression.AttributeExpression
import com.ibm.team.workitem.common.expression.IQueryableAttribute
import com.ibm.team.workitem.common.expression.QueryableAttributes
import com.ibm.team.workitem.common.expression.Term
import com.ibm.team.workitem.common.model.AttributeOperation
import com.ibm.team.workitem.common.model.AttributeTypes
import com.ibm.team.workitem.common.model.IAttachment
import com.ibm.team.workitem.common.model.IAttachmentHandle
import com.ibm.team.workitem.common.model.IAttribute
import com.ibm.team.workitem.common.model.ICategoryHandle
import com.ibm.team.workitem.common.model.IDeliverable
import com.ibm.team.workitem.common.model.IDeliverableHandle
import com.ibm.team.workitem.common.model.ILiteral
import com.ibm.team.workitem.common.model.IState
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.IWorkItemHandle
import com.ibm.team.workitem.common.model.IWorkItemType
import com.ibm.team.workitem.common.model.Identifier
import com.ibm.team.workitem.common.model.ItemProfile
import com.ibm.team.workitem.common.model.WorkItemEndPoints
import com.ibm.team.workitem.common.model.WorkItemLinkTypes
import com.ibm.team.workitem.common.query.IQueryResult
import com.ibm.team.workitem.common.query.IResolvedResult
import org.eclipse.core.runtime.AssertionFailedException
import org.eclipse.core.runtime.NullProgressMonitor
import org.springframework.beans.BeanWrapperImpl
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

open class RtcClient(private val setup: IssueTrackingApplication) : IssueTrackingClient<IWorkItem>, Logging {
    private val progressMonitor = NullProgressMonitor()
    private val teamRepository: ITeamRepository =
        TeamPlatform.getTeamRepositoryService().getTeamRepository(setup.endpoint)
    private val workItemClient: IWorkItemClient
    private val auditableClient: IAuditableClient
    private val projectArea: IProjectArea
    private val millisToMinutes = 1000 * 60

    init {
        teamRepository.registerLoginHandler(LoginHandler())
        teamRepository.login(NullProgressMonitor())
        workItemClient = teamRepository.getClientLibrary(IWorkItemClient::class.java) as IWorkItemClient
        auditableClient = teamRepository.getClientLibrary(IAuditableClient::class.java) as IAuditableClient
        val processClient = teamRepository.getClientLibrary(IProcessClientService::class.java) as IProcessClientService
        val uri = URI.create(
            setup.project?.replace(" ", "%20") ?: throw IllegalStateException(
                "Need project for RTC client"
            )
        )
        projectArea = processClient.findProcessArea(uri, null, null) as IProjectArea?
            ?: throw IllegalStateException("Project area ${setup.project} is invalid")
    }

    override fun getIssue(key: String): Issue? {
        return getRtcIssue(key)?.let {
            toSyncIssue(it)
        }
    }

    override fun getIssueFromWebhookBody(body: JsonNode): Issue =
        throw UnsupportedOperationException("RTC does not support webhooks")

    override fun getProprietaryIssue(issueKey: String): IWorkItem? {
        return getRtcIssue(issueKey)
    }

    override fun getProprietaryIssue(fieldName: String, fieldValue: String): IWorkItem? {
        val queryClient = workItemClient.queryClient
        val attrExpression =
            AttributeExpression(
                getQueryableAttribute(fieldName),
                AttributeOperation.EQUALS,
                fieldValue
            )
        val resolvedResultOfWorkItems =
            queryClient.getResolvedExpressionResults(projectArea, attrExpression, IWorkItem.FULL_PROFILE)
        val workItems = toWorkItems(resolvedResultOfWorkItems)
        return when (workItems.size) {
            0 -> null
            // reload to get full issue incl. collections such as comments
            1 -> getProprietaryIssue(workItems[0].id.toString())
            else -> throw IssueClientException("Query too broad, multiple issues found for $fieldValue")
        }
    }

    private fun getRtcIssue(key: String): IWorkItem? {
        return workItemClient.findWorkItemById(Integer.parseInt(key), IWorkItem.SMALL_PROFILE, progressMonitor)
    }

    override fun getLastUpdated(internalIssue: IWorkItem): LocalDateTime =
        toLocalDateTime(internalIssue.modified())

    override fun getKey(internalIssue: IWorkItem): String =
        internalIssue.id.toString()

    override fun getIssueUrl(internalIssue: IWorkItem): String {
        val endpoint =
            if (setup.endpoint.endsWith("/")) setup.endpoint.substring(0, setup.endpoint.length - 1) else setup.endpoint
        val encodedProjectName = URLEncoder.encode(setup.project, "UTF-8").replace("+", "%20")
        return "$endpoint/web/projects/$encodedProjectName#action=com.ibm.team.workitem.viewWorkItem&id=${internalIssue.id}"
    }

    override fun getHtmlValue(internalIssue: IWorkItem, fieldName: String): String? {
        return when (val value = getValue(internalIssue, fieldName)) {
            is XMLString -> value.xmlText
            else -> value?.toString()
        }
    }

    override fun getValue(internalIssue: IWorkItem, fieldName: String): Any? {
        val beanWrapper = BeanWrapperImpl(internalIssue)
        val internalValue = if (beanWrapper.isReadableProperty(fieldName))
            beanWrapper.getPropertyValue(fieldName)
        else
            getPropertyValueForCustomFields(internalIssue, fieldName)
        return internalValue?.let { convertFromMetadataId(fieldName, it) }
    }

    private fun getPropertyValueForCustomFields(internalIssue: IWorkItem, fieldName: String): Any? {
        val attribute: IAttribute
        try {
            attribute = getAttribute(fieldName)
        } catch (ex: Exception) {
            return null
        }
        return if (internalIssue.hasAttribute(attribute))
            internalIssue.getValue(attribute)
        else
            null
    }

    override fun setValue(
        internalIssueBuilder: Any,
        issue: Issue,
        fieldName: String,
        value: Any?
    ) {
        val workItem = internalIssueBuilder as IWorkItem
        val attribute = getAttribute(fieldName)
        convertToMetadataId(fieldName, value)?.let {
            logger().debug("Setting value $value on $fieldName")
            when {
                it is IIterationHandle && fieldName == "target" -> workItem.target = it
                it is ICategoryHandle -> workItem.category = it
                else -> workItem.setValue(attribute, it)
            }
        }
    }

    override fun getTimeValueInMinutes(internalIssue: Any, fieldName: String): Number {
        val time = (getValue(internalIssue as IWorkItem, fieldName) ?: 0) as Long
        return time / millisToMinutes
    }

    override fun setHtmlValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, htmlString: String) =
        setValue(internalIssueBuilder, issue, fieldName, htmlString)

    /**
     * Given a field and value, attempt to map the value to an RTC internal (metadata) ID. A value of
     * "Needs analysis" might thus become "com.foobar.rtc.process_state.1"
     */
    private fun convertToMetadataId(fieldName: String, value: Any?): Any? {
        //
        val attribute = getAttribute(fieldName)
        return when {
            attribute.attributeType == "priority" -> RtcMetadata.getPriorityId(
                value?.toString() ?: "",
                getAttribute(IWorkItem.PRIORITY_PROPERTY),
                workItemClient
            )
            fieldName == "severity" || fieldName == "internalSeverity" -> RtcMetadata.getSeverityId(
                value?.toString() ?: "",
                getAttribute(IWorkItem.SEVERITY_PROPERTY),
                workItemClient
            )
            attribute.attributeType == "category" -> getCategoryIdentifier(value as String?)
            attribute.attributeType == "interval"
                    || attribute.attributeType == "deliverable" -> getIntervalIdentifier(value as String?)
            // need to map 'internalTags' directly here, as this is in fact a list type, but has no enumeration
            fieldName == "internalTags" -> value
            // handle multi-select values
            AttributeTypes.isEnumerationListAttributeType(attribute.attributeType) && value is List<*> ->
                getEnumerationValues(fieldName, value)
            // handle single-select values
            AttributeTypes.isListAttributeType(attribute.attributeType) ||
                    AttributeTypes.isEnumerationAttributeType(attribute.attributeType)
            -> getEnumerationIdentifier(fieldName, value)
            else -> value
        }
    }

    private fun getEnumerationIdentifier(fieldName: String, value: Any?): Identifier<out ILiteral>? {
        try {
            return workItemClient.resolveEnumeration(
                getAttribute(fieldName),
                null
            ).enumerationLiterals//
                .find { it.name == value?.toString() ?: "" }?.identifier2
        } catch (ex: AssertionFailedException) {
            throw IllegalArgumentException("Attempted to enumerate field $fieldName, which isn't an enumeration", ex)
        }
    }

    private fun getEnumerationName(fieldName: String, identifier: Identifier<*>): String {
        @Suppress("UNCHECKED_CAST")
        return workItemClient.resolveEnumeration(getAttribute(fieldName), null)
            .findEnumerationLiteral(identifier as Identifier<out ILiteral>?)
            .name
    }

    private fun getEnumerationValues(fieldName: String, value: List<*>): List<Identifier<out ILiteral>> {
        val enumerations = workItemClient.resolveEnumeration(getAttribute(fieldName), null)
        return enumerations.enumerationLiterals//
            .filter { value.contains(it.name) }//
            .map { it.identifier2 }
    }

    private fun getCategoryName(value: ICategoryHandle): String {
        val common = teamRepository.getClientLibrary(IWorkItemCommon::class.java) as IWorkItemCommon
        return common.resolveHierarchicalName(value, progressMonitor)
    }

    private fun getCategoryIdentifier(categoryName: String?): ICategoryHandle? {
        return categoryName?.let {
            val common = teamRepository.getClientLibrary(IWorkItemCommon::class.java) as IWorkItemCommon
            return common.findCategoryByNamePath(projectArea, it.split("/"), null)
        }
    }

    private fun getIteration(handle: IIterationHandle): IIteration =
        auditableClient.resolveAuditable(handle, ItemProfile.ITERATION_DEFAULT, null) as IIteration

    private fun getDeliverable(handle: IDeliverableHandle): IDeliverable =
        teamRepository.itemManager().fetchCompleteItem(handle, IItemManager.DEFAULT, null) as IDeliverable

    private fun getContributorName(value: IContributorHandle): String {
        return auditableClient.resolveAuditable(value, ItemProfile.CONTRIBUTOR_DEFAULT, null).name
    }

    /**
     * The [intervalName] might be something like "I2003.3 - 3.77", while RTC defines:
     *
     * - Iteration named "I2003"
     * - Child named '3 - 3.77'
     */
    private fun getIntervalIdentifier(intervalName: String?): IIterationHandle? {
        if (intervalName == null) {
            return null
        }
        return auditableClient.resolveAuditable(
            projectArea.projectDevelopmentLine,
            ItemProfile.DEVELOPMENT_LINE_DEFAULT,
            null
        )
            .iterations
            .map { getIteration(it) }
            .firstOrNull { intervalName.startsWith(it.name) }
            ?.let { getIntervalIdentifier(it, intervalName) }
    }

    private fun getIntervalIdentifier(parentIteration: IIteration, intervalName: String): IIterationHandle? {
        if (parentIteration.name == intervalName) {
            return parentIteration
        }
        return parentIteration.children
            .map { getIteration(it) }
            .firstOrNull { intervalName.startsWith(it.name) }
            ?.let { getIntervalIdentifier(it, intervalName) }
    }

    private fun convertFromMetadataId(fieldName: String, value: Any): Any {
        return when {
            fieldName == "priority" || fieldName == "internalPriority" -> RtcMetadata.getPriorityName(
                value.toString(),
                getAttribute(IWorkItem.PRIORITY_PROPERTY),
                workItemClient
            )
            fieldName == "severity" || fieldName == "internalSeverity" -> RtcMetadata.getSeverityName(
                value.toString(),
                getAttribute(IWorkItem.SEVERITY_PROPERTY),
                workItemClient
            )
            fieldName == "internalTags" -> value.toString().split("|").toTypedArray().filter { label -> label.isNotBlank() }
            value is ICategoryHandle -> getCategoryName(value)
            value is Identifier<*> -> getEnumerationName(fieldName, value)
            value is IIterationHandle -> getIteration(value).name
            value is IDeliverableHandle -> getDeliverable(value).name
            value is IContributorHandle -> getContributorName(value)
            else -> value
        }
    }

    override fun createOrUpdateTargetIssue(
        issue: Issue,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        val targetKeyFieldname = issue.keyFieldMapping!!.getTargetFieldname()
        val targetIssueKey = issue.keyFieldMapping!!.getKeyForTargetIssue().toString()
        val targetIssue =
            (issue.proprietaryTargetInstance ?: if (targetIssueKey.isNotEmpty()) getProprietaryIssue(
                targetKeyFieldname,
                targetIssueKey
            ) else null) as IWorkItem?
        when {
            targetIssue != null -> updateTargetIssue(targetIssue, issue)
            defaultsForNewIssue != null -> createTargetIssue(defaultsForNewIssue, issue)
            else -> throw SynchronizationAbortedException("No target issue found for $targetIssueKey, and no defaults for creating issue were provided")
        }
    }

    private fun createTargetIssue(defaultsForNewIssue: DefaultsForNewIssue, issue: Issue): IWorkItem {
        val workItemType: IWorkItemType =
            workItemClient.findWorkItemType(projectArea, defaultsForNewIssue.issueType, progressMonitor)
        val path = defaultsForNewIssue.category.split("/")
        val category: ICategoryHandle = workItemClient.findCategoryByNamePath(projectArea, path, progressMonitor)
        val operation = getInitialisedIssue(category, defaultsForNewIssue)
        val handle: IWorkItemHandle = operation.run(workItemType, progressMonitor)
        operation.workItem?.let { mapNewIssueValues(it, issue) }
        val workItem: IWorkItem = auditableClient.resolveAuditable(handle, IWorkItem.FULL_PROFILE, progressMonitor)
        logger().info("Created new RTC issue ${workItem.id}")
        issue.workLog.add("Created new RTC issue ${workItem.id}")
        setTargetPropertiesOnSyncIssue(workItem, issue)
        return workItem
    }

    private fun getInitialisedIssue(
        category: ICategoryHandle,
        defaultsForNewIssue: DefaultsForNewIssue
    ): WorkItemInitialization {
        defaultsForNewIssue.additionalFields.enumerationFields.forEach {
            defaultsForNewIssue.additionalFields.multiselectFields.set(
                it.key,
                it.value
            )
        }
        return WorkItemInitialization(
            "creating new issue",
            category,
            defaultsForNewIssue.additionalFields.multiselectFields.mapValues {
                convertToMetadataId(
                    it.key,
                    it.value
                )
            }.mapKeys { getAttribute(it.key) }
        )
    }

    private fun updateTargetIssue(targetIssue: IWorkItem, issue: Issue) {
        setTargetPropertiesOnSyncIssue(targetIssue, issue)
        doWithWorkingCopy(targetIssue) {
            val changeableWorkingItem = it.workItem
            mapNewIssueValues(changeableWorkingItem, issue)
            logger().info("Updating RTC issue ${targetIssue.id}")
        }
    }

    private fun setTargetPropertiesOnSyncIssue(targetIssue: IWorkItem, issue: Issue) {
        issue.proprietaryTargetInstance = targetIssue
        issue.targetKey = getKey(targetIssue)
        issue.targetUrl = getIssueUrl(targetIssue)
    }

    private fun mapNewIssueValues(targetIssue: IWorkItem, issue: Issue) {
        issue.fieldMappings.forEach {
            it.setTargetValue(targetIssue, issue, this)
        }
    }

    override fun changedIssuesSince(
        lastPollingTimestamp: LocalDateTime,
        batchSize: Int,
        offset: Int
    ): Collection<Issue> {
        val queryClient = workItemClient.queryClient
        val searchTerms = buildSearchTermForChangedIssues(lastPollingTimestamp)
        val resolvedResultOfWorkItems =
            queryClient.getResolvedExpressionResults(projectArea, searchTerms, IWorkItem.FULL_PROFILE)
        return toWorkItems(resolvedResultOfWorkItems).map { toSyncIssue(it) }
    }

    private fun buildSearchTermForChangedIssues(lastPollingTimestamp: LocalDateTime): Term {
        val modifiedRecently =
            AttributeExpression(
                getQueryableAttribute(IWorkItem.MODIFIED_PROPERTY),
                AttributeOperation.GREATER_OR_EQUALS,
                Timestamp.valueOf(lastPollingTimestamp)
            )
        val createdRecently =
            AttributeExpression(
                getQueryableAttribute(IWorkItem.CREATION_DATE_PROPERTY),
                AttributeOperation.GREATER_OR_EQUALS,
                Timestamp.valueOf(lastPollingTimestamp)
            )
        val projectAreaExpression = AttributeExpression(
            getQueryableAttribute(IWorkItem.PROJECT_AREA_PROPERTY),
            AttributeOperation.EQUALS,
            projectArea
        )
        val relevantIssuesTerm = Term(Term.Operator.OR)
        relevantIssuesTerm.add(modifiedRecently)
        relevantIssuesTerm.add(createdRecently)
        //
        val searchTerm = Term(Term.Operator.AND)
        searchTerm.add(relevantIssuesTerm)
        searchTerm.add(projectAreaExpression)
        return searchTerm
    }

    private fun getQueryableAttribute(attributeName: String): IQueryableAttribute {
        val auditableCommon: IAuditableCommon =
            teamRepository.getClientLibrary(IAuditableCommon::class.java) as IAuditableCommon
        return QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(
            projectArea,
            attributeName,
            auditableCommon,
            progressMonitor
        ) ?: throw IllegalArgumentException("Attribute $attributeName is either unknown or not query-able")
    }

    private fun getAttribute(attributeName: String): IAttribute =
        getAttributeNullable(attributeName) ?: throw IllegalArgumentException("Unknown attribute $attributeName")

    private fun getAttributeNullable(attributeName: String): IAttribute? =
        workItemClient.findAttribute(
            projectArea,
            attributeName,
            progressMonitor
        )

    private fun toWorkItems(resolvedResults: IQueryResult<IResolvedResult<IWorkItem>>): List<IWorkItem> {
        val result = LinkedList<IWorkItem>()
        while (resolvedResults.hasNext(progressMonitor)) {
            result.add(resolvedResults.next(progressMonitor).item)
        }
        return result
    }

    private fun toSyncIssue(workItem: IWorkItem): Issue {
        return Issue(
            workItem.id.toString(),
            setup.name,
            getLastUpdated(workItem)
        )
    }

    override fun getComments(internalIssue: IWorkItem): List<Comment> {
        return internalIssue.comments.contents.map { rtcComment ->
            Comment(
                (auditableClient.resolveAuditable(
                    rtcComment.creator,
                    ItemProfile.CONTRIBUTOR_DEFAULT,
                    null
                ) as IContributor).name,
                toLocalDateTime(rtcComment.creationDate),
                rtcComment.htmlContent.xmlText,
                rtcComment.creationDate.time.toString()
            )
        }
    }

    override fun addComment(internalIssue: IWorkItem, comment: Comment) {
        doWithWorkingCopy(internalIssue) {
            val changeableWorkingItem = it.workItem
            val comments = changeableWorkingItem.comments
            val newComment = comments.createComment(
                teamRepository.loggedInContributor(),
                XMLString.createFromXMLText(comment.content)
            )
            comments.append(newComment)
        }
    }

    override fun getAttachments(internalIssue: IWorkItem): List<Attachment> {
        val common = teamRepository.getClientLibrary(IWorkItemCommon::class.java) as IWorkItemCommon
        return common.resolveWorkItemReferences(internalIssue, progressMonitor)
            .getReferences(WorkItemEndPoints.ATTACHMENT)
            .map {
                val attachHandle = it.resolve() as IAttachmentHandle
                val attachment = auditableClient.resolveAuditable(
                    attachHandle,
                    IAttachment.DEFAULT_PROFILE, null
                ) as IAttachment
                val baos = ByteArrayOutputStream()
                teamRepository.contentManager().retrieveContent(attachment.content, baos, null)
                Attachment(attachment.name, baos.toByteArray())
            }
    }

    override fun addAttachment(internalIssue: IWorkItem, attachment: Attachment) {
        doWithWorkingCopy(internalIssue) {
            val contentType = IContent.CONTENT_TYPE_UNKNOWN // or IContent.CONTENT_TYPE_TEXT?
            val encoding = IContent.ENCODING_UTF_8
            var newAttachment = workItemClient.createAttachment(
                projectArea, attachment.filename, "", contentType,
                encoding, ByteArrayInputStream(attachment.content), progressMonitor
            )
            newAttachment = newAttachment.workingCopy as IAttachment
            newAttachment = workItemClient.saveAttachment(newAttachment, progressMonitor)
            val reference = WorkItemLinkTypes.createAttachmentReference(newAttachment)
            it.references.add(WorkItemEndPoints.ATTACHMENT, reference)
        }
    }

    override fun getMultiSelectValues(internalIssue: IWorkItem, fieldName: String): List<String> {
        val attribute = getAttribute(fieldName)
        if (!AttributeTypes.isListAttributeType(attribute.attributeType) && !AttributeTypes.isEnumerationAttributeType(
                attribute.attributeType
            )
        ) {
            throw IllegalArgumentException("Attribute $fieldName has no list")
        }
        val enumeration = workItemClient.resolveEnumeration(attribute, null)
        val values = getValue(internalIssue, fieldName) ?: listOf<Identifier<ILiteral>>()
        if (values is List<*>) {
            val fieldValues = values.filterIsInstance<Identifier<ILiteral>>()
            val stringIdentifiers = fieldValues.map { it.stringIdentifier }
            return enumeration.enumerationLiterals//
                .filter { stringIdentifiers.contains(it.identifier2.stringIdentifier) }//
                .map { it.name }
        }
        if (values is Identifier<*>) {
            val stringIdentifier = values.stringIdentifier
            return enumeration.enumerationLiterals//
                .filter { it.identifier2.stringIdentifier == stringIdentifier }//
                .map { it.name }
        }
        throw IllegalArgumentException("The field $fieldName was expected to return an array, got $values instead. Did you forget to configure the MultiSelectionFieldMapper?")
    }

    override fun getState(internalIssue: IWorkItem): String {
        val workflowInfo = workItemClient.findWorkflowInfo(internalIssue, null)
        return workflowInfo.getStateName(internalIssue.state2)
    }

    override fun getStateHistory(internalIssue: IWorkItem): List<StateHistory> {
        var previousState: Identifier<IState>? = null
        val result = mutableListOf<StateHistory>()
        val itemManager = teamRepository.itemManager()
        val workflowInfo = workItemClient.findWorkflowInfo(internalIssue, null)
        // note that RTC returns a history collection which is NOT sorted by change date
        itemManager.fetchAllStateHandles(internalIssue.stateHandle as IAuditableHandle, null)
            .map { itemManager.fetchCompleteState(it as IAuditableHandle, null) as IWorkItem }
            .sortedBy { it.modified() }
            .forEach {
                if (previousState != null && it.state2 != previousState) {
                    val updateTimestamp = LocalDateTime.ofInstant(it.modified().toInstant(), ZoneId.systemDefault())
                    result.add(
                        StateHistory(
                            updateTimestamp,
                            workflowInfo.getStateName(previousState),
                            workflowInfo.getStateName(it.state2)
                        )
                    )
                }
                previousState = it.state2
            }
        return result
    }

    override fun setState(internalIssue: IWorkItem, targetState: String) {
        val workflowInfo = workItemClient.findWorkflowInfo(internalIssue, null)
        val workflowActionTowardsTargetState = workflowInfo.getActionIds(internalIssue.state2)
            .first { workflowInfo.getStateName(workflowInfo.getActionResultState(it)) == targetState }
            ?: throw IllegalArgumentException("No action found leading to state $targetState")
        doWithWorkingCopy(internalIssue) {
            it.workflowAction = workflowActionTowardsTargetState.stringIdentifier
        }
    }

    override fun setTimeValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, timeInMinutes: Number?) {
        setValue(internalIssueBuilder, issue, fieldName, (timeInMinutes?.toLong() ?: 0) * millisToMinutes)
    }

    override fun logException(
        issue: Issue,
        exception: java.lang.Exception,
        notificationObserver: NotificationObserver,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ) {
        val errorMessage = if (exception is TeamRepositoryException) {
            "RTC: ${exception.message}"
        } else {
            exception.message
        }
        logger().debug(errorMessage)
        notificationObserver.notifyException(issue, Exception(errorMessage), syncActions)
    }

    private fun doWithWorkingCopy(originalWorkItem: IWorkItem, consumer: (WorkItemWorkingCopy) -> Unit) {
        val copyManager = workItemClient.workItemWorkingCopyManager
        copyManager.connect(originalWorkItem, IWorkItem.FULL_PROFILE, progressMonitor)
        try {
            val workingCopy = copyManager.getWorkingCopy(originalWorkItem)
            consumer.invoke(workingCopy)
            val detailedStatus = workingCopy.save(null)
            if (!detailedStatus.isOK) {
                throw  RuntimeException("Error saving work item", detailedStatus.exception)
            }
        } finally {
            copyManager.disconnect(originalWorkItem)
        }
    }

    fun listMetadata(): List<IAttribute> =
        workItemClient.findAttributes(projectArea, progressMonitor).toList()

    private fun toLocalDateTime(sqlDate: Date): LocalDateTime =
        sqlDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun toLocalDateTime(sqlTimestamp: Timestamp): LocalDateTime =
        sqlTimestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

    inner class LoginHandler : ITeamRepository.ILoginHandler, ITeamRepository.ILoginHandler.ILoginInfo {
        override fun getUserId(): String {
            return setup.username
        }

        override fun getPassword(): String {
            return setup.password
        }

        override fun challenge(repository: ITeamRepository?): ITeamRepository.ILoginHandler.ILoginInfo {
            return this
        }
    }
}
