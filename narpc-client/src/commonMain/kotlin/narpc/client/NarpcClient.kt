package narpc.client

import narpc.exceptions.NarpcBaseExceptionFactory

expect object NarpcClient {
    inline fun <reified T : Any> build(
        endpoint: String,
        headers: Map<String, String> = mapOf(),
        crossinline deserializerGetter: ((name: String) -> (it: String) -> Any) = { {it} },
//        clientConfig: Json = KotlinxSerializer.DefaultJson
    ): T

    val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory>
}