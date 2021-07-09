package ch.loewenfels.issuetrackingsync.syncconfig

import ch.loewenfels.issuetrackingsync.executor.fields.DirectFieldMapper

/**<!-- tag::overview[] -->
 * Each issue field to be synchronized must be configured by a field mapping.
 * Fields can also be joined to one or split into several target fields.
 * <!-- end::overview[] -->
 **/
open class FieldMappingDefinition(
    var sourceName: String = "",
    var targetName: String = "",
    var mapperClassname: String = DirectFieldMapper::class.qualifiedName ?: "",
    var fieldSkipEvaluators: MutableList<FieldSkippingEvaluatorDefinition> = mutableListOf(),
    var callback: FieldMappingDefinition? = null,
    associations: MutableMap<String, String> = mutableMapOf()
) : AssociationsFieldDefinition(associations)
