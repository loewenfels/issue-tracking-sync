package ch.loewenfels.issuetrackingsync.scheduling

import java.io.Serializable
import java.time.LocalDateTime

data class SyncRequest(
    val key: String,
    val clientSourceName: String,
    val lastUpdated: LocalDateTime
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = -1
    }
}
