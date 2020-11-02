package narpc.client

import io.ktor.client.features.json.serializer.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import narpc.exceptions.NarpcBaseExceptionFactory

expect object NarpcClient {
    inline fun <reified T : Any> build(
        endpoint: String,
        headers: Map<String, String> = mapOf(),
        crossinline deserializerGetter: ((name: String) -> KSerializer<out Any>?) = { null },
        clientConfig: Json = KotlinxSerializer.DefaultJson
    ): T

    val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory>
}