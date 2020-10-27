package com.narbase.narpc.server


import io.ktor.application.ApplicationCall
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.ContentType
import io.ktor.http.charset
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.request.*
import io.ktor.response.respond
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.charset.Charset
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * NARBASE TECHNOLOGIES CONFIDENTIAL
 * ______________________________
 * [2017] -[2018] Narbase Technologies
 * All Rights Reserved.
 * Created by islam
 * On: 5/19/17.
 */

class NarpcKtorHandler<C : Any>(private val narpcServer: NarpcServer<C>) {

    suspend fun handle(call: ApplicationCall) {
        injectCall(call)
        if (call.request.isMultipart()) {
            val multipart = call.receiveMultipart()
            val parts = multipart.readAllParts()
            val response = processMultipart(parts)
            call.respond(response)
        } else {
            val requestDto = call.extractDto()
            val dataResponse = narpcServer.process(requestDto)
            call.respond(dataResponse)
        }

    }

    private fun injectCall(call: ApplicationCall) {
        val service = narpcServer.service
        val fields = service::class.memberProperties.filter { field ->
            field is KMutableProperty<*> && field.annotations.any { it is InjectApplicationCall }
        }
        fields.forEach {
            (it as KMutableProperty<*>).setter.call(service, call)
        }
    }

    suspend fun processMultipart(parts: List<PartData>): NarpcResponseDto {
        val narpcDtoJson = parts.first { it is PartData.FormItem && it.name == "nrpcDto" }.let { it as PartData.FormItem }.value
        val requestDto = Json.decodeFromString<NarpcServerRequestDto>(narpcDtoJson)
//        val requestDto = Gson().fromJson(narpcDtoJson, NarpcServerRequestDto::class.java)
        val files = parts.filter { it is PartData.FileItem }.map {
            val item = it as PartData.FileItem
            FileDescriptor(item.originalFileName, item.streamProvider())
        }
        return narpcServer.process(files, requestDto)

    }


    private suspend fun ApplicationCall.extractDto(): NarpcServerRequestDto {
        return try {
            val text = receiveTextWithCorrectEncoding()
            Json.decodeFromString<NarpcServerRequestDto>(text)
//            narpcServer.gson.fromJson(text, NarpcServerRequestDto::class.java)
                    ?: throw GsonParsingContentTransformationException()
        } catch (e: UnsupportedMediaTypeException) {
            Json.decodeFromString<NarpcServerRequestDto>("{}")
//            narpcServer.gson.fromJson("{}", NarpcServerRequestDto::class.java)
        } catch (e: ContentTransformationException) {
            Json.decodeFromString<NarpcServerRequestDto>("{}")
        }
    }


    class GsonParsingContentTransformationException :
            ContentTransformationException("Cannot transform this request's content to the desired type")

    /**
     * https://github.com/ktorio/ktor/issues/384
     * Receive the request as String.
     * If there is no Content-Type in the HTTP header specified use ISO_8859_1 as default charset, see https://www.w3.org/International/articles/http-charset/index#charset.
     * But use UTF-8 as default charset for application/json, see https://tools.ietf.org/html/rfc4627#section-3
     */
    private suspend fun ApplicationCall.receiveTextWithCorrectEncoding(): String {
        fun ContentType.defaultCharset(): Charset = when (this) {
            ContentType.Application.Json -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }

        val contentType = request.contentType()
        val suitableCharset = contentType.charset() ?: contentType.defaultCharset()
        return receiveStream().bufferedReader(charset = suitableCharset).readText()
    }

    open class InvalidRequestException(message: String = "") : Exception(message)

}
