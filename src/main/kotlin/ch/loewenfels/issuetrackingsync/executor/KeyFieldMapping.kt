package ch.loewenfels.issuetrackingsync.executor

class KeyFieldMapping(
    sourceName: String,
    targetName: String,
    val writeBackToSourceName: String,
    mapper: FieldMapper
) : FieldMapping(sourceName, targetName, mapper) {
    fun getKeyForTargetIssue(): Any? = sourceValue
    fun getTargetFieldname(): String = targetName
}