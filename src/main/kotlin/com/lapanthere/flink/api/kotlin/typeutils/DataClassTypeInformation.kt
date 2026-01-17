package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.operators.Keys
import org.apache.flink.api.common.serialization.SerializerConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.CompositeType
import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.java.typeutils.TupleTypeInfoBase

private const val REGEX_INT_FIELD = "[0-9]+"
private const val REGEX_FIELD = "[\\p{L}_\\$][\\p{L}\\p{Digit}_\\$]*"
private const val REGEX_NESTED_FIELDS = "($REGEX_FIELD)(\\.(.+))?"
private const val REGEX_NESTED_FIELDS_WILDCARD = "$REGEX_NESTED_FIELDS|\\${Keys.ExpressionKeys.SELECT_ALL_CHAR}"

private val PATTERN_NESTED_FIELDS = Regex(REGEX_NESTED_FIELDS)
private val PATTERN_NESTED_FIELDS_WILDCARD = Regex(REGEX_NESTED_FIELDS_WILDCARD)
private val PATTERN_INT_FIELD = Regex(REGEX_INT_FIELD)

public class DataClassTypeInformation<T : Any>(
    private val klass: Class<T>,
    private val typeParameters: Map<String, TypeInformation<*>>,
    kotlinFieldTypes: Array<TypeInformation<*>>,
    private val kotlinFieldNames: Array<String>,
) : TupleTypeInfoBase<T>(klass, *kotlinFieldTypes) {
    override fun toString(): String =
        buildString {
            append(klass.simpleName)
            if (types.isNotEmpty()) {
                append(types.joinToString(", ", "<", ">"))
            }
        }

    public override fun equals(other: Any?): Boolean =
        when (other) {
            is DataClassTypeInformation<*> -> {
                other.canEqual(this) &&
                    super.equals(other) &&
                    genericParameters == other.genericParameters &&
                    fieldNames.contentEquals(other.fieldNames)
            }

            else -> {
                false
            }
        }

    public override fun canEqual(obj: Any): Boolean = obj is DataClassTypeInformation<*>

    public override fun hashCode(): Int =
        31 * (31 * super.hashCode() + fieldNames.contentHashCode()) + genericParameters.values.toTypedArray().contentHashCode()

    @Deprecated("Deprecated in Java")
    override fun createSerializer(config: ExecutionConfig): TypeSerializer<T> =
        DataClassTypeSerializer(
            klass,
            types.take(arity).map { it.createSerializer(config) }.toTypedArray(),
        )

    override fun createSerializer(config: SerializerConfig?): TypeSerializer<T> =
        DataClassTypeSerializer(klass, types.take(arity).map { it.createSerializer(config) }.toTypedArray())

    override fun createTypeComparatorBuilder(): TypeComparatorBuilder<T> = DataClassTypeComparatorBuilder()

    override fun getFieldNames(): Array<String> = kotlinFieldNames

    override fun getFieldIndex(fieldName: String): Int = kotlinFieldNames.indexOf(fieldName)

    override fun getFlatFields(
        fieldExpression: String,
        offset: Int,
        result: MutableList<FlatFieldDescriptor>,
    ) {
        val match =
            PATTERN_NESTED_FIELDS_WILDCARD.matchEntire(fieldExpression)
                ?: throw InvalidFieldReferenceException("""Invalid tuple field reference "$fieldExpression".""")
        var field = match.groups[0]?.value!!
        if (field == Keys.ExpressionKeys.SELECT_ALL_CHAR) {
            var keyPosition = 0
            fieldTypes.forEach { fieldType ->
                when (fieldType) {
                    is CompositeType<*> -> {
                        fieldType.getFlatFields(Keys.ExpressionKeys.SELECT_ALL_CHAR, offset + keyPosition, result)
                        keyPosition += fieldType.totalFields - 1
                    }

                    else -> {
                        result.add(FlatFieldDescriptor(offset + keyPosition, fieldType))
                    }
                }
            }
        } else {
            field = match.groups[1]?.value!!
            val intFieldMatch = PATTERN_INT_FIELD.matchEntire(field)
            if (intFieldMatch != null) {
                field = "_" + (Integer.valueOf(field) + 1)
            }

            val tail = match.groups[3]?.value
            if (tail == null) {
                fun extractFlatFields(
                    index: Int,
                    pos: Int,
                ) {
                    if (index >= fieldNames.size) {
                        throw InvalidFieldReferenceException("""Unable to find field "$field" in type "$this".""")
                    } else if (field == fieldNames[index]) {
                        when (val fieldType = fieldTypes[index]) {
                            is CompositeType<*> -> fieldType.getFlatFields("*", pos, result)
                            else -> result.add(FlatFieldDescriptor(pos, fieldType))
                        }
                    } else {
                        extractFlatFields(index + 1, pos + fieldTypes[index].totalFields)
                    }
                }
                extractFlatFields(0, offset)
            } else {
                fun extractFlatFields(
                    index: Int,
                    pos: Int,
                ) {
                    if (index >= fieldNames.size) {
                        throw InvalidFieldReferenceException("""Unable to find field "$field" in type "$this".""")
                    } else if (field == fieldNames[index]) {
                        when (val fieldType = fieldTypes[index]) {
                            is CompositeType<*> -> fieldType.getFlatFields(tail, pos, result)

                            else -> throw InvalidFieldReferenceException(
                                """Nested field expression "$tail" not possible on atomic type "$fieldType".""",
                            )
                        }
                    } else {
                        extractFlatFields(index + 1, pos + fieldTypes[index].totalFields)
                    }
                }
                extractFlatFields(0, offset)
            }
        }
    }

    override fun <X : Any?> getTypeAt(fieldExpression: String): TypeInformation<X> {
        val match =
            PATTERN_NESTED_FIELDS.matchEntire(fieldExpression)
                ?: if (fieldExpression.startsWith(Keys.ExpressionKeys.SELECT_ALL_CHAR)) {
                    throw InvalidFieldReferenceException("Wildcard expressions are not allowed here.")
                } else {
                    throw InvalidFieldReferenceException("""Invalid format of data class field expression "$fieldExpression".""")
                }

        var field = match.groups[1]?.value!!
        val tail = match.groups[3]?.value
        val intFieldMatcher = PATTERN_INT_FIELD.matchEntire(field)
        if (intFieldMatcher != null) {
            field = "_" + (Integer.valueOf(field) + 1)
        }

        fieldNames.zip(fieldTypes).forEachIndexed { i, (fieldName, fieldType) ->
            if (fieldName == field) {
                return if (tail == null) {
                    getTypeAt(i)
                } else {
                    when (fieldType) {
                        is CompositeType<*> -> fieldType.getTypeAt(i)

                        else -> throw InvalidFieldReferenceException(
                            """Nested field expression "$tail" not possible on atomic type "$fieldType".""",
                        )
                    }
                }
            }
        }
        throw InvalidFieldReferenceException("""Unable to find field "$field" in type "$this".""")
    }

    override fun getGenericParameters(): Map<String, TypeInformation<*>> = typeParameters

    private inner class DataClassTypeComparatorBuilder<T> : TypeComparatorBuilder<T> {
        private val fieldComparators: MutableList<TypeComparator<*>> = mutableListOf()
        private val logicalKeyFields: MutableList<Int> = mutableListOf()

        override fun initializeTypeComparatorBuilder(size: Int) {}

        override fun addComparatorField(
            fieldId: Int,
            comparator: TypeComparator<*>,
        ) {
            fieldComparators += comparator
            logicalKeyFields += fieldId
        }

        override fun createTypeComparator(config: ExecutionConfig): TypeComparator<T> =
            DataClassTypeComparator(
                logicalKeyFields.toIntArray(),
                fieldComparators.toTypedArray(),
                types.take(logicalKeyFields.max() + 1).map { it.createSerializer(config) }.toTypedArray(),
            )
    }
}
