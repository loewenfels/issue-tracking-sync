package ch.loewenfels.issuetrackingsync.scheduling

import org.springframework.stereotype.Component
import javax.jms.*

typealias QueueName = String

@Component
class BrokerFacade(private val connectionFactory: ConnectionFactory) {
    private val statisticsBrokers = mutableMapOf<QueueName, StatisticsBrokerAccess>()

    companion object {
        const val timeoutIntervall: Long = 2000
    }

    @Throws(JMSException::class)
    fun getStatistics(queueName: QueueName): QueueStatistics? {
        val brokerAccess = statisticsBrokers.getOrPut(queueName) { StatisticsBrokerAccess(queueName) }
        return brokerAccess.getCurrentStatistics()?.let {
            QueueStatistics(
                queueName,
                it.getLong("size"),
                it.getLong("dequeueCount"),
                it.getDouble("minEnqueueTime"),
                it.getDouble("maxEnqueueTime"),
                it.getDouble("averageEnqueueTime"),
                it.getLong("memoryUsage"),
                it.getLong("memoryPercentUsage")
            )
        }
    }

    inner class StatisticsBrokerAccess(queueName: QueueName) {
        private val statisticsMessageConsumer: MessageConsumer
        private val statisticsMessageProducer: MessageProducer
        private val statisticsMessage: Message

        init {
            val connection = connectionFactory.createConnection()
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val statisticsReplyQueue = session.createTemporaryQueue()
            statisticsMessageConsumer = session.createConsumer(statisticsReplyQueue)
            val statisticsQueue = session.createQueue("ActiveMQ.Statistics.Destination.$queueName")
            statisticsMessageProducer = session.createProducer(statisticsQueue)
            statisticsMessage = session.createMessage()
            statisticsMessage.jmsReplyTo = statisticsReplyQueue
        }

        fun getCurrentStatistics(): MapMessage? {
            statisticsMessageProducer.send(statisticsMessage)
            return statisticsMessageConsumer.receive(timeoutIntervall) as MapMessage?
        }
    }
}