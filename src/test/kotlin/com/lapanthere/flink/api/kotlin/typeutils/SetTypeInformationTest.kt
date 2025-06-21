package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeutils.TypeInformationTestBase

class SetTypeInformationTest : TypeInformationTestBase<SetTypeInformation<*>>() {
    override fun getTestData(): Array<SetTypeInformation<*>> = arrayOf(
        createTypeInformation<Set<String>>() as SetTypeInformation<*>,
        createTypeInformation<MutableSet<Pair<String, Int>>>() as SetTypeInformation<*>,
        createTypeInformation<HashSet<DataClass>>() as SetTypeInformation<*>,
        createTypeInformation<LinkedHashSet<Int>>() as SetTypeInformation<*>,
    )
}
