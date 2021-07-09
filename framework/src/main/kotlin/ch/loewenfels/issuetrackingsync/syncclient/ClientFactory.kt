package ch.loewenfels.issuetrackingsync.syncclient

import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication

interface ClientFactory {
    fun getClient(clientSettings: IssueTrackingApplication): IssueTrackingClient<Any>
}

object DefaultClientFactory : ClientFactory {
    override fun getClient(clientSettings: IssueTrackingApplication): IssueTrackingClient<Any> {
        return Class.forName(clientSettings.className)
            .constructors.filter { it.parameterCount == 1 }
            .filter { IssueTrackingApplication::class.java.isAssignableFrom(it.parameters[0].type) }
            .map { it.newInstance(clientSettings) }
            .filterIsInstance<IssueTrackingClient<Any>>()
            .firstOrNull()
            ?: throw IllegalArgumentException("Failed to load client class ${clientSettings.className}")
    }
}
