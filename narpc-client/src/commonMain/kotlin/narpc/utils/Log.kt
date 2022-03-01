package narpc.utils

/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/11/09.
 */

object NarpcGlobalConfig {
    const val debugLog = false
}

fun nlog(msg: Any?) {
    if (NarpcGlobalConfig.debugLog) {
        printMessage(msg)
    }
}

expect fun printMessage(msg: Any?)

fun Throwable.printDebugStackTrace(){
    if (NarpcGlobalConfig.debugLog)
        printStackTrace()
}
