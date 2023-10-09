package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class ExampleTest {
    companion object {
        @JvmStatic
        private val cluster =
            MiniClusterWithClientResource(
                MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(2)
                    .setNumberTaskManagers(1)
                    .build(),
            )

        @JvmStatic
        @BeforeAll
        fun beforeAll(): Unit = cluster.before()

        @JvmStatic
        @AfterAll
        fun afterAll(): Unit = cluster.after()
    }

    private val environment =
        StreamExecutionEnvironment.getExecutionEnvironment().apply {
            config.disableGenericTypes()
        }

    @Test
    fun `handles data classes`() {
        val purchases =
            environment
                .fromCollection(listOf(Purchase(2.0), Purchase(1.0)), createTypeInformation())
                .executeAndCollect(10)
        assertEquals(2, purchases.size)
    }

    @Test
    fun `handles list`() {
        val orders =
            environment
                .fromCollection(listOf(Order(Purchase(2.0), Purchase(1.0))), createTypeInformation())
                .executeAndCollect(10)
        assertEquals(1, orders.size)
        assertEquals(2, orders.first().purchases.size)
    }
}
