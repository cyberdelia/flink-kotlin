package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot
import org.apache.flink.api.java.typeutils.runtime.TupleSerializerBase
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView
import kotlin.reflect.full.functions
import kotlin.reflect.full.primaryConstructor

public class DataClassTypeSerializer<T : Any>(
    type: Class<T>?,
    fieldSerializers: Array<TypeSerializer<*>>
) : TupleSerializerBase<T>(type, fieldSerializers) {
    override fun duplicate(): TypeSerializer<T> = DataClassTypeSerializer(
        tupleClass,
        fieldSerializers.map { it.duplicate() }.toTypedArray()
    )

    override fun createInstance(fields: Array<out Any?>): T? = try {
        tupleClass.kotlin.primaryConstructor?.call(*fields)
    } catch (e: Throwable) {
        null
    }

    override fun createInstance(): T? =
        createInstance(fieldSerializers.map { it.createInstance() }.toTypedArray())

    override fun deserialize(source: DataInputView): T? =
        createInstance(fieldSerializers.map { it.deserialize(source) }.toTypedArray())

    override fun snapshotConfiguration(): TypeSerializerSnapshot<T> = DataClassTypeSerializerSnapshot(this)

    override fun createOrReuseInstance(fields: Array<out Any>, reuse: T): T? = createInstance(fields)

    override fun deserialize(reuse: T?, source: DataInputView): T? = deserialize(source)

    override fun serialize(record: T?, target: DataOutputView) {
        fieldSerializers.forEachIndexed { i, serializer ->
            serializer.serialize(record?.component(i), target)
        }
    }

    override fun copy(from: T?, reuse: T): T? = copy(from)

    override fun copy(from: T?): T? = if (from == null) {
        null
    } else {
        createInstance(
            fieldSerializers.mapIndexed { i, serializer ->
                serializer.copy(from.component(i))
            }.toTypedArray()
        )
    }
}

internal fun <T : Any> T.component(i: Int): Any? =
    this::class.functions.first { it.name == "component${i + 1}" }.call(this)
