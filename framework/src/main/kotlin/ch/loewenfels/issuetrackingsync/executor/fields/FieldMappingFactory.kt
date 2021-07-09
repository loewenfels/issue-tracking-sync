package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.executor.fields.skipping.FieldSkippingEvaluator
import ch.loewenfels.issuetrackingsync.executor.fields.skipping.FieldSkippingEvaluatorFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

@Suppress("TooGenericExceptionCaught")
object FieldMappingFactory {
    private val mapperInstances = mutableMapOf<String, FieldMapper>()

    fun getMapping(fieldMappingDefinition: FieldMappingDefinition): FieldMapping =
        FieldMapping(
            fieldMappingDefinition.sourceName,
            fieldMappingDefinition.targetName,
            getMapper(fieldMappingDefinition),
            getFieldSkippingEvaluator(fieldMappingDefinition)
        )

    fun getKeyMapping(fieldMappingDefinition: FieldMappingDefinition): KeyFieldMapping =
        KeyFieldMapping(
            fieldMappingDefinition.sourceName,
            fieldMappingDefinition.targetName,
            getMapper(fieldMappingDefinition),
            fieldMappingDefinition.callback
        )

    private fun getMapper(fieldMappingDefinition: FieldMappingDefinition): FieldMapper {
        return mapperInstances[fieldMappingDefinition.mapperClassname] ?: buildMapperAndCacheIfReusable(
            fieldMappingDefinition
        )
    }

    private fun getFieldSkippingEvaluator(fieldMappingDefinition: FieldMappingDefinition): List<FieldSkippingEvaluator> =
        FieldSkippingEvaluatorFactory.getEvaluators(fieldMappingDefinition)

    private fun buildMapperAndCacheIfReusable(fieldMappingDefinition: FieldMappingDefinition): FieldMapper {
        val mapper = buildMapper(fieldMappingDefinition)
        if (fieldMappingDefinition.associations.isEmpty()) {
            mapperInstances[fieldMappingDefinition.mapperClassname] = mapper
        }
        return mapper
    }

    @Suppress("UNCHECKED_CAST", "SwallowedException")
    private fun buildMapper(fieldMappingDefinition: FieldMappingDefinition): FieldMapper {
        val mapperClass = try {
            Class.forName(fieldMappingDefinition.mapperClassname) as Class<FieldMapper>
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to load field mapper class ${fieldMappingDefinition.mapperClassname}",
                e
            )
        }
        return try {
            mapperClass.getConstructor(FieldMappingDefinition::class.java).newInstance(fieldMappingDefinition)
        } catch (e: Exception) {
            // no ctor taking a FieldMappingDefinition, so look for empty ctor
            try {
                mapperClass.getDeclaredConstructor().newInstance()
            } catch (e2: Exception) {
                throw IllegalArgumentException("Failed to instantiate mapper class $mapperClass", e2)
            }
        }
    }
}