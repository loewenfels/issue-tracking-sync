package ch.loewenfels.issuetrackingsync.syncconfig

open class PreActionDefinition (val className: String) {
    var parameters: Map<String, Any> = emptyMap()
}