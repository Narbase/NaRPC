package narpc.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
actual class NarpcServerRequestDto(val functionName: String, val args: Array<JsonElement>)

@Serializable
actual data class NarpcResponseDto(
    val dto: JsonElement?,

    val status: String = "0",

    val message: String = ""
)