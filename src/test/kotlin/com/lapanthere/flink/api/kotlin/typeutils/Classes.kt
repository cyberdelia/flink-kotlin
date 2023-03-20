package com.lapanthere.flink.api.kotlin.typeutils

import org.apache.flink.api.common.typeinfo.TypeInfo
import java.io.Serializable
import java.util.Date

@TypeInfo(DataClassTypeInfoFactory::class)
data class Word(
    val ignoreStaticField: Int = 0,
    @Transient
    private val ignoreTransientField: Int = 0,
    val date: Date,
    val someFloat: Float = 0f,
    var nothing: Any,
    val collection: List<String> = emptyList(),
)

@TypeInfo(DataClassTypeInfoFactory::class)
data class WordCount(val count: Int = 0, val word: Word)

@TypeInfo(DataClassTypeInfoFactory::class)
class NotADataClass

@TypeInfo(DataClassTypeInfoFactory::class)
data class Basic(val abc: String, val field: Int)

data class ParameterizedClass<T>(val name: String, val field: T)

@TypeInfo(DataClassTypeInfoFactory::class)
data class Nested(
    val string: String,
) : Serializable

@TypeInfo(DataClassTypeInfoFactory::class)
data class DataClass(
    val string: String,
    val long: Long,
    val nested: Nested,
) : Serializable

data class Purchase(val amount: Double)

data class Order(val purchases: List<Purchase>) {
    constructor(vararg purchases: Purchase) : this(purchases.toList())
}
