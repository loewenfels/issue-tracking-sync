package ch.loewenfels.issuetrackingsync.syncclient.jira

import com.atlassian.event.api.EventPublisher
import com.atlassian.httpclient.apache.httpcomponents.DefaultHttpClientFactory
import com.atlassian.httpclient.api.factory.HttpClientOptions
import com.atlassian.jira.rest.client.api.AuthenticationHandler
import com.atlassian.jira.rest.client.internal.async.AsynchronousHttpClientFactory
import com.atlassian.jira.rest.client.internal.async.AtlassianHttpClientDecorator
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.UrlMode
import com.atlassian.sal.api.executor.ThreadLocalContextManager
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

class ExtendedAsynchronousHttpClientFactory : AsynchronousHttpClientFactory() {

    fun createClient(
        serverUri: URI,
        authenticationHandler: AuthenticationHandler,
        socketTimeout: Int
    ): DisposableHttpClient {
        val options = HttpClientOptions()
        options.setSocketTimeout(socketTimeout, TimeUnit.MILLISECONDS)

        return createHttpClient(serverUri, authenticationHandler, options)
    }

    private fun createHttpClient(
        serverUri: URI,
        authenticationHandler: AuthenticationHandler,
        options: HttpClientOptions
    ): AtlassianHttpClientDecorator {
        val defaultHttpClientFactory: DefaultHttpClientFactory<*> = DefaultHttpClientFactory(NoOpEventPublisher(),
            RestClientApplicationProperties(serverUri),
            object : ThreadLocalContextManager<Any?> {
                override fun getThreadLocalContext(): Any? = null
                override fun setThreadLocalContext(context: Any?) {}
                override fun clearThreadLocalContext() {}
            })
        val httpClient = defaultHttpClientFactory.create(options)
        return object : AtlassianHttpClientDecorator(httpClient, authenticationHandler) {
            override fun destroy() = defaultHttpClientFactory.dispose(httpClient)
        }
    }

    private class NoOpEventPublisher : EventPublisher {
        override fun publish(o: Any) {}
        override fun register(o: Any) {}
        override fun unregister(o: Any) {}
        override fun unregisterAll() {}
    }

    /**
     * These properties are used to present JRJC as a User-Agent during http requests.
     */
    private class RestClientApplicationProperties(jiraURI: URI) : ApplicationProperties {
        private val baseUrl: String = jiraURI.path
        override fun getBaseUrl(): String = baseUrl

        /** We'll always have an absolute URL as a client.*/
        override fun getBaseUrl(urlMode: UrlMode): String = baseUrl
        override fun getDisplayName(): String = "Atlassian JIRA Rest Java Client"
        override fun getPlatformId(): String = ApplicationProperties.PLATFORM_JIRA
        override fun getVersion(): String =
            MavenUtils.getVersion("com.atlassian.jira", "jira-rest-java-com.atlassian.jira.rest.client")

        override fun getBuildDate(): Date = throw UnsupportedOperationException()

        // TODO implement using MavenUtils, JRJC-123
        override fun getBuildNumber(): String = 0.toString()
        override fun getHomeDirectory(): File = File(".")
        override fun getPropertyValue(s: String): String = throw UnsupportedOperationException("Not implemented")
    }

    private object MavenUtils {
        private val logger = LoggerFactory.getLogger(MavenUtils::class.java)
        private const val UNKNOWN_VERSION = "unknown"
        fun getVersion(groupId: String?, artifactId: String?): String {
            val props = Properties()
            return try {
                MavenUtils::class.java
                    .getResourceAsStream("/META-INF/maven/$groupId/$artifactId/pom.properties")
                    .use { resourceAsStream ->
                        props.load(resourceAsStream)
                        props.getProperty("version", UNKNOWN_VERSION)
                    }
            } catch (e: Exception) {
                logger.debug("Could not find version for maven artifact {}:{}", groupId, artifactId)
                logger.debug("Got the following exception", e)
                UNKNOWN_VERSION
            }
        }
    }
}