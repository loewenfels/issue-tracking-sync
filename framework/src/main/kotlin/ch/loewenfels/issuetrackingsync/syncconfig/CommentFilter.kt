package ch.loewenfels.issuetrackingsync.syncconfig

data class CommentFilter(
    var filterClassname: String = "",
    var filterProperties: Map<String, String> = emptyMap()
)
