package narpc.client

import kotlinx.browser.window
import kotlinx.coroutines.await
import narpc.dto.FileContainer
import narpc.dto.NarpcClientRequestDto
import narpc.dto.NarpcResponseDto
import narpc.exceptions.InvalidRequestException
import narpc.exceptions.ServerException
import narpc.exceptions.UnauthenticatedException
import narpc.exceptions.UnknownErrorException
import narpc.utils.nlog
import narpc.utils.printDebugStackTrace
import org.w3c.fetch.RequestInit
import org.w3c.xhr.FormData
import kotlin.coroutines.Continuation
import kotlin.js.json


/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

class NarpcJsClient {

    class EmptyClass

    suspend fun sendRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        block: NarpcClientRequestBuilder.() -> Unit,
    ): NarpcResponseDto {
        val dto = NarpcClientRequestDto(
            methodName,
            args.filterNot { it is FileContainer || (it is List<*> && it.isNotEmpty() && it.first() is FileContainer) }
                .map { if (it is Continuation<*>) EmptyClass() else it }
                .toTypedArray())

        nlog("sendRequest\n")
        nlog("requestArgs: ${args.joinToString { "${it::class.simpleName}" }}\n")
        nlog("getContinuation: ${args.any { it is Continuation<*> }}\n")
        nlog(dto)
        val text = JSON.stringify(dto)
        nlog("serialized NarpcClientRequestDto is $text\n")

        val narpcClientRequestBuilder = NarpcClientRequestBuilder()
        narpcClientRequestBuilder.block()

        try {
            val response: NarpcResponseDto = synchronousPost(
                endpoint,
                headers = narpcClientRequestBuilder.headers,
                body = text
            )
            nlog("\n parsed response is $response\n")
            return response
        } catch (t: Throwable) {
            t.printDebugStackTrace()
            throw t
        }
    }

    suspend fun sendMultipartRequest(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        block: NarpcClientRequestBuilder.() -> Unit
    ): NarpcResponseDto {
        val dto = NarpcClientRequestDto(methodName, args.filterNot {
            it is FileContainer ||
                    (it is Collection<*> && it.isNotEmpty() && it.first() is FileContainer) ||
                    (it is Array<*> && it.isNotEmpty() && it.first() is FileContainer)
        }
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
            body = formData
        )
    }


    private suspend inline fun <reified T : Any> synchronousPost(
        url: String,
        headers: Map<String, String> = mapOf(),
        body: Any? = null
    ) = makeSynchronousPostRequest<T>(
        url, headers, body
    )


    @Suppress("UnnecessaryVariable")
    private suspend inline fun <reified T : Any> makeSynchronousPostRequest(
        url: String,
        headers: Map<String, String>? = null,
        requestBody: Any? = null
    ): T {

        nlog("\nmakeSynchronousPostRequest : requestDto = $requestBody\n")

        val headersPairs = headers?.map { header ->
            header.key to header.value
        }?.toMutableList() ?: mutableListOf()

        try {

            val headersJson = json(*headersPairs.toTypedArray())

            val httpResponse = window.fetch(
                url, RequestInit(
                    method = "POST",
                    headers = headersJson,
                    body = requestBody
                )
            ).await()
            nlog("\n httpResponse: Status: ${httpResponse.status}\n")

            when (httpResponse.status) {
                in 200..299 -> {
                    val json = httpResponse.json().await()
                    nlog("\nhttpResponse: json is $json")
                    nlog(json)
                    return json.unsafeCast<T>()
                }
                401.toShort() -> throw UnauthenticatedException("${httpResponse.status}")
                403.toShort() -> throw UnauthenticatedException("${httpResponse.status}")
                in 400..499 -> throw InvalidRequestException("${httpResponse.status}")
                in 500..599 -> throw ServerException(httpResponse.status.toInt(), "", "")
                else -> throw UnknownErrorException("Status: ${httpResponse.status}")
            }
        } catch (t: Throwable) {
            t.printDebugStackTrace()
            throw t
        }

    }
}