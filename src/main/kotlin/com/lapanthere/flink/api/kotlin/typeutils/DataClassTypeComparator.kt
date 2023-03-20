package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.java.typeutils.runtime.TupleComparatorBase
import org.apache.flink.core.memory.MemorySegment
import org.apache.flink.types.NullKeyFieldException

internal class DataClassTypeComparator<T>(
    keys: IntArray,
    comparators: Array<TypeComparator<*>>,
    serializers: Array<TypeSerializer<*>>,
) : TupleComparatorBase<T>(keys, comparators, serializers) {
    override fun hash(record: T?): Int {
        val code = comparators.first().hash(record?.component(0))
        return comparators.drop(1).foldIndexed(code) { i, hash, comparator ->
            val j = i + 1
            try {
                (hash * HASH_SALT[j and 0x1f]) + comparator.hash(record?.component(j))
            } catch (e: NullPointerException) {
                throw NullKeyFieldException(keyPositions[j])
            }
        }
    }

    override fun setReference(toCompare: T?) {
        keyPositions.zip(comparators).forEach { (position, comparator) ->
            try {
                comparator.setReference(toCompare?.component(position))
            } catch (e: NullPointerException) {
                throw NullKeyFieldException(position)
            }
        }
    }

    override fun equalToReference(candidate: T?): Boolean =
        keyPositions.zip(comparators).all { (position, comparator) ->
            try {
                comparator.equalToReference(candidate?.component(position))
            } catch (e: NullPointerException) {
                throw NullKeyFieldException(position)
            }
        }

    override fun compare(first: T?, second: T?): Int {
        keyPositions.zip(comparators).forEach { (position, comparator) ->
            try {
                val cmp = comparator.compare(first?.component(position), second?.component(position))
                if (cmp != 0) {
                    return cmp
                }
            } catch (e: NullPointerException) {
                throw NullKeyFieldException(position)
            }
        }
        return 0
    }

    override fun putNormalizedKey(record: T?, target: MemorySegment, offset: Int, numBytes: Int) {
        var localNumBytes = numBytes
        var localOffset = offset
        var i = 0
        try {
            while (i < numLeadingNormalizableKeys && numBytes > 0) {
                var len: Int = normalizedKeyLengths[i]
                len = if (numBytes >= len) len else numBytes
                val comparator = comparators[i]
                comparator.putNormalizedKey(record?.component(keyPositions[i]), target, offset, len)
                localNumBytes -= len
                localOffset += len
                i += 1
            }
        } catch (e: NullPointerException) {
            throw NullKeyFieldException(keyPositions[i])
        }
    }

    override fun duplicate(): TypeComparator<T> {
        instantiateDeserializationUtils()
        val comparator = DataClassTypeComparator<T>(keyPositions, comparators, serializers)
        comparator.privateDuplicate(this)
        return comparator
    }

    override fun extractKeys(record: Any?, target: Array<out Any>, index: Int): Int {
        var localIndex = index
        keyPositions.zip(comparators).forEach { (position, comparator) ->
            localIndex += comparator.extractKeys(position, target, localIndex)
        }
        return localIndex - index
    }
}
