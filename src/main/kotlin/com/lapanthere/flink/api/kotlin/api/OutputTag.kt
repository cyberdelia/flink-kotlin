package com.lapanthere.flink.api.kotlin.api

import com.lapanthere.flink.api.kotlin.typeutils.createTypeInformation
import org.apache.flink.util.OutputTag

public inline fun <reified T : Any> OutputTag(id: String): OutputTag<T> =
    OutputTag(id, createTypeInformation())
