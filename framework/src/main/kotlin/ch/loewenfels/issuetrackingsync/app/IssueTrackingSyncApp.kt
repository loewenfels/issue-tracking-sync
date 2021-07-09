package ch.loewenfels.issuetrackingsync.app

import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncclient.DefaultClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.Settings
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import java.io.File

@SpringBootApplication(
    scanBasePackages = [
        "ch.loewenfels.issuetrackingsync.controller",
        "ch.loewenfels.issuetrackingsync.scheduling",
        "ch.loewenfels.issuetrackingsync.executor"
    ]
)
@EnableScheduling
@EnableConfigurationProperties(SyncApplicationProperties::class)
@ImportResource("classpath:activemq.xml")
open class IssueTrackingSyncApp : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var syncApplicationProperties: SyncApplicationProperties

    @Bean
    open fun settings(@Autowired objectMapper: ObjectMapper): Settings {
        return Settings.loadFromFile(syncApplicationProperties.settingsLocation)
    }

    @Bean
    open fun appState(@Autowired objectMapper: ObjectMapper): AppState {
        val location = File(syncApplicationProperties.appStateLocation)
        return AppState.loadFromFile(location, objectMapper)
    }

    @Bean
    open fun clientFactory(): ClientFactory {
        return DefaultClientFactory
    }

    @Bean
    open fun notificationObserver(): NotificationObserver {
        val observer = NotificationObserver()
        syncApplicationProperties.notificationChannels.forEach { observer.addChannel(it) }
        return observer
    }

    /**
     * Disable CSRF as this app will be run as an internal app only. Should security ever be a concern,
     * enable CSRF, and adapt index.html accordingly
     */
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .authorizeRequests().antMatchers("/webhook/**").permitAll()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                runApplication<IssueTrackingSyncApp>(*args)
            } catch (e: Exception) {
                System.err.println(e)
                throw e
            }
        }
    }
}