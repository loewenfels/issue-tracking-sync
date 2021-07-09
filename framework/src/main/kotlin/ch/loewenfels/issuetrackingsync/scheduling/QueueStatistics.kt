package ch.loewenfels.issuetrackingsync.scheduling

data class QueueStatistics(
    val queuename: String,
    /**
     * The number of messages that currently reside in the queue.
     */
    val currentSize: Long,
    /**
     * The number of messages that have been successfully (i.e., they’ve been acknowledged from the consumer)
     * read off the queue over the lifetime of the queue.
     */
    val totalMessagesProcessed: Long,
    /**
     * The minimum amount of time that messages remained enqueued (in ms).
     */
    val minEnqueueTime: Double,
    /**
     * The maximum amount of time that messages remained enqueued (in ms).
     */
    val maxEnqueueTime: Double,
    /**
     * On average, the amount of time (ms) that messages remained enqueued.
     * Or average time it is taking the consumers to successfully process messages.
     */
    val averageEnqueueTime: Double,
    val memoryUsage: Long,
    val memoryUsageInPercent: Long
) {
    val memoryPercentUsage: Double = memoryUsageInPercent / 100.0

}