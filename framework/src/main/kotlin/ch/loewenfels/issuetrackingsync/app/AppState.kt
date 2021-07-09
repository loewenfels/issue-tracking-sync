package ch.loewenfels.issuetrackingsync.app

import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.logger
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.time.LocalDateTime

const val FILENAME = "appstate.json"

class AppState {
    private var backingFile: File? = null
    var lastPollingTimestamp: LocalDateTime? = null

    companion object : Logging {
        fun loadFromFile(parentDir: File, objectMapper: ObjectMapper): AppState {
            val appStateFile = File(parentDir, FILENAME)
            val result = try {
                if (appStateFile.exists())
                    objectMapper.readValue(appStateFile, AppState::class.java)
                else AppState()
            } catch (ex: IOException) {
                throw IllegalStateException("Failed to load app state", ex)
            }
            result.backingFile = appStateFile
            logger().info("Loaded app state {}", result)

            return result
        }
    }

    fun persist(objectMapper: ObjectMapper) =
        objectMapper.writeValue(backingFile, this)

    override fun toString(): String =
        this.javaClass.simpleName + "{lastPollingTimestamp=$lastPollingTimestamp}"
}