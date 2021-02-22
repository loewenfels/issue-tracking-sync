package ch.loewenfels.issuetrackingsync.syncconfig

data class LogDefinition(
    val every: Map<String, String>?,
    val onChange: Map<String, String>?,
    val onCreateEqual: Map<String, Map<String, String>>,
    val onChangeEqual: Map<String, Map<String, String>>
)