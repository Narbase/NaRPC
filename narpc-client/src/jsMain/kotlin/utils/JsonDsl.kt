package utils

import narpc.utils.nlog
import kotlin.js.Json
import kotlin.js.json

/**
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/03/19.
 */

fun createJson2() {

    val js = json {
        "key1" to "value"
        "key2" to "value"
        "key3" to {
            "key4" to 4
            "key5" to arrayOf(1, 2, 3, 4)
        }
    }
    nlog(js)
}

private fun populateChartData() {
    val xAxisData = arrayOf<Int>()
    val seriesData = arrayOf<Int>()

    val option = json(
            "tooltip" to json(
                    "trigger" to "axis"
            ),
            "xAxis" to json(
                    "data" to xAxisData
            ),
            "yAxis" to json(),
            "series" to arrayOf(
                    json(
                            "type" to "type",
                            "color" to "#ee3d48",
                            "data" to seriesData
                    )

            )
    )
}

fun createJson() {
    val xAxisData = arrayOf<Int>()
    val seriesData = arrayOf<Int>()

    val option = json {
        "tooltip" to {
            "trigger" to "axis"
        }
        "xAxis" to {
            "data" to xAxisData
        }
        "yAxis" to {}
        "series" to arrayOf(
                json {
                    "type" to "type"
                    "color" to "#ee3d48"
                    "data" to seriesData
                }
        )
        "series" to jsonArray(
            {
                "1" to 1
                "2" to 2
                "3" to 3
            },
            {
                "just_empty" to 0
            }
        )
    }
    nlog(option)
    nlog(JSON.stringify(option))
}


fun json(block: JsonBuilder.() -> Unit): Json {
    val res: dynamic = js("({})")
    JsonBuilder(res).block()
    return res

}

class JsonBuilder(private val jsonObject: dynamic) {
    infix fun String.to(value: Any?) {
        jsonObject[this] = value
    }

    infix fun String.to(block: JsonBuilder.() -> Unit) {
        jsonObject[this] = json(block)
    }

    fun jsonArray(vararg items: JsonBuilder.() -> Unit): Array<Json> {
        return items.map { json(it) }.toTypedArray()
    }
}
