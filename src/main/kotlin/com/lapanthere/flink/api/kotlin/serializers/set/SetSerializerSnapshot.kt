package com.lapanthere.flink.api.kotlin.serializers.set

import org.apache.flink.api.common.typeutils.CompositeTypeSerializerSnapshot
import org.apache.flink.api.common.typeutils.TypeSerializer

public class SetSerializerSnapshot<T> : CompositeTypeSerializerSnapshot<Set<T>, SetTypeSerializer<T>> {

    public constructor() : super()
    public constructor(serializerInstance: SetTypeSerializer<T>) : super(serializerInstance)

    override fun getCurrentOuterSnapshotVersion(): Int = CURRENT_VERSION

    override fun createOuterSerializerWithNestedSerializers(nestedSerializers: Array<out TypeSerializer<*>>): SetTypeSerializer<T> {
        @Suppress("UNCHECKED_CAST")
        val elementSerializer = nestedSerializers[0] as TypeSerializer<T>
        return SetTypeSerializer(elementSerializer)
    }

    override fun getNestedSerializers(outerSerializer: SetTypeSerializer<T>): Array<out TypeSerializer<*>> {
        return arrayOf(outerSerializer.elementSerializer)
    }

    public companion object {
        public const val CURRENT_VERSION: Int = 1
    }
}
