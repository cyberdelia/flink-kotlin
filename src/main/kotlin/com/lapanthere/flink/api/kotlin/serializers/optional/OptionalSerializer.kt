package com.lapanthere.flink.api.kotlin.serializers.optional

import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView
import java.util.Optional

/** A serializer for [Optional] that delegates present values to [elementSerializer]. */
public class OptionalSerializer<T : Any>(
    public val elementSerializer: TypeSerializer<T>,
) : TypeSerializer<Optional<T>>() {
    override fun isImmutableType(): Boolean = elementSerializer.isImmutableType

    override fun duplicate(): TypeSerializer<Optional<T>> = OptionalSerializer(elementSerializer.duplicate())

    override fun createInstance(): Optional<T> = Optional.empty()

    override fun copy(from: Optional<T>): Optional<T> =
        if (from.isPresent) {
            Optional.of(elementSerializer.copy(from.get()))
        } else {
            Optional.empty()
        }

    override fun copy(
        from: Optional<T>,
        reuse: Optional<T>,
    ): Optional<T> = copy(from)

    override fun getLength(): Int = -1

    override fun serialize(
        record: Optional<T>,
        target: DataOutputView,
    ) {
        target.writeBoolean(record.isPresent)
        if (record.isPresent) {
            elementSerializer.serialize(record.get(), target)
        }
    }

    override fun deserialize(source: DataInputView): Optional<T> =
        if (source.readBoolean()) {
            Optional.of(elementSerializer.deserialize(source))
        } else {
            Optional.empty()
        }

    override fun deserialize(
        reuse: Optional<T>,
        source: DataInputView,
    ): Optional<T> = deserialize(source)

    override fun copy(
        source: DataInputView,
        target: DataOutputView,
    ) {
        val present = source.readBoolean()
        target.writeBoolean(present)
        if (present) {
            elementSerializer.copy(source, target)
        }
    }

    override fun snapshotConfiguration(): TypeSerializerSnapshot<Optional<T>> = OptionalSerializerSnapshot(this)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is OptionalSerializer<*> && elementSerializer == other.elementSerializer)

    override fun hashCode(): Int = elementSerializer.hashCode()
}
