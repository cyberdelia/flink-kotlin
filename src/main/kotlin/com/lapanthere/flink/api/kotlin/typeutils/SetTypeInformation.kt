package com.lapanthere.flink.api.kotlin.typeutils

import com.lapanthere.flink.api.kotlin.serializers.set.SetTypeSerializer
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.serialization.SerializerConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.TypeSerializer

/**
 * Type information for kotlin set collection
 * Inspired from [org.apache.flink.api.java.typeutils.ListTypeInfo]
 */
public class SetTypeInformation<T>(private val elementTypeInfo: TypeInformation<T>) : TypeInformation<Set<T>>() {

    override fun isBasicType(): Boolean = false

    override fun isTupleType(): Boolean = false

    override fun getArity(): Int = 0

    override fun getTotalFields(): Int = 1

    @Suppress("UNCHECKED_CAST")
    override fun getTypeClass(): Class<Set<T>> {
        return Set::class.java as Class<Set<T>>
    }

    override fun isKeyType(): Boolean = false

    @Deprecated("Deprecated in Java")
    override fun createSerializer(config: ExecutionConfig?): TypeSerializer<Set<T>> {
        return createSerializer(config?.serializerConfig)
    }

    override fun createSerializer(config: SerializerConfig?): TypeSerializer<Set<T>> {
        val elementSerializer = elementTypeInfo.createSerializer(config)
        return SetTypeSerializer(elementSerializer)
    }

    override fun toString(): String {
        return "SetTypeInformation<$elementTypeInfo>"
    }

    override fun canEqual(obj: Any?): Boolean = obj?.javaClass == javaClass

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetTypeInformation<*>) return false;
        return other.canEqual(this) && other.elementTypeInfo == this.elementTypeInfo
    }

    override fun hashCode(): Int {
        return 31 * elementTypeInfo.hashCode() + 1
    }
}
