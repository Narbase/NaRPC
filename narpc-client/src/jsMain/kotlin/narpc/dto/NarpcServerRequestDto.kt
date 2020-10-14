package narpc.dto

actual class NarpcServerRequestDto(val functionName: String, val args: Array<JSON>)

actual class NarpcResponseDto(
    val dto: JSON?,

    val status: String = "0",

    val message: String? = null
)