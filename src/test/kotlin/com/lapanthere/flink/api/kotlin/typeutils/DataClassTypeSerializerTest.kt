package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.SerializerTestBase
import org.apache.flink.api.common.typeutils.TypeSerializer

internal abstract class AbstractDataClassTypeSerializerTest<T : Any> : SerializerTestBase<T>() {
    protected abstract val typeInformation: TypeInformation<T>

    override fun createSerializer(): TypeSerializer<T> = typeInformation.createSerializer(ExecutionConfig())

    override fun getLength(): Int = -1

    override fun getTypeClass(): Class<T> = typeInformation.typeClass
}

internal class DataClassTypeSerializerTest : AbstractDataClassTypeSerializerTest<DataClass>() {
    override val typeInformation: TypeInformation<DataClass> = createTypeInformation()

    override fun getTestData(): Array<DataClass> =
        arrayOf(
            DataClass("string", 1, Nested("string")),
            DataClass("string", 123, Nested("123")),
        )
}

internal class ParameterizedTypeSerializerTest : AbstractDataClassTypeSerializerTest<ParameterizedClass<Int>>() {
    override val typeInformation: TypeInformation<ParameterizedClass<Int>> = createTypeInformation()

    override fun getTestData(): Array<ParameterizedClass<Int>> =
        arrayOf(
            ParameterizedClass("string", 1),
            ParameterizedClass("string", 4),
        )
}

internal class OrderTypeSerializerTest : AbstractDataClassTypeSerializerTest<Order>() {
    override val typeInformation: TypeInformation<Order> = createTypeInformation()

    override fun getTestData(): Array<Order> =
        arrayOf(
            Order(Purchase(2.0), Purchase(1.0)),
            Order(Purchase(20.0), Purchase(15.0)),
        )
}

internal class PairTypeSerializerTest : AbstractDataClassTypeSerializerTest<Pair<String, Int>>() {
    override val typeInformation: TypeInformation<Pair<String, Int>> = createTypeInformation()

    override fun getTestData(): Array<Pair<String, Int>> =
        arrayOf(
            Pair("Hello", 1),
            Pair("World", 2),
        )
}

internal class TripleTypeSerializerTest : AbstractDataClassTypeSerializerTest<Triple<String, String, Int>>() {
    override val typeInformation: TypeInformation<Triple<String, String, Int>> = createTypeInformation()

    override fun getTestData(): Array<Triple<String, String, Int>> =
        arrayOf(
            Triple("Hello", "World", 1),
            Triple("Super", "Mario", 2),
        )
}
