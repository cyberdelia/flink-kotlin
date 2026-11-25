package com.lapanthere.flink.api.kotlin.serializers.optional

import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot
import org.apache.flink.api.common.typeutils.base.IntSerializer
import org.apache.flink.api.common.typeutils.base.StringSerializer
import org.apache.flink.core.memory.DataInputDeserializer
import org.apache.flink.core.memory.DataOutputSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

internal class OptionalSerializerSnapshotTest {
    @Test
    fun `snapshot round trip restores the nested serializer`() {
        val serializer = OptionalSerializer(StringSerializer.INSTANCE)
        val output = DataOutputSerializer(128)
        TypeSerializerSnapshot.writeVersionedSnapshot(output, serializer.snapshotConfiguration())

        val restoredSnapshot =
            TypeSerializerSnapshot.readVersionedSnapshot<Optional<String>>(
                DataInputDeserializer(output.copyOfBuffer),
                javaClass.classLoader,
            )

        assertEquals(serializer, restoredSnapshot.restoreSerializer())
        assertTrue(restoredSnapshot is OptionalSerializerSnapshot<*>)
    }

    @Test
    fun `snapshot compatibility follows the element serializer`() {
        val current = OptionalSerializer(StringSerializer.INSTANCE).snapshotConfiguration()
        val same = OptionalSerializer(StringSerializer.INSTANCE).snapshotConfiguration()

        @Suppress("UNCHECKED_CAST")
        val different =
            OptionalSerializer(IntSerializer.INSTANCE).snapshotConfiguration()
                as TypeSerializerSnapshot<Optional<String>>

        assertTrue(current.resolveSchemaCompatibility(same).isCompatibleAsIs)
        assertTrue(current.resolveSchemaCompatibility(different).isIncompatible)
    }
}
