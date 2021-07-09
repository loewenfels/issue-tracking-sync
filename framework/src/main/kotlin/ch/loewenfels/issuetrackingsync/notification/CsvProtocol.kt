package ch.loewenfels.issuetrackingsync.notification

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.app.NotificationChannelProperties
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CsvProtocol(properties: NotificationChannelProperties) : NotificationChannel {
    private val csvHeader = "Datum;Source Issue;Target Issue;Sync Actions;Status"
    private val csvLocation = properties.endpoint
    val file = File(csvLocation)

    override fun onSuccessfulSync(
        issue: Issue,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ) {
        writeToCsv(issue, syncActions, true)
    }

    override fun onException(
        issue: Issue,
        ex: Exception,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ) {
        writeToCsv(issue, syncActions, false)
    }

    private fun writeToCsv(
        issue: Issue,
        syncActions: Map<SyncActionName, SynchronizationAction>,
        success: Boolean
    ) {
        val fileHeaders = !file.exists()
        val source = issue.key
        val target = issue.targetKey ?: ""
        val status = if (success) "Erfolgreich" else "Fehler"
        val entry = "${currentTime()};${source};${target};${concatActions(syncActions)};$status"
        FileOutputStream(file, true).bufferedWriter(Charsets.ISO_8859_1).use { out ->
            if (fileHeaders) {
                out.appendLine(csvHeader)
            }
            out.appendLine(entry)
        }
    }

    private fun currentTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    private fun concatActions(syncActions: Map<SyncActionName, SynchronizationAction>) =
        syncActions.keys.toList().toString().replace("[", "").replace("]", "")
}