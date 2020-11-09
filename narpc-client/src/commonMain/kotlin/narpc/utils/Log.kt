package narpc.utils

/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/11/09.
 */

const val IS_DEBUG = false

fun nlog(msg: Any?) {
    if (IS_DEBUG) {
        printMessage(msg)
    }
}

expect fun printMessage(msg: Any?)