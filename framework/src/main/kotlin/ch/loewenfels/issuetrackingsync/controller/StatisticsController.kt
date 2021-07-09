package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.INTERNAL_QUEUE_NAME
import ch.loewenfels.issuetrackingsync.scheduling.BrokerFacade
import ch.loewenfels.issuetrackingsync.scheduling.QueueStatistics
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StatisticsController(private val brokerFacade: BrokerFacade) {
    @GetMapping("/statistics")
    fun statistics(): QueueStatistics? =
        brokerFacade.getStatistics(INTERNAL_QUEUE_NAME)
}
