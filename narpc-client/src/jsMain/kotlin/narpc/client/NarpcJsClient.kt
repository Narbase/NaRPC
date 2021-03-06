package narpc.client

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.http.*
import narpc.dto.FileContainer
import narpc.dto.NarpcClientRequestDto
import narpc.dto.NarpcResponseDto
import narpc.exceptions.ServerException
import narpc.utils.nlog
import narpc.utils.printDebugStackTrace
import org.w3c.xhr.FormData
import kotlin.coroutines.Continuation


/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

class NarpcJsClient {

    val client = HttpClient {}

    class EmptyClass

    suspend fun sendRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        block: NarpcClientRequestBuilder.()-> Unit,
    ): NarpcResponseDto {
        val dto = NarpcClientRequestDto(
            methodName,
            args.filterNot { it is FileContainer || (it is List<*> && it.isNotEmpty() && it.first() is FileContainer) }
                .map { if (it is Continuation<*>) EmptyClass() else it }
//                .map {
//                    serializeArgument(it)
//                }
                .toTypedArray())

        nlog("sendRequest\n")
        nlog("requestArgs: ${args.joinToString { "${it::class.simpleName}" }}\n")
        nlog("getContinuation: ${args.any { it is Continuation<*> }}\n")
        nlog(dto)
        val text = JSON.stringify(dto)
        nlog("serialized NarpcClientRequestDto is $text\n")
        val body = TextContent(text, ContentType.Application.Json)
        nlog(body)
        val narpcClientRequestBuilder = NarpcClientRequestBuilder()
        narpcClientRequestBuilder.block()

        try {
            val json: String = synchronousPost(
                endpoint,
                headers = narpcClientRequestBuilder.headers,
//                    headers = mapOf("Authorization" to "Bearer ${ServerCaller.accessToken}"),
                body = body
            )

            nlog(json)
            nlog("\n received json as ${json}\n")
            StringBuilder(json)
//            val o = json.quoteString()
//            val o = json.escapeIfNeeded()
//            nlog(o)
//            nlog("\n processed json as $o\n")
            val response: NarpcResponseDto = JSON.parse(json)
            nlog("\n parsed response is $response\n")
            return response
        } catch (t: Throwable) {
            t.printDebugStackTrace()
            throw t
        }
    }

    /*
    private fun serializeArgument(arg: Any): JsonElement {
        return try {
            log("trying to serialize arg $arg \n")
            if (arg is Continuation<*>) {
                log("\narg is Continuation<*> $arg \n")
                Json.encodeToJsonElement("{}")
            } else {
                val serializer = buildSerializer(arg, SerializersModule { }) // reflection call to get real KClass
                Json.encodeToJsonElement(serializer, arg)
            }

        } catch (e: SerializationException) {
            Json.encodeToJsonElement("{}")
        } catch (e: Throwable) {
            e.printDebugStackTrace()
            Json.encodeToJsonElement("{}}")
        }
    }
*/

    suspend fun sendMultipartRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        block: NarpcClientRequestBuilder.()-> Unit
    ): NarpcResponseDto {
        val dto = NarpcClientRequestDto(methodName, args.filterNot {
            it is FileContainer ||
                    (it is Collection<*> && it.isNotEmpty() && it.first() is FileContainer) ||
                    (it is Array<*> && it.isNotEmpty() && it.first() is FileContainer)
        }
//            .map { serializeArgument(it) }
            .toTypedArray())

        val formData = FormData()

        args.forEach { arg ->
            if (arg is FileContainer) {
                formData.append(arg.file.name, arg.file)
            }
            (arg as? Collection<*>)?.forEach {
                if (it is FileContainer) formData.append(it.file.name, it.file)
            }
            (arg as? Array<*>)?.forEach {
                if (it is FileContainer) formData.append(it.file.name, it.file)
            }
        }
        formData.append("nrpcDto", JSON.stringify(dto))
        val narpcClientRequestBuilder = NarpcClientRequestBuilder()
        narpcClientRequestBuilder.block()

        return synchronousPost(
            url = endpoint,
            headers = narpcClientRequestBuilder.headers,
//                headers = mapOf("Authorization" to "Bearer ${ServerCaller.accessToken}"),//Todo: why was this commented out?
            body = formData,
            setContentType = false
        )
    }


    private suspend inline fun <reified T : Any> synchronousPost(
        url: String,
        headers: Map<String, String> = mapOf(),
        body: Any? = null,
        setContentType: Boolean = true
    ) = makeSynchronousPostRequest<T>(
        url, headers, body, setContentType
    )


    @Suppress("UnnecessaryVariable")
    private suspend inline fun <reified T : Any> makeSynchronousPostRequest(
        url: String,
        headers: Map<String, String>? = null,
        requestBody: Any? = null,
        setContentType: Boolean = true
    ): T {

        nlog("\nmakeSynchronousPostRequest : requestDto = $requestBody\n")


        val headersPairs = headers?.map { header ->
            header.key to header.value
        }?.toMutableList() ?: mutableListOf()
/*
        if (setContentType) {
            headersPairs.add("Content-Type" to "application/json")
        }
*/
        /*      if (requestVerb == "POST") {
          "Client-Language" to ServerCaller.clientLanguageString()
      }
*/

        try {

            return client.post(url) {
                headers {
                    headersPairs.forEach {
                        append(it.first, it.second)
                    }
                }
//            contentType(ContentType("application", "json"))
                requestBody?.let {
                    body = requestBody
                }

            }
        } catch (t: Throwable) {
            t.printDebugStackTrace()
            if (t is ResponseException) {
                nlog("caught a response exception\n")
                throw ServerException(
                    t.response?.status?.value ?: defaultHttpErrorCode,
                    t.response?.status?.description ?: defaultHttpErrorMessage,
                    t.message ?: ""
                )
            } else {
                throw t
            }
        }

    }


}


