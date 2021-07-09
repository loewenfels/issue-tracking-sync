package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.Comment

interface CommentFilter {
    fun getFilter(): (Comment) -> Boolean
}