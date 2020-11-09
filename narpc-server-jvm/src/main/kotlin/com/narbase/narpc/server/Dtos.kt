package com.narbase.narpc.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
@Suppress("unused", "ArrayInDataClass")
@Serializable
data class NarpcClientRequestDto(val functionName: String, val args: Array<Any>)
*/

@Serializable
data class NarpcServerRequestDto(val functionName: String, val args: Array<JsonElement>)

@Serializable
data class NarpcResponseDto(
    val dto: String?,

    val status: String = "0",

    val message: String = ""
)

