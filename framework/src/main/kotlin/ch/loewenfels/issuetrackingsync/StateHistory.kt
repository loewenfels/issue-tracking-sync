package ch.loewenfels.issuetrackingsync

import java.time.LocalDateTime

data class StateHistory(
    val timestamp: LocalDateTime,
    val fromState: String,
    val toState: String
)