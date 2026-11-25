package com.lapanthere.flink.api.kotlin.typeutils

import com.lapanthere.flink.api.kotlin.serializers.optional.OptionalSerializer
import com.lapanthere.flink.api.kotlin.serializers.optional.OptionalTypeComparator
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.serialization.SerializerConfig
import org.apache.flink.api.common.typeinfo.AtomicType
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.CompositeType
import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import java.util.Optional

/** Type information that treats a Java [Optional] and its value as one indivisible field. */
public class OptionalTypeInformation<T : Any>(
    private val elementTypeInfo: TypeInformation<T>,
) : TypeInformation<Optional<T>>(),
    AtomicType<Optional<T>> {
    override fun isBasicType(): Boolean = false

    override fun isTupleType(): Boolean = false

    override fun getArity(): Int = 1

    override fun getTotalFields(): Int = 1

    @Suppress("UNCHECKED_CAST")
    override fun getTypeClass(): Class<Optional<T>> = Optional::class.java as Class<Optional<T>>

    override fun getGenericParameters(): Map<String, TypeInformation<*>> = mapOf("T" to elementTypeInfo)

    override fun isKeyType(): Boolean =
        elementTypeInfo.isKeyType &&
            (elementTypeInfo is AtomicType<*> || elementTypeInfo is CompositeType<*>)

    override fun createSerializer(config: SerializerConfig?): TypeSerializer<Optional<T>> =
        OptionalSerializer(elementTypeInfo.createSerializer(config))

    override fun createComparator(
        sortOrderAscending: Boolean,
        executionConfig: ExecutionConfig,
    ): TypeComparator<Optional<T>> {
        require(isKeyType) { "Optional<$elementTypeInfo> is not a key type" }

        val elementComparator = createElementComparator(sortOrderAscending, executionConfig)
        val elementSerializer = elementTypeInfo.createSerializer(executionConfig.serializerConfig)
        return OptionalTypeComparator(elementComparator, elementSerializer, sortOrderAscending)
    }

    override fun toString(): String = "OptionalTypeInformation<$elementTypeInfo>"

    override fun canEqual(obj: Any?): Boolean = obj is OptionalTypeInformation<*>

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is OptionalTypeInformation<*> && other.canEqual(this) && elementTypeInfo == other.elementTypeInfo)

    override fun hashCode(): Int = 31 * elementTypeInfo.hashCode() + 1

    @Suppress("UNCHECKED_CAST")
    private fun createElementComparator(
        sortOrderAscending: Boolean,
        executionConfig: ExecutionConfig,
    ): TypeComparator<T> =
        when (elementTypeInfo) {
            is AtomicType<*> -> {
                (elementTypeInfo as AtomicType<T>).createComparator(sortOrderAscending, executionConfig)
            }

            is CompositeType<*> -> {
                val compositeType = elementTypeInfo as CompositeType<T>
                val keyFields = IntArray(compositeType.totalFields) { it }
                val orders = BooleanArray(compositeType.totalFields) { sortOrderAscending }
                compositeType.createComparator(keyFields, orders, 0, executionConfig)
            }

            else -> {
                error("No public comparator factory is available for $elementTypeInfo")
            }
        }
}
