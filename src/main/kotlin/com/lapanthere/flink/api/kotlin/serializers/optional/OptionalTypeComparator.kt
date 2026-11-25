package com.lapanthere.flink.api.kotlin.serializers.optional

import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView
import org.apache.flink.core.memory.MemorySegment
import java.util.Optional

/** Comparator for [Optional] values whose present values are compared by [elementComparator]. */
internal class OptionalTypeComparator<T : Any>(
    private val elementComparator: TypeComparator<T>,
    private val elementSerializer: TypeSerializer<T>,
    private val ascending: Boolean,
) : TypeComparator<Optional<T>>() {
    private var referencePresent: Boolean = false

    override fun hash(record: Optional<T>): Int =
        if (record.isPresent) {
            elementComparator.hash(record.get())
        } else {
            EMPTY_HASH
        }

    override fun setReference(toCompare: Optional<T>) {
        referencePresent = toCompare.isPresent
        if (referencePresent) {
            elementComparator.setReference(toCompare.get())
        }
    }

    override fun equalToReference(candidate: Optional<T>): Boolean =
        when {
            referencePresent != candidate.isPresent -> false
            !referencePresent -> true
            else -> elementComparator.equalToReference(candidate.get())
        }

    override fun compareToReference(referencedComparator: TypeComparator<Optional<T>>): Int {
        require(referencedComparator is OptionalTypeComparator<*>) {
            "Optional values can only be compared against another OptionalTypeComparator"
        }

        @Suppress("UNCHECKED_CAST")
        val other = referencedComparator as OptionalTypeComparator<T>
        return if (referencePresent && other.referencePresent) {
            elementComparator.compareToReference(other.elementComparator)
        } else {
            comparePresence(other.referencePresent, referencePresent)
        }
    }

    override fun compare(
        first: Optional<T>,
        second: Optional<T>,
    ): Int =
        if (first.isPresent && second.isPresent) {
            elementComparator.compare(first.get(), second.get())
        } else {
            comparePresence(first.isPresent, second.isPresent)
        }

    override fun compareSerialized(
        firstSource: DataInputView,
        secondSource: DataInputView,
    ): Int {
        val firstPresent = firstSource.readBoolean()
        val secondPresent = secondSource.readBoolean()

        return when {
            firstPresent && secondPresent -> {
                elementComparator.compareSerialized(firstSource, secondSource)
            }

            firstPresent -> {
                elementSerializer.deserialize(firstSource)
                comparePresence(firstPresent = true, secondPresent = false)
            }

            secondPresent -> {
                elementSerializer.deserialize(secondSource)
                comparePresence(firstPresent = false, secondPresent = true)
            }

            else -> {
                0
            }
        }
    }

    override fun supportsNormalizedKey(): Boolean = elementComparator.supportsNormalizedKey()

    override fun supportsSerializationWithKeyNormalization(): Boolean = false

    override fun getNormalizeKeyLen(): Int {
        if (!supportsNormalizedKey()) {
            return 0
        }

        val elementLength = elementComparator.normalizeKeyLen
        return if (elementLength == Int.MAX_VALUE) Int.MAX_VALUE else elementLength + PRESENCE_BYTES
    }

    override fun isNormalizedKeyPrefixOnly(keyBytes: Int): Boolean =
        !supportsNormalizedKey() ||
            keyBytes < PRESENCE_BYTES ||
            elementComparator.isNormalizedKeyPrefixOnly(keyBytes - PRESENCE_BYTES)

    override fun putNormalizedKey(
        record: Optional<T>,
        target: MemorySegment,
        offset: Int,
        numBytes: Int,
    ) {
        require(supportsNormalizedKey()) { "The element comparator does not support normalized keys" }
        if (numBytes <= 0) {
            return
        }

        target.put(offset, if (record.isPresent) PRESENT_MARKER else EMPTY_MARKER)
        val elementBytes = numBytes - PRESENCE_BYTES
        if (elementBytes == 0) {
            return
        }

        if (record.isPresent) {
            elementComparator.putNormalizedKey(record.get(), target, offset + PRESENCE_BYTES, elementBytes)
        } else {
            repeat(elementBytes) { index ->
                target.put(offset + PRESENCE_BYTES + index, EMPTY_PADDING)
            }
        }
    }

    override fun writeWithKeyNormalization(
        record: Optional<T>,
        target: DataOutputView,
    ): Unit = throw UnsupportedOperationException("Serialization with key normalization is not supported")

    override fun readWithKeyDenormalization(
        reuse: Optional<T>,
        source: DataInputView,
    ): Optional<T> = throw UnsupportedOperationException("Serialization with key normalization is not supported")

    override fun invertNormalizedKey(): Boolean = !ascending

    override fun duplicate(): TypeComparator<Optional<T>> =
        OptionalTypeComparator(
            elementComparator.duplicate(),
            elementSerializer.duplicate(),
            ascending,
        ).also { duplicate ->
            duplicate.referencePresent = referencePresent
        }

    override fun extractKeys(
        record: Any?,
        target: Array<out Any>,
        index: Int,
    ): Int {
        @Suppress("UNCHECKED_CAST")
        (target as Array<Any?>)[index] = record
        return 1
    }

    override fun getFlatComparators(): Array<TypeComparator<*>> = arrayOf(this)

    private fun comparePresence(
        firstPresent: Boolean,
        secondPresent: Boolean,
    ): Int =
        when {
            firstPresent == secondPresent -> 0
            firstPresent -> if (ascending) 1 else -1
            else -> if (ascending) -1 else 1
        }

    private companion object {
        const val EMPTY_HASH: Int = 0
        const val PRESENCE_BYTES: Int = 1
        const val EMPTY_MARKER: Byte = 0
        const val PRESENT_MARKER: Byte = 1
        const val EMPTY_PADDING: Byte = 0
    }
}
