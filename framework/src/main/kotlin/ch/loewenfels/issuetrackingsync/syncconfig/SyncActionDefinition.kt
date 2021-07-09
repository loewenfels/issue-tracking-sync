package ch.loewenfels.issuetrackingsync.syncconfig

/**<!-- tag::overview[] -->
 * An Action is a class which implements SynchronizationAction and is called with a set of field mappings as parameter.
 * Actions and Field mappings can be very complex.
 * <!-- end::overview[] -->
 **/
data class SyncActionDefinition(
    var name: String = "",
    var classname: String = "",
    var fieldMappingDefinitions: MutableList<FieldMappingDefinition> = mutableListOf(),
    var additionalProperties: AdditionalProperties = AdditionalProperties()
)