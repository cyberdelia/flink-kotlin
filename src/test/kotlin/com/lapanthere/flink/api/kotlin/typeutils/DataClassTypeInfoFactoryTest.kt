package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.functions.InvalidTypesException
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date

internal class DataClassTypeInfoFactoryTest {
    @Test
    fun `reject non data classes`() {
        assertThrows<InvalidTypesException> { TypeExtractor.createTypeInfo(NotADataClass::class.java) }
    }

    @Test
    fun `allows basic data classes`() {
        val typeInformation = TypeExtractor.createTypeInfo(Basic::class.java)
        assertTrue(typeInformation is DataClassTypeInformation<Basic>)
    }

    @Test
    fun `allows data classes with type parameters`() {
        val typeInformation = TypeExtractor.createTypeInfo(ParameterizedClass::class.java)
        assertTrue(typeInformation is DataClassTypeInformation<ParameterizedClass<*>>)
    }

    @Test
    fun `detects all fields and nested classes`() {
        val typeInformation = TypeExtractor.createTypeInfo(WordCount::class.java)
        assertFalse(typeInformation.isBasicType)
        assertTrue(typeInformation.isTupleType)
        assertEquals(7, typeInformation.totalFields)

        assertTrue(typeInformation is DataClassTypeInformation<WordCount>)
        require(typeInformation is DataClassTypeInformation<WordCount>)

        val fields = arrayOf("count", "word.date", "word.someFloat", "word.collection", "word.nothing")
        val positions = arrayOf(0, 3, 4, 6, 5)
        assertEquals(fields.size, positions.size)
        fields.zip(positions).forEach { (field, position) ->
            val fieldDescriptors = typeInformation.getFlatFields(field)
            assertEquals(1, fieldDescriptors.size)
            assertEquals(position, fieldDescriptors.first().position)
        }

        val fieldDescriptors = typeInformation.getFlatFields("word.*")
        assertEquals(6, fieldDescriptors.size)
        fieldDescriptors.sortBy { it.position }
        assertEquals(Int::class.javaObjectType, fieldDescriptors[0].type.typeClass)
        assertEquals(Int::class.javaObjectType, fieldDescriptors[1].type.typeClass)
        assertEquals(Date::class.java, fieldDescriptors[2].type.typeClass)
        assertEquals(Float::class.javaObjectType, fieldDescriptors[3].type.typeClass)
        assertEquals(Any::class.java, fieldDescriptors[4].type.typeClass)
        assertEquals(List::class.java, fieldDescriptors[5].type.typeClass)

        val nestedTypeInformation = typeInformation.getTypeAt<Word>(1)
        assertEquals(6, nestedTypeInformation.arity)
        assertEquals(6, nestedTypeInformation.totalFields)
        assertTrue(nestedTypeInformation is DataClassTypeInformation<Word>)
    }
}
