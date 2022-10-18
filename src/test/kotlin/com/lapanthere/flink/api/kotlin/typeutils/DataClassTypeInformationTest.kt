package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeutils.TypeInformationTestBase

internal class DataClassTypeInformationTest : TypeInformationTestBase<DataClassTypeInformation<*>>() {
    override fun getTestData(): Array<DataClassTypeInformation<*>> = arrayOf(
        createTypeInformation<DataClass>() as DataClassTypeInformation<DataClass>,
        createTypeInformation<Order>() as DataClassTypeInformation<Order>,
        createTypeInformation<ParameterizedClass<Int>>() as DataClassTypeInformation<ParameterizedClass<Int>>,
        createTypeInformation<Pair<String, Int>>() as DataClassTypeInformation<Pair<String, Int>>,
        createTypeInformation<Triple<String, String, Int>>() as DataClassTypeInformation<Triple<String, String, Int>>
    )
}
