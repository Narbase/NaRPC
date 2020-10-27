package narpc.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

@Suppress("unused", "ArrayInDataClass")
@Serializable
data class NarpcClientRequestDto(val functionName: String, val args: Array<JsonElement>)


expect class NarpcServerRequestDto

expect class NarpcResponseDto
