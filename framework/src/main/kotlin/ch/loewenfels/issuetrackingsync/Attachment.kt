package ch.loewenfels.issuetrackingsync

data class Attachment(
    val filename: String,
    val content: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Attachment
        return content.contentHashCode() == other.content.contentHashCode()
    }

    override fun hashCode(): Int =
        content.contentHashCode()
}