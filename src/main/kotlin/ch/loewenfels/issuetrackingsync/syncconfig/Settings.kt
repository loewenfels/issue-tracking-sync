package ch.loewenfels.issuetrackingsync.syncconfig

import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.logger
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.util.Objects

data class Settings(
    var earliestSyncDate: String? = null,
    var trackingApplications: MutableList<IssueTrackingApplication> = mutableListOf(),
    var actionDefinitions: MutableList<SyncActionDefinition> = mutableListOf(),
    var syncFlowDefinitions: MutableList<SyncFlowDefinition> = mutableListOf(),
    var common: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) {
    companion object : Logging {
        fun loadFromFile(fileLocation: String, objectMapper: ObjectMapper): Settings {
            val settingsFile = File(fileLocation)
            try {
                if (!settingsFile.exists()) {
                    throw IOException("Settings file " + settingsFile.absolutePath + " not found.")
                }
                logger().info("Loading settings from {}", settingsFile.absolutePath)
                val result = objectMapper.readValue(settingsFile, Settings::class.java)
                result.mapCommons()
                return result
            } catch (ex: IOException) {
                throw IllegalStateException("Failed to load settings", ex)
            }
        }
    }

    fun toTrackingApplication(name: TrackingApplicationName): IssueTrackingApplication? =
        trackingApplications.find { Objects.equals(it.name, name) }

    /**
     * For any map holding a key `#common`, attempt to locate the common definition in [common]
     * and replace the content with a copy. If the value references ends with `->reversed`, the
     * map is reversed
     */
    private fun mapCommons() =
        actionDefinitions.flatMap { it.fieldMappingDefinitions }.forEach { fldMapping ->
            mapAssociations(fldMapping)

            fldMapping.fieldSkipEvalutors.map { fldSkipEvaluator ->
                mapAssociations(fldSkipEvaluator)
            }
        }

    private fun mapCommons(fieldMapping: AssociationsFieldDefinition, commonName: String, invert: Boolean) {
        (common[commonName] ?: throw IllegalArgumentException("Undefined common expression $commonName")).let {
            fieldMapping.associations.remove("#common")
            fieldMapping.associations.putAll(
                if (invert) {
                    it.entries.associate { (k, v) -> v to k }.toMutableMap()
                } else {
                    it
                }
            )
        }
    }

    private fun mapAssociations(fieldMapping: AssociationsFieldDefinition) {
        fieldMapping.associations["#common"]?.split(",")?.map(String::trim)?.forEach {
            if (it.endsWith("->reversed")) {
                mapCommons(fieldMapping, it.substring(0, it.length - 10), true)
            } else {
                mapCommons(fieldMapping, it, false)
            }
        }
    }

}
