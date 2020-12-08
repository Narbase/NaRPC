package narpc.client

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import narpc.dto.File
import narpc.dto.FileContainer
import narpc.dto.NarpcClientRequestDto
import narpc.exceptions.ServerException
import narpc.utils.printDebugStackTrace

/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

class NarpcKtorClient {

    val client by lazy {
        HttpClient(Apache) {
            engine {
                connectionRequestTimeout = 0
                connectTimeout = 0
                socketTimeout = 0

            }
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
    }

    companion object {
        private const val defaultHttpErrorMessage = ""
        private const val defaultHttpErrorCode = 500 //Todo : is this a decent default if the response is null?
    }

    suspend fun sendRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        block: NarpcClientRequestBuilder.() -> Unit
    ): String {

        val narpcClientRequestBuilder = NarpcClientRequestBuilder()
        narpcClientRequestBuilder.block()
        val dto = NarpcClientRequestDto(methodName, args)
        println("requestDto = $dto")
        try {
            val response: String = client.post(endpoint) {
                headers {
                    narpcClientRequestBuilder.headers.forEach {
                        append(it.key, it.value)
                    }
                }
                contentType(ContentType("application", "json"))
                body = dto
            }
            println(response)
            return response

        } catch (t: Throwable) {
            t.printDebugStackTrace()
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

    suspend fun sendMultipartRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        block: NarpcClientRequestBuilder.() -> Unit
    ): String {
        val narpcClientRequestBuilder = NarpcClientRequestBuilder()
        narpcClientRequestBuilder.block()

        val dto = NarpcClientRequestDto(
            methodName,
            args.filterNot { it is FileContainer || (it is List<*> && it.isNotEmpty() && it.first() is FileContainer) }
                .toTypedArray())
        try {
            val response: String = client.post(endpoint) {
                headers {
                    narpcClientRequestBuilder.headers.forEach {
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
                        this.append(FormPart("nrpcDto", Gson().toJson(dto)))
                    }
                )
            }
            return response
        } catch (t: Throwable) {
            t.printDebugStackTrace()
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