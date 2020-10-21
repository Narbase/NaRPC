package narpc.dto

import com.google.gson.JsonElement

actual class NarpcResponseDto(
    val dto: JsonElement?,

    val status: String = "0",

    val message: String = ""
)

actual class NarpcServerRequestDto(val functionName: String, val args: List<JsonElement>)
