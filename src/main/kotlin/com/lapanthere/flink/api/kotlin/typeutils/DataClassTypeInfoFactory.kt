package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeinfo.TypeInfoFactory
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractionUtils
import java.lang.reflect.Type
import kotlin.reflect.full.starProjectedType

public class DataClassTypeInfoFactory<T : Any> : TypeInfoFactory<T>() {
    @Suppress("UNCHECKED_CAST")
    override fun createTypeInfo(
        t: Type,
        genericParameters: Map<String, TypeInformation<*>>,
    ): TypeInformation<T>? {
        val klass = TypeExtractionUtils.typeToClass<T>(t).kotlin
        if (!klass.isData) {
            return null
        }
        return createTypeInformation(klass.starProjectedType) as TypeInformation<T>
    }
}
