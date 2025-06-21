package com.lapanthere.flink.api.kotlin.serializers.set

import com.lapanthere.flink.api.kotlin.typeutils.AbstractDataClassTypeSerializerTest
import com.lapanthere.flink.api.kotlin.typeutils.SetTypeInformation
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeinfo.Types
import org.junit.jupiter.api.Assertions.*

internal class SetTypeSerializerTest : AbstractDataClassTypeSerializerTest<Set<String>>() {
    override val typeInformation: TypeInformation<Set<String>> = SetTypeInformation(Types.STRING)

    override fun getTestData(): Array<Set<String>> = arrayOf(
        emptySet(),
        hashSetOf<String>("Hello", "World!"),
        setOf("Hello", "World!"),
        setOf(),
        mutableSetOf(),
        mutableSetOf("Hello", "World!"),
        linkedSetOf("Hello", "World!"),
    )
}