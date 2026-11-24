package com.lapanthere.flink.api.kotlin.serializers.optional

import org.apache.flink.api.common.typeutils.SerializerTestBase
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.base.ListSerializer
import org.apache.flink.api.common.typeutils.base.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.util.Optional

internal class OptionalSerializerTest : SerializerTestBase<Optional<List<String>>>() {
    override fun createSerializer(): TypeSerializer<Optional<List<String>>> = OptionalSerializer(ListSerializer(StringSerializer.INSTANCE))

    override fun getLength(): Int = -1

    @Suppress("UNCHECKED_CAST")
    override fun getTypeClass(): Class<Optional<List<String>>> = Optional::class.java as Class<Optional<List<String>>>

    override fun getTestData(): Array<Optional<List<String>>> =
        arrayOf(
            Optional.empty(),
            Optional.of(emptyList()),
            Optional.of(listOf("one")),
            Optional.of(listOf("one", "two")),
        )

    @Test
    fun `copy delegates a deep copy to the element serializer`() {
        val mutableValue = mutableListOf("one")
        val original = Optional.of<List<String>>(mutableValue)

        val copy = createSerializer().copy(original)
        mutableValue += "two"

        assertNotSame(original.get(), copy.get())
        assertEquals(listOf("one"), copy.get())
    }
}
