package narpc.dto

import kotlin.js.Json

//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.JsonElement

//@Serializable
//actual class NarpcServerRequestDto(val functionName: String, val args: Array<JSON>)

actual class NarpcClientRequestDto(val functionName: String, val args: Array<Any>)
//@Serializable
actual data class NarpcResponseDto(
    val dto: String?,

    val status: String = "0",

    val message: String = ""
)