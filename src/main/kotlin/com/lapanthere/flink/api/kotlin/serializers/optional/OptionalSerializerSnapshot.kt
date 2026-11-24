package com.lapanthere.flink.api.kotlin.serializers.optional

import org.apache.flink.api.common.typeutils.CompositeTypeSerializerSnapshot
import org.apache.flink.api.common.typeutils.TypeSerializer
import java.util.Optional

/** Serializer snapshot that delegates schema compatibility to the serializer of the present value. */
public class OptionalSerializerSnapshot<T : Any> : CompositeTypeSerializerSnapshot<Optional<T>, OptionalSerializer<T>> {
    public constructor() : super()

    public constructor(serializer: OptionalSerializer<T>) : super(serializer)

    override fun getCurrentOuterSnapshotVersion(): Int = CURRENT_VERSION

    override fun getNestedSerializers(outerSerializer: OptionalSerializer<T>): Array<TypeSerializer<*>> =
        arrayOf(outerSerializer.elementSerializer)

    override fun createOuterSerializerWithNestedSerializers(nestedSerializers: Array<TypeSerializer<*>>): OptionalSerializer<T> {
        @Suppress("UNCHECKED_CAST")
        return OptionalSerializer(nestedSerializers.single() as TypeSerializer<T>)
    }

    private companion object {
        const val CURRENT_VERSION: Int = 1
    }
}
