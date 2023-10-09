package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.operators.Keys.ExpressionKeys
import org.apache.flink.api.common.typeutils.ComparatorTestBase
import org.apache.flink.api.common.typeutils.CompositeType
import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.java.typeutils.TypeExtractor
import java.util.Arrays

internal class DataClassTypeComparatorTest : ComparatorTestBase<DataClass>() {
    private val type = TypeExtractor.getForClass(DataClass::class.java)

    override fun createComparator(ascending: Boolean): TypeComparator<DataClass> {
        require(type is CompositeType<DataClass>)
        val keys = ExpressionKeys(arrayOf("*"), type)
        val orders = BooleanArray(keys.numberOfKeyFields)
        Arrays.fill(orders, ascending)
        return type.createComparator(
            keys.computeLogicalKeyPositions(),
            orders,
            0,
            ExecutionConfig(),
        )
    }

    override fun createSerializer(): TypeSerializer<DataClass> = type.createSerializer(ExecutionConfig())

    override fun getSortedTestData(): Array<DataClass> =
        arrayOf(
            DataClass("abc", 1, Nested("abc")),
            DataClass("def", 2, Nested("def")),
            DataClass("xyz", 3, Nested("xyz")),
        )
}
