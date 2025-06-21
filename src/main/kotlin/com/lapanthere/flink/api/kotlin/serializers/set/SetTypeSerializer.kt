package com.lapanthere.flink.api.kotlin.serializers.set

import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView

/**
 * Type Serializer for kotlin set collection
 * Inspired from [org.apache.flink.api.common.typeutils.base.ListSerializer]
 */
public class SetTypeSerializer<T>(
    public val elementSerializer: TypeSerializer<T>
) : TypeSerializer<Set<T>>() {
    override fun isImmutableType(): Boolean = false

    override fun duplicate(): TypeSerializer<Set<T>> {
        val duplicateElement = elementSerializer.duplicate()
        return if (duplicateElement === elementSerializer)
            this
        else
            SetTypeSerializer<T>(duplicateElement)
    }

    override fun createInstance(): Set<T> = emptySet()

    override fun copy(from: Set<T>): Set<T> {
        if (elementSerializer.isImmutableType) {
            return HashSet(from)
        }
        return HashSet<T>(from.size).apply {
            for (element in from) {
                add(elementSerializer.copy(element))
            }
        }
    }

    override fun copy(from: Set<T>, reuse: Set<T>): Set<T> = copy(from)

    override fun getLength(): Int = -1 // var length

    override fun serialize(record: Set<T>, target: DataOutputView) {
        val size = record.size
        target.writeInt(size);

        for (element in record) {
            elementSerializer.serialize(element, target)
        }
    }

    override fun deserialize(source: DataInputView): Set<T> {
        val size = source.readInt()
        return HashSet<T>(size).apply {
            repeat(size) {
                add(elementSerializer.deserialize(source))
            }
        }
    }

    override fun deserialize(reuse: Set<T>, source: DataInputView): Set<T> {
        return deserialize(source)
    }

    override fun copy(source: DataInputView, target: DataOutputView) {
        val size = source.readInt()
        target.writeInt(size)
        repeat(size) {
            elementSerializer.copy(source, target)
        }
    }

    override fun snapshotConfiguration(): TypeSerializerSnapshot<Set<T>> {
        return SetSerializerSnapshot(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetTypeSerializer<*>

        return elementSerializer == other.elementSerializer
    }

    override fun hashCode(): Int {
        return elementSerializer.hashCode()
    }
}
