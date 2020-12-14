package com.narbase.narpc.server

import com.google.gson.JsonElement


/*
@Suppress("unused", "ArrayInDataClass")
@Serializable
data class NarpcClientRequestDto(val functionName: String, val args: Array<Any>)
*/

data class NarpcServerRequestDto(val functionName: String, val args: Array<JsonElement>)

data class NarpcResponseDto(
    val dto: String?,

    val status: String = "0",

    val message: String = ""
)

