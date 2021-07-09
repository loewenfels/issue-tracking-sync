package ch.loewenfels.issuetrackingsync.syncconfig

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.app.SyncApplicationProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

internal class SettingsTest : AbstractSpringTest() {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var syncApplicationProperties: SyncApplicationProperties

    @Test
    fun loadFromFile_validParameters_fileProperlyLoaded() {
        // arrange
        val classpathResource = javaClass.getResource("/settings.yml")
        assertNotNull(classpathResource, "Failed to locate settings.yml")
        // act
        val result = Settings.loadFromFile(classpathResource.path)
        // assert
        assertEquals(2, result.trackingApplications.size, "Count of configured tracking applications")
        assertEquals(5, result.actionDefinitions.size, "Count of configured actions")
        val actionDefinition = result.actionDefinitions[0]
        assertEquals("SimpleFieldsRtcToJira", actionDefinition.name, "Name of first action definition")
        assertEquals(5, actionDefinition.fieldMappingDefinitions.size, "Count of field mappings in first action")
        assertEquals(
            "summary", actionDefinition.fieldMappingDefinitions[0].sourceName, "Source property of first field mapping"
        )
        val cantonMapping = actionDefinition.fieldMappingDefinitions[2]
        assertEquals("cantons", cantonMapping.sourceName, "Source property of third field mapping")
        assertEquals(23, cantonMapping.associations.size, "Size of 'associations' map")
        assertEquals("NW,OW", cantonMapping.associations["Unterwalden"], "Common entry loading")

        val cantonMappingExtended = actionDefinition.fieldMappingDefinitions[3]
        assertEquals("cantons_ext", cantonMappingExtended.sourceName, "Source property of fourth field mapping")
        assertEquals(24, cantonMappingExtended.associations.size, "Size of 'associations' map")
        assertEquals("BE", cantonMappingExtended.associations["AKB"], "Additional mappings are loaded")

        val cantonMappingTwoCommonEntries = actionDefinition.fieldMappingDefinitions[4]
        assertEquals(
            "cantons_two_commons",
            cantonMappingTwoCommonEntries.sourceName,
            "Source property of fith field mapping"
        )
        assertEquals(24, cantonMappingTwoCommonEntries.associations.size, "Size of 'associations' map")
        assertEquals("BE", cantonMappingTwoCommonEntries.associations["AKB"], "Additional mappings are loaded")

        val jiraToRtcAction = result.actionDefinitions[1]
        assertEquals("SimpleFieldsJiraToRtc", jiraToRtcAction.name, "Name of first action definition")
        val reversedCantonMapping = jiraToRtcAction.fieldMappingDefinitions[2]
        assertEquals(23, reversedCantonMapping.associations.size, "Size of 'associations' map")
        assertEquals("Unterwalden", reversedCantonMapping.associations["NW,OW"], "Common entry loading, reversed")

        assertEquals(1, result.syncFlowDefinitions.size, "Count of configured flows")
        val syncFlow = result.syncFlowDefinitions[0]
        assertEquals("id", syncFlow.keyFieldMappingDefinition.sourceName, "source property name for key field mapping")
        assertEquals(
            "RTC ID", syncFlow.keyFieldMappingDefinition.targetName, "target property name for key field mapping"
        )
        assertEquals(
            "key",
            syncFlow.writeBackFieldMappingDefinition[0].sourceName,
            "source property name for write-back mapping"
        )
        assertEquals(
            "ch.loewenfels.team.workitem.attribute.external_refid",
            syncFlow.writeBackFieldMappingDefinition[0].targetName,
            "target property name for write-back mapping"
        )
        assertNotNull(syncFlow.defaultsForNewIssue)
        assertEquals("TST", syncFlow.defaultsForNewIssue?.project, "Default project for new issues")
        assertEquals(3, syncFlow.actions.size, "Count of actions in sync flow")
    }

    @Test
    fun loadFromFile_currentSettingsFile_fileLoadedAndProperlyFormatted() {
        // arrange
        val settingsName = File(syncApplicationProperties.settingsLocation).name
        val settingsFile = File(settingsName)
        Assumptions.assumeTrue(settingsFile.exists())
        try {
            // act
            val result = Settings.loadFromFile(settingsFile.absolutePath)
            // assert
            assertNotNull(result)
        } catch (ex: Exception) {
            fail("$settingsName could not be loaded, it may not properly formatted: ${ex.message}")
        }
    }

    @Test
    fun serialize_validSettingsDataStructure_fileProperlyLoaded() {
        // arrange
        val jira = IssueTrackingApplication()
        jira.name = "JIRA"
        jira.username = "foobar"
        val settings = Settings()
        settings.trackingApplications.add(jira)
        // act
        val serialized = objectMapper.writeValueAsString(settings)
        // assert
        assertThat(serialized, containsString("foobar"))
    }
}
