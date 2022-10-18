package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.api.java.typeutils.ListTypeInfo
import org.apache.flink.api.java.typeutils.MapTypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TypeInformationTest {
    @Test
    fun `returns Triple type information`() {
        val typeInformation = createTypeInformation<Triple<String, String, Int>>()
        assertTrue(typeInformation is DataClassTypeInformation<Triple<String, String, Int>>)
        assertEquals(Types.STRING, typeInformation.genericParameters["A"])
        assertEquals(Types.STRING, typeInformation.genericParameters["B"])
        assertEquals(Types.INT, typeInformation.genericParameters["C"])
    }

    @Test
    fun `returns Pair type information`() {
        val typeInformation = createTypeInformation<Pair<String, String>>()
        assertTrue(typeInformation is DataClassTypeInformation<Pair<String, String>>)
        assertEquals(Types.STRING, typeInformation.genericParameters["A"])
        assertEquals(Types.STRING, typeInformation.genericParameters["B"])
    }

    @Test
    fun `returns data class type information`() {
        assertTrue(createTypeInformation<DataClass>() is DataClassTypeInformation<DataClass>)
        assertTrue(createTypeInformation<ParameterizedClass<Int>>() is DataClassTypeInformation<ParameterizedClass<Int>>)
    }

    @Test
    fun `returns generic parameters`() {
        val typeInformation = createTypeInformation<ParameterizedClass<Int>>()
        assertEquals(Types.INT, typeInformation.genericParameters["T"])
    }

    @Test
    fun `returns list type information`() {
        val typeInformation = createTypeInformation<List<String>>()
        require(typeInformation is ListTypeInfo<*>)
        assertEquals(Types.STRING, typeInformation.elementTypeInfo)
    }

    @Test
    fun `returns map type information`() {
        val typeInformation = createTypeInformation<Map<String, Int>>()
        require(typeInformation is MapTypeInfo<*, *>)
        assertEquals(Types.STRING, typeInformation.keyTypeInfo)
        assertEquals(Types.INT, typeInformation.valueTypeInfo)
    }
}
