package ch.loewenfels.issuetrackingsync.syncconfig

import com.fasterxml.jackson.annotation.JsonFormat

/**<!-- tag::overview[] -->
 * This class defines a row of actions, which are applied when an issues passed all filters.
 * Their are no own implementation of SyncFlowDefinition allowed, but the referenced actions and filter.
 * FilterClassname can point to a class of own implementation of IssueFilter.
 * IMPORTANT: Each issue must match only one SyncFlowDefinition.
 * <!-- end::overview[] -->
 **/
data class SyncFlowDefinition(
    var name: String = "",
    var source: TrackingApplicationName = "",
    var target: TrackingApplicationName = "",
    var filterClassname: String? = null,
    var filterProperties: Map<String, String> = emptyMap(),
    var defaultsForNewIssue: DefaultsForNewIssue? = null,
    var keyFieldMappingDefinition: FieldMappingDefinition = FieldMappingDefinition(),
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    var writeBackFieldMappingDefinition: List<FieldMappingDefinition> = emptyList(),
    var preActions: MutableList<PreActionDefinition> = mutableListOf(),
    var actions: MutableList<String> = mutableListOf()
)
