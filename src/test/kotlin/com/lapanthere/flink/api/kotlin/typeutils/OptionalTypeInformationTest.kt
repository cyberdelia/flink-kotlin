package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.api.common.typeutils.TypeInformationTestBase
import org.apache.flink.api.java.typeutils.ListTypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

internal class OptionalTypeInformationTest : TypeInformationTestBase<OptionalTypeInformation<*>>() {
    override fun getTestData(): Array<OptionalTypeInformation<*>> =
        arrayOf(
            createTypeInformation<Optional<String>>() as OptionalTypeInformation<*>,
            createTypeInformation<Optional<Int>>() as OptionalTypeInformation<*>,
            createTypeInformation<Optional<List<String>>>() as OptionalTypeInformation<*>,
            createTypeInformation<Optional<DataClass>>() as OptionalTypeInformation<*>,
        )

    @Test
    fun `type extraction creates an atomic optional key type`() {
        val typeInformation = createTypeInformation<Optional<String>>()

        assertTrue(typeInformation is OptionalTypeInformation<*>)
        assertTrue(typeInformation.isKeyType)
        assertEquals(1, typeInformation.arity)
        assertEquals(1, typeInformation.totalFields)
        assertEquals(Types.STRING, typeInformation.genericParameters["T"])
    }

    @Test
    fun `optional is not a key when its element is not a key`() {
        val typeInformation = OptionalTypeInformation(ListTypeInfo(Types.STRING))

        assertFalse(typeInformation.isKeyType)
    }

    @Test
    fun `optional delegates comparison of a composite key`() {
        val typeInformation = createTypeInformation<Optional<DataClass>>()
        require(typeInformation is OptionalTypeInformation<DataClass>)
        val comparator = typeInformation.createComparator(true, ExecutionConfig())

        assertTrue(typeInformation.isKeyType)
        assertTrue(
            comparator.compare(
                Optional.of(DataClass("a", 1, Nested("a"))),
                Optional.of(DataClass("b", 1, Nested("a"))),
            ) < 0,
        )
    }
}
