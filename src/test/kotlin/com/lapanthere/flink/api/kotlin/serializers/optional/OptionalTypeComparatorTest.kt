package com.lapanthere.flink.api.kotlin.serializers.optional

import com.lapanthere.flink.api.kotlin.typeutils.OptionalTypeInformation
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo
import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.api.common.typeutils.ComparatorTestBase
import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.core.memory.MemorySegmentFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.math.sign

internal class OptionalTypeComparatorTest : ComparatorTestBase<Optional<String>>() {
    private val typeInformation = OptionalTypeInformation(Types.STRING)

    override fun createComparator(ascending: Boolean): TypeComparator<Optional<String>> =
        typeInformation.createComparator(ascending, ExecutionConfig())

    override fun createSerializer(): TypeSerializer<Optional<String>> = typeInformation.createSerializer(ExecutionConfig().serializerConfig)

    override fun getSortedTestData(): Array<Optional<String>> =
        arrayOf(
            Optional.empty(),
            Optional.of("a"),
            Optional.of("b"),
            Optional.of("c"),
        )

    @Test
    fun `descending order puts present values before empty`() {
        val comparator = createComparator(false)

        assertTrue(comparator.compare(Optional.of("a"), Optional.empty()) < 0)
        assertTrue(comparator.compare(Optional.empty(), Optional.of("a")) > 0)
        assertTrue(comparator.compare(Optional.of("b"), Optional.of("a")) < 0)
    }

    @Test
    fun `present value hash is delegated to the element comparator`() {
        val bytesTypeInfo = OptionalTypeInformation(PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO)
        val comparator = bytesTypeInfo.createComparator(true, ExecutionConfig())
        val first = Optional.of(byteArrayOf(1, 2, 3))
        val second = Optional.of(byteArrayOf(1, 2, 3))

        assertEquals(0, comparator.compare(first, second))
        assertEquals(comparator.hash(first), comparator.hash(second))
    }

    @Test
    fun `duplicate has independent reference state`() {
        val comparator = createComparator(true)
        comparator.setReference(Optional.of("original"))
        val duplicate = comparator.duplicate()

        duplicate.setReference(Optional.of("duplicate"))

        assertTrue(comparator.equalToReference(Optional.of("original")))
        assertFalse(comparator.equalToReference(Optional.of("duplicate")))
        assertTrue(duplicate.equalToReference(Optional.of("duplicate")))
    }

    @Test
    fun `normalized keys preserve ascending and descending order`() {
        val values =
            listOf(
                Optional.empty(),
                Optional.of(Int.MIN_VALUE),
                Optional.of(-1),
                Optional.of(0),
                Optional.of(Int.MAX_VALUE),
            )

        listOf(true, false).forEach { ascending ->
            val comparator = OptionalTypeInformation(Types.INT).createComparator(ascending, ExecutionConfig())
            for (keyBytes in 1..comparator.normalizeKeyLen) {
                values.forEach { first ->
                    values.forEach { second ->
                        val expected = comparator.compare(first, second).sign
                        val actual =
                            compareUnsigned(
                                normalizedKey(comparator, first, keyBytes),
                                normalizedKey(comparator, second, keyBytes),
                            ).sign

                        if (actual != 0) {
                            assertEquals(expected, actual)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `normalized key length saturates and writes only the requested range`() {
        val stringComparator = createComparator(true)
        assertEquals(Int.MAX_VALUE, stringComparator.normalizeKeyLen)

        val bytes = ByteArray(9) { 0x55 }
        val target = MemorySegmentFactory.wrap(bytes)
        stringComparator.putNormalizedKey(Optional.empty(), target, 2, 5)

        assertEquals(0x55.toByte(), bytes[1])
        assertTrue(bytes.sliceArray(2..6).all { it == 0.toByte() })
        assertEquals(0x55.toByte(), bytes[7])
    }

    private fun normalizedKey(
        comparator: TypeComparator<Optional<Int>>,
        value: Optional<Int>,
        length: Int,
    ): ByteArray {
        val bytes = ByteArray(length)
        comparator.putNormalizedKey(value, MemorySegmentFactory.wrap(bytes), 0, length)
        if (comparator.invertNormalizedKey()) {
            bytes.indices.forEach { index -> bytes[index] = bytes[index].toInt().xor(0xff).toByte() }
        }
        return bytes
    }

    private fun compareUnsigned(
        first: ByteArray,
        second: ByteArray,
    ): Int {
        first.indices.forEach { index ->
            val comparison = (first[index].toInt() and 0xff).compareTo(second[index].toInt() and 0xff)
            if (comparison != 0) {
                return comparison
            }
        }
        return 0
    }
}
