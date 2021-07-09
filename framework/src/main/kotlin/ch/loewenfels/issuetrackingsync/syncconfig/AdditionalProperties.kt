package ch.loewenfels.issuetrackingsync.syncconfig

data class AdditionalProperties(
    var preComment: String = "",
    var postComment: String = "",
    var commentFilter: List<CommentFilter> = mutableListOf(),
    var allTransitions: Map<String, List<String>> = mutableMapOf(),
    var statesMapping: Map<String, String> = mutableMapOf(),
    var happyPath: List<String> = mutableListOf()
)
