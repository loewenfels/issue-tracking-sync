package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.safeEq
import ch.loewenfels.issuetrackingsync.safeNot
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

internal class CompoundStringFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    @Test
    fun getValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        // act
        val result = testee.getValue(issue, "text,text2,text3", sourceClient)
        // assert
        assertNotNull(result)
        assertEquals(
            "text should have no title<br><h4>Text 2</h4><br>foobar<br><h4>Text 3</h4><br>foobar<br>",
            (result as String)
        )
    }

    @Test
    fun setValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = "foobar\n\n<h4>Text 2</h4>\nSome more text\n<h4>Text 3</h4>\nAnd still some more"
        // act
        testee.setValue(issue, "text,text2,text3", issue, targetClient, value)
        // assert
        verify(targetClient).setHtmlValue(issue, issue, "text", "foobar")
        verify(targetClient).setHtmlValue(issue, issue, "text2", "Some more text")
        verify(targetClient).setHtmlValue(issue, issue, "text3", "And still some more")
    }

    @Test
    fun setValue_associationsFromSourceToTargetWithOneSourceFieldNameMultipleTargetFieldNames_onlyTheOneTargetFieldNameShouldGetSet() {
        // arrange
        val associations = mutableMapOf(
            "defectdescription" to "\nh4. Text2",
            "conduct" to "\nh4. Text3"
        )
        val fieldDefinitions = FieldMappingDefinition(
            "description",
            "description,defectdescription,conduct",
            CompoundStringFieldMapper::class.toString(),
            associations = associations
        )
        val testee = CompoundStringFieldMapper(fieldDefinitions)
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = "foobar\nh4. Text2\nSome more text\nh4. Text3\nAnd still some more"
        // act
        testee.setValue(issue, "description,defectdescription,conduct", issue, targetClient, value)
        // assert
        verify(targetClient).setHtmlValue(
            safeEq(issue),
            safeEq(issue),
            safeEq("conduct"),
            safeEq("And still some more")
        )
        verify(targetClient).setHtmlValue(
            safeEq(issue),
            safeEq(issue),
            safeEq("defectdescription"),
            safeEq("Some more text")
        )
        verify(targetClient).setHtmlValue(safeEq(issue), safeEq(issue), safeEq("description"), safeEq("foobar"))
    }

    @Test
    fun setValue_associationsFromSourceToTargetWithOneTargetFieldName_onlyTheOneTargetFieldNameShouldGetSet() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = "foobar\nh4. Text 2\nSome more text\nh4. Text 3\nAnd still some more"
        // act
        testee.setValue(issue, "description", issue, targetClient, value)
        // assert
        verify(targetClient, never()).setHtmlValue(
            safeEq(issue),
            safeEq(issue),
            safeNot(safeEq("description")),
            anyString()
        )
    }

    private fun buildTestee(): CompoundStringFieldMapper {
        val associations =
            mutableMapOf(
                "text2" to "<h4>Text 2</h4>",
                "text3" to "<h4>Text 3</h4>"
            )
        val fieldDefinition = FieldMappingDefinition(
            "text,text2,text3", "description",
            CompoundStringFieldMapper::class.toString(),
            associations = associations
        )
        return CompoundStringFieldMapper(fieldDefinition)
    }
}