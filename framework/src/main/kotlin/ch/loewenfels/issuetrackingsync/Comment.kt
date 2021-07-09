package ch.loewenfels.issuetrackingsync

import java.time.LocalDateTime

/**
 * Holds a comment with [timestamp] in local date/time, and [content] formatted as HTML (or, alternatively, as
 * plain text. The content will, however, *never* be in JIRA markup)
 */
data class Comment(
    val author: String,
    val timestamp: LocalDateTime,
    val content: String,
    val internalId: String
)