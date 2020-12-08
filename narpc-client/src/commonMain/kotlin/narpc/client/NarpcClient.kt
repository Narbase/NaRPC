package narpc.client

import narpc.exceptions.NarpcBaseExceptionFactory

expect object NarpcClient {
    inline fun <reified T : Any> build(
        endpoint: kotlin.String,
        noinline block: narpc.client.NarpcClientRequestBuilder.() -> kotlin.Unit = {},
        ): T

    val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory>
}

class NarpcClientRequestBuilder{
    var headers = mutableMapOf<String, String>()
}

fun NarpcClientRequestBuilder.headers(map: Map<String, String>){
    headers = map.toMutableMap()
}
