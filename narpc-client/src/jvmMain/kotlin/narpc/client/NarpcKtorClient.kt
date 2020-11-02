package narpc.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.*
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import narpc.dto.File
import narpc.dto.FileContainer
import narpc.dto.NarpcClientRequestDto
import narpc.exceptions.ServerException
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

@Serializable
class ContinuationClass()
class NarpcKtorClient(val clientConfig: Json = KotlinxSerializer.DefaultJson) {

    val client by lazy {
        HttpClient(Apache) {
            engine {
                connectionRequestTimeout = 0
                connectTimeout = 0
                socketTimeout = 0

            }
            install(JsonFeature) {
                serializer = KotlinxSerializer(clientConfig)
            }
        }
    }

    companion object {
        private const val defaultHttpErrorMessage = ""
        private const val defaultHttpErrorCode = 500 //Todo : is this a decent default if the response is null?
    }

    @InternalSerializationApi
    suspend fun sendRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        globalHeaders: Map<String, String>
    ): String {

        val argsList = args.toList()
//        val serial = serializerForSending(argsList, SerializersModule {  })

//        ListSerializer(argsList.elementSerializer(SerializersModule {  }))
        val dto = NarpcClientRequestDto(
            methodName,
            argsList.map {
//            val serial = it::class.serializer() // reflection call to get real KClass
                serializeArgument(it)
            }.toTypedArray()

//            (Json.encodeToJsonElement(serial, argsList) as JsonArray).toTypedArray()
        )
        println("requestDto = $dto")
        try {
            val response: String = client.post(endpoint) {
                headers {
                    globalHeaders.forEach {
                        append(it.key, it.value)
                    }
                }
                contentType(ContentType("application", "json"))
                body = dto
            }
            println(response)
            return response

        } catch (t: Throwable) {
            t.printStackTrace()
            if (t is ResponseException) {
                throw ServerException(
                    t.response?.status?.value ?: defaultHttpErrorCode,
                    t.response?.status?.description ?: defaultHttpErrorMessage,
                    t.localizedMessage
                )
            } else {
                throw t
            }
        }
    }

    private fun serializeArgument(arg: Any): JsonElement {
        return try {
            //                    val serial = serializer(it::class.starProjectedType) // reflection call to get real KClass
            val serializer = serializerForSending(
                arg,
                clientConfig.serializersModule
            ) as KSerializer<Any> // reflection call to get real KClass
            Json.encodeToJsonElement(serializer, arg)

        } catch (e: UnsupportedOperationException) {
            if (arg is Continuation<*>) {
                //                        val serial = serializer<Continuation<Unit>>()

                Json.encodeToJsonElement(ContinuationClass.serializer(), ContinuationClass())
            } else {
                val serial = serializer(arg::class.java)
                Json.encodeToJsonElement(serial, arg)
            }
        } catch (e: SerializationException) {
            if (arg is Continuation<*>) {
                //                        val serial = serializer<Continuation<Unit>>()
                Json.encodeToJsonElement(ContinuationClass.serializer(), ContinuationClass())
            } else {
                val serial = serializer(arg::class.java)
                Json.encodeToJsonElement(serial, arg)
            }
        }
    }


    @InternalSerializationApi
    suspend fun sendMultipartRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        globalHeaders: Map<String, String>
    ): String {
        val dto = NarpcClientRequestDto(
            methodName,
            args.filterNot { it is FileContainer || (it is List<*> && it.isNotEmpty() && it.first() is FileContainer) }
                .map {
                    serializeArgument(it)
                }
                .toTypedArray()
        )
        try {
            val response: String = client.post(endpoint) {
                headers {
                    globalHeaders.forEach {
                        append(it.key, it.value)
                    }
                    //                append("Authorization", "XXXX")
                    append("Accept", ContentType.Application.Json)
                }
                body = MultiPartFormDataContent(
                    formData {
                        args.forEach { arg ->
                            if (arg is FileContainer) {
                                appendFile(arg.file)
                            }
                            if (arg is List<*>) {
                                arg.forEach {
                                    if (it is FileContainer) {
                                        appendFile(it.file)
                                    }
                                }
                            }
                        }
                        this.append(FormPart("nrpcDto", Json.encodeToString(dto)))
                    }
                )
            }
            return response
        } catch (t: Throwable) {
            t.printStackTrace()
            if (t is ResponseException) {
                throw ServerException(
                    t.response?.status?.value ?: defaultHttpErrorCode,
                    t.response?.status?.description ?: defaultHttpErrorMessage,
                    t.localizedMessage
                )
            } else {
                throw t
            }

        }
    }

    private fun FormBuilder.appendFile(it: File) {
        appendInput(
            it.name,
            headers = Headers.build {
                append(
                    HttpHeaders.ContentDisposition,
                    "filename=${it.name}"
                )
            },
            size = it.length()
        ) {
            buildPacket { writeFully(it.readBytes()) }
        }
    }

