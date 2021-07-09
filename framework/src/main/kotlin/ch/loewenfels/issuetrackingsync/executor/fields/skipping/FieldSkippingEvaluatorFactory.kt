package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition

object FieldSkippingEvaluatorFactory : Logging {
    private val evaluators: MutableMap<FieldMappingDefinition, MutableList<FieldSkippingEvaluator>> = mutableMapOf()

    fun getEvaluators(fieldMappingDefinition: FieldMappingDefinition): MutableList<FieldSkippingEvaluator> {
        return evaluators[fieldMappingDefinition] ?: createFieldSkippEvaluators(fieldMappingDefinition)
    }

    private fun createFieldSkippEvaluators(fieldMappingDefinition: FieldMappingDefinition): MutableList<FieldSkippingEvaluator> {
        evaluators[fieldMappingDefinition] = fieldMappingDefinition.fieldSkipEvaluators
            .mapNotNull(this::createFieldSkippingEvaluator)
            .toMutableList()
        return evaluators[fieldMappingDefinition] ?: mutableListOf()
    }

    @Suppress("UNCHECKED_CAST")
    private fun createFieldSkippingEvaluator(fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition): FieldSkippingEvaluator? {
        val clazz: Class<FieldSkippingEvaluator>?
        try {
            clazz = Class.forName(fieldSkippingEvaluatorDefinition.classname) as Class<FieldSkippingEvaluator>
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to load FieldSkippingEvaluator class ${fieldSkippingEvaluatorDefinition.classname}",
                e
            )
        }
        return instantiateClass(clazz, fieldSkippingEvaluatorDefinition)
    }

    private fun instantiateClass(
        clazz: Class<FieldSkippingEvaluator>,
        fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition
    ): FieldSkippingEvaluator? {
        return try {
            clazz.getConstructor(FieldSkippingEvaluatorDefinition::class.java)
                .newInstance(fieldSkippingEvaluatorDefinition)
        } catch (e: Exception) {
            null
        } ?: try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to instantiate evaluator class $clazz", e)
        }
    }
}