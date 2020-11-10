package narpc.dto

actual class NarpcResponseDto(
    val dto: String?,

    val status: String = "0",

    val message: String = ""
)

actual class NarpcClientRequestDto(val functionName: String, val args: Array<Any>)
//actual class NarpcServerRequestDto(val functionName: String, val args: List<JsonElement>)
//