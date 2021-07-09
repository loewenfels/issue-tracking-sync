package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FieldSkippingEvaluatorFactoryTest {
    @Test
    fun getEvaluator_validClass_classLoaded() {
        // arrange
        val mapping = buildFieldMappingDefinition(mutableListOf(SkipUnlessAllowedState::class.java.name))
        // act
        val evaluator = FieldSkippingEvaluatorFactory.getEvaluators(mapping)
        // assert
        assertTrue(evaluator.size > 0)
        assertNotNull(evaluator[0])
    }

    @Test
    fun getEvaluator_invalidClassName_exception() {
        // arrange
        val unknownMapping = buildFieldMappingDefinition(mutableListOf("ch.loewenfels.MapperDoesntExist"))
        // act
        assertThrows(IllegalArgumentException::class.java) {
            FieldSkippingEvaluatorFactory.getEvaluators(unknownMapping)
        }
    }

    private fun buildFieldMappingDefinition(evaluatorClassNames: MutableList<String>) =
        FieldMappingDefinition(
            "", "", "",
            fieldSkipEvaluators = evaluatorClassNames.map { FieldSkippingEvaluatorDefinition(classname = it) }
                .toMutableList()
        )
}