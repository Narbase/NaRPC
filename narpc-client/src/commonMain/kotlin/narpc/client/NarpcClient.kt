package narpc.client

import kotlinx.serialization.KSerializer
import narpc.exceptions.NarpcBaseExceptionFactory

expect object NarpcClient {
    inline fun <reified T : Any> build(endpoint: String, headers: Map<String, String> = mapOf(), crossinline deserializerGetter: ((name: String) -> KSerializer<out Any>?) = {null}): T

    val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory>
}