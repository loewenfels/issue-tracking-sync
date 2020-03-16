package ch.loewenfels.issuetrackingsync.syncconfig

import ch.loewenfels.issuetrackingsync.executor.fields.DirectFieldMapper

open class FieldMappingDefinition(
    var sourceName: String = "",
    var targetName: String = "",
    var mapperClassname: String = DirectFieldMapper::class.qualifiedName ?: "",
    var associations: MutableMap<String, String> = mutableMapOf(),
    var additionalAssociations: MutableMap<String, String> = mutableMapOf(),
    var fieldSkipEvalutors: MutableList<FieldSkippingEvaluatorDefinition> = mutableListOf()
)
