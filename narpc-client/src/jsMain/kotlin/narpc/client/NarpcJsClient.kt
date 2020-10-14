package narpc.client

import kotlinx.coroutines.await
import narpc.ServerCaller
import narpc.dto.FileContainer
import narpc.dto.NarpcClientRequestDto
import narpc.dto.NarpcResponseDto
import org.w3c.fetch.RequestInit
import org.w3c.xhr.FormData
import kotlin.browser.window
import kotlin.js.json


/*
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2019] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 2020/09/18.
 */

object NarpcJsClient {

    suspend fun sendRequest(endpoint: String, methodName: String, args: Array<Any>): NarpcResponseDto {
        val dto = NarpcClientRequestDto(
            methodName,
            args.filterNot { it is FileContainer || (it is List<*> && it.isNotEmpty() && it.first() is FileContainer) }
                .toTypedArray())

        console.log("sendRequest")
        console.log(dto)
        try {
            return synchronousPost(
                endpoint,
//                    headers = mapOf("Authorization" to "Bearer ${ServerCaller.accessToken}"),
                body = dto
            )
        } catch (t: Throwable) {
            throw t
        }
    }

    suspend fun sendMultipartRequest(endpoint: String, methodName: String, args: Array<Any>): NarpcResponseDto {
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

        return synchronousPost(
            url = endpoint,
//                headers = mapOf("Authorization" to "Bearer ${ServerCaller.accessToken}"),//Todo: why was this commented out?
            body = formData,
            stringify = false,
            setContentType = false
        )
    }


    suspend fun <T : Any> synchronousPost(
        url: String,
        headers: Map<String, String> = mapOf(),
        body: Any? = null,
        stringify: Boolean = true,
        setContentType: Boolean = true
    ) = makeSynchronousRequest<T>(
        "POST", url, headers, body, stringify, setContentType
    )


    @Suppress("UnnecessaryVariable")
    private suspend fun <T : Any> makeSynchronousRequest(
        requestVerb: String,
        url: String,
        headers: Map<String, String>? = null,
        body: Any? = null,
        stringify: Boolean = true,
        setContentType: Boolean = true
    ): T {

        val bodyToSend = body?.let {
            if (stringify) JSON.stringify(it) else it
        }


        val headersPairs = headers?.map { header ->
            header.key to header.value
        }?.toMutableList() ?: mutableListOf()
        if (setContentType) {
            headersPairs.add("Content-Type" to "application/json")
        }
        /*      if (requestVerb == "POST") {
          "Client-Language" to ServerCaller.clientLanguageString()
      }
*/

        val headersJson = json(*headersPairs.toTypedArray())

        val httpResponse = window.fetch(
            "${ServerCaller.BASE_URL}$url", RequestInit(
                method = requestVerb,
                headers = headersJson,
                body = bodyToSend
            )
        ).await()

        return httpResponse.json().await().unsafeCast<T>()
    }


}
