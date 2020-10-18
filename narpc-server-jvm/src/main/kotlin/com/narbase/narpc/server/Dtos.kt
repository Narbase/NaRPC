package com.narbase.narpc.server

import com.google.gson.JsonElement
import java.io.File

@Suppress("unused", "ArrayInDataClass")
data class NarpcClientRequestDto(val functionName: String, val args: Array<Any>)

data class NarpcServerRequestDto(val functionName: String, val args: Array<JsonElement>)

data class NarpcResponseDto(
    val dto: JsonElement?,

    val status: String = "0",

    val message: String? = null
)