private const val defaultHttpErrorCode = 500 //Todo : is this a decent default if the response is null?
private const val defaultHttpErrorMessage = ""


/***
 * The following few functions are ripped of ktor's serialization
 */

/*
@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
fun buildSerializer(value: Any, module: SerializersModule): KSerializer<Any> {
    log("buildSerializer called\n")
    return when (value) {
        is JsonElement -> {
            log("buildSerializer called on value $value which is a JsonElement\n")
            JsonElement.serializer()
        }
        is List<*> -> {
            log("buildSerializer called on value $value which is a List\n")
            ListSerializer(value.elementSerializer(module))
        }
        is Array<*> -> {
            log("buildSerializer called on value $value which is an Array\n")
            value.firstOrNull()?.let { buildSerializer(it, module) } ?: ListSerializer(String.serializer())
        }
        is Set<*> -> {
            log("buildSerializer called on value $value which is a Set\n")
            SetSerializer(value.elementSerializer(module))
        }
        is Map<*, *> -> {
            log("buildSerializer called on value $value which is a Map\n")
            val keySerializer = value.keys.elementSerializer(module)
            val valueSerializer = value.values.elementSerializer(module)
            MapSerializer(keySerializer, valueSerializer)
        }
        else -> {
            log("buildSerializer called on value $value which is else\n")
            module.getContextual(value::class) ?: value::class.serializer()
        }
    } as KSerializer<Any>
}

@OptIn(ExperimentalSerializationApi::class)
@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
private fun Collection<*>.elementSerializer(module: SerializersModule): KSerializer<*> {
    val serializers: List<KSerializer<*>> =
        filterNotNull().map { buildSerializer(it, module) }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        error(
            "Serializing collections of different element types is not yet supported. " +
                    "Selected serializers: ${serializers.map { it.descriptor.serialName }}"
        )
    }

    val selected = serializers.singleOrNull() ?: String.serializer()

    if (selected.descriptor.isNullable) {
        return selected
    }

    @Suppress("UNCHECKED_CAST")
    selected as KSerializer<Any>

    if (any { it == null }) {
        return selected.nullable
    }

    return selected
}
*/

/*
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> T.decodeNarpcResponse(): T =
    if (this is Unit) {
        log("decoding a Unit\n")
        Unit as T
    } else {
//        log("are these logs appearing?\n")
//        this.asDynamic() as JsonElement
        val deserializer = buildSerializer(this, SerializersModule { })
        log("decoding a non Unit.. ${this::class} to be exact\n")
        log("deserializer ${deserializer.descriptor}\n")
        val element = this.asDynamic() as JsonElement
        log("element $element is ${element::class}\n")
        val decoded = Json.decodeFromJsonElement(deserializer, element)
        log("decoded is $decoded which is a ${decoded::class}\n")
        decoded as T
    }
*/




fun String.quoteString() = buildString {
    append("\"")
    append(this@quoteString)
    append("\"")
}
