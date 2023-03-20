package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeutils.CompositeTypeSerializerSnapshot
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView
import org.apache.flink.util.InstantiationUtil

public class DataClassTypeSerializerSnapshot<T : Any> :
    CompositeTypeSerializerSnapshot<T, DataClassTypeSerializer<T>> {
    private var type: Class<T>? = null

    public constructor() : super(DataClassTypeSerializer::class.java)

    public constructor(type: Class<T>) : super(DataClassTypeSerializer::class.java) {
        this.type = type
    }

    public constructor(serializerInstance: DataClassTypeSerializer<T>) : super(serializerInstance) {
        type = serializerInstance.tupleClass
    }

    override fun getCurrentOuterSnapshotVersion(): Int = 1

    override fun getNestedSerializers(outerSerializer: DataClassTypeSerializer<T>): Array<out TypeSerializer<*>> =
        outerSerializer.fieldSerializers

    override fun createOuterSerializerWithNestedSerializers(nestedSerializers: Array<TypeSerializer<*>>): DataClassTypeSerializer<T> =
        DataClassTypeSerializer(type!!, nestedSerializers)

    override fun writeOuterSnapshot(out: DataOutputView) {
        out.writeUTF(type!!.name)
    }

    override fun readOuterSnapshot(
        readOuterSnapshotVersion: Int,
        `in`: DataInputView,
        userCodeClassLoader: ClassLoader,
    ) {
        type = InstantiationUtil.resolveClassByName(`in`, userCodeClassLoader)
    }

    override fun resolveOuterSchemaCompatibility(newSerializer: DataClassTypeSerializer<T>): OuterSchemaCompatibility =
        if (type == newSerializer.tupleClass) {
            OuterSchemaCompatibility.COMPATIBLE_AS_IS
        } else {
            OuterSchemaCompatibility.INCOMPATIBLE
        }
}
