package narpc.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement



@Serializable
actual class NarpcResponseDto(
    val dto: JsonElement?,

    val status: String = "0",

    val message: String = ""
)

@Serializable
actual class NarpcClientRequestDto(val functionName: String, val args: Array<JsonElement>)
//actual class NarpcServerRequestDto(val functionName: String, val args: List<JsonElement>)
//