/*
    private fun headersBuilder(name: String): Headers {
        return HeadersBuilder().apply {
            append(HttpHeaders.ContentDisposition, "form-data; name=$name")
        }.build()
    }
*/

}


/***
 * The following few functions are ripped of ktor's serialization
 */

//public fun serializerForSending(value: Any): KSerializer<Any> = serializerForSending(value, SerializersModule { }) as KSerializer<Any>

@OptIn(InternalSerializationApi::class)
public fun serializerForSending(value: Any, module: SerializersModule): KSerializer<*> = when (value) {
    is JsonElement -> JsonElement.serializer()
    is List<*> -> ListSerializer(value.elementSerializer(module))
    is Set<*> -> SetSerializer(value.elementSerializer(module))
    is Map<*, *> -> MapSerializer(value.keys.elementSerializer(module), value.values.elementSerializer(module))
    is Map.Entry<*, *> -> MapEntrySerializer(
        serializerForSending(value.key ?: error("Map.Entry(null, ...) is not supported"), module),
        serializerForSending(value.value ?: error("Map.Entry(..., null) is not supported)"), module)
    )
    is Array<*> -> {
        val componentType = value.javaClass.componentType.kotlin.starProjectedType
        val componentClass =
            componentType.classifier as? KClass<*> ?: error("Unsupported component type $componentType")

        @Suppress("UNCHECKED_CAST")
        (ArraySerializer(
            componentClass as KClass<Any>,
            serializerByTypeInfo(componentType) as KSerializer<Any>
        ))
    }
    else -> module.getContextual(value::class) ?: value::class.serializer()
}

@OptIn(ExperimentalStdlibApi::class)
internal fun serializerByTypeInfo(type: KType): KSerializer<*> {
    val classifierClass = type.classifier as? KClass<*>
    if (classifierClass != null && classifierClass.java.isArray) {
        return arraySerializer(type)
    }

    return serializer(type)
}

// NOTE: this should be removed once kotlinx.serialization serializer get support of arrays that is blocked by KT-32839
private fun arraySerializer(type: KType): KSerializer<*> {
    val elementType = type.arguments[0].type ?: error("Array<*> is not supported")
    val elementSerializer = serializerByTypeInfo(elementType)


    @Suppress("UNCHECKED_CAST")
    return ArraySerializer(
        elementType.jvmErasure as KClass<Any>,
        elementSerializer as KSerializer<Any>
    )
}


@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
private fun Collection<*>.elementSerializer(module: SerializersModule): KSerializer<*> {
    val serializers = mapNotNull { value ->
        value?.let { serializerForSending(it, module) }
    }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        val message = "Serializing collections of different element types is not yet supported. " +
                "Selected serializers: ${serializers.map { it.descriptor.serialName }}"
        error(message)
    }

    val selected: KSerializer<*> = serializers.singleOrNull() ?: String.serializer()
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

