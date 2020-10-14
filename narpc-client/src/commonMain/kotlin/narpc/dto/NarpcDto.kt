package narpc.dto


/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

@Suppress("unused", "ArrayInDataClass")
data class NarpcClientRequestDto(val functionName: String, val args: Array<Any>)


expect class NarpcServerRequestDto

expect class NarpcResponseDto
