package ch.loewenfels.issuetrackingsync.syncconfig

import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.logger
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.util.*

data class Settings(
    var earliestSyncDate: String? = null,
    var trackingApplications: MutableList<IssueTrackingApplication> = mutableListOf(),
    var actionDefinitions: MutableList<SyncActionDefinition> = mutableListOf(),
    var syncFlowDefinitions: MutableList<SyncFlowDefinition> = mutableListOf(),
    var common: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) {
    companion object : Logging {
        fun loadFromFile(fileLocation: String): Settings {
            val settingsFile = File(fileLocation)
            if (!settingsFile.exists()) {
                throw IllegalStateException("Settings file " + settingsFile.absolutePath + " not found.")
            }
            logger().info("Loading settings from {}", settingsFile.absolutePath)
            val objectMapper = ObjectMapper(YAMLFactory())
            objectMapper.findAndRegisterModules()
            val result = objectMapper.readValue(settingsFile, Settings::class.java)
            result.mapCommons()
            return result
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
        actionDefinitions
            .flatMap { it.fieldMappingDefinitions }
            .forEach { fldMapping ->
                mapAssociations(fldMapping)

                fldMapping.fieldSkipEvaluators.map { fldSkipEvaluator ->
                    mapAssociations(fldSkipEvaluator)
                }
            }

    private fun mapCommons(fieldMapping: AssociationsFieldDefinition, commonName: String, invert: Boolean) {
        (common[commonName] ?: throw IllegalArgumentException("Undefined common expression $commonName"))
            .let {
                fieldMapping.associations.remove("#common")
                fieldMapping.associations.putAll(
                    if (invert) {
                        it.entries.associate { (k, v) -> v to k }.toMap()
                    } else {
                        it
                    }
                )
            }
    }

    private fun mapAssociations(fieldMapping: AssociationsFieldDefinition) {
        fieldMapping.associations["#common"]
            ?.split(",")
            ?.map(String::trim)
            ?.forEach {
                if (it.endsWith("->reversed")) {
                    mapCommons(fieldMapping, it.substring(0, it.length - 10), true)
                } else {
                    mapCommons(fieldMapping, it, false)
                }
            }
    }
}
