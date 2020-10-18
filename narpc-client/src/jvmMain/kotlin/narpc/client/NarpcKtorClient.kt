package narpc.client

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.*
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import narpc.dto.File
import narpc.dto.FileContainer
import narpc.dto.NarpcClientRequestDto

/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

object NarpcKtorClient {

    private val client by lazy {
        HttpClient(Apache) {
/*
            engine {
                connectionRequestTimeout = 0
                connectTimeout = 0
                socketTimeout = 0

            }
*/
            install(JsonFeature) {
                serializer = GsonSerializer {
                    serializeNulls()
                    disableHtmlEscaping()
                }
            }
        }
    }

    suspend fun sendRequest(endpoint: String, methodName: String, args: Array<Any>, globalHeaders: Map<String, String>): String {

        val dto = NarpcClientRequestDto(methodName, args)
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
            throw t
        }
    }

    suspend fun sendMultipartRequest(endpoint: String, methodName: String, args: Array<Any>, globalHeaders: Map<String, String>): String {
        val dto = NarpcClientRequestDto(methodName, args.filterNot { it is FileContainer || (it is List<*> && it.isNotEmpty() && it.first() is FileContainer) }
                .toTypedArray())
        return client.post(endpoint) {
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
                        this.append(FormPart("nrpcDto", Gson().toJson(dto)))
                    }
            )
        }
    }

    private fun FormBuilder.appendFile(it: File) {
        appendInput(
                it.name,
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition,
                            "filename=${it.name}")
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