package com.narbase.narpc.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import narpc.dto.FileContainer
import narpc.exceptions.NarpcBaseException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.starProjectedType


@Suppress("FunctionName")
inline fun <reified C : Any> NarpcServer(service: C) = NarpcServer(service, service::class.java)

class NarpcServer<C>(val service: C, private val serviceClass: Class<out C>) {
//    var gson = Gson()

    fun process(requestDto: NarpcServerRequestDto): NarpcResponseDto {
        val method = getMethod(requestDto.functionName)
        val typesList = method.parameterTypes
        val results = requestDto.args.mapIndexed { index, any ->
            val type = typesList[index]
            if (type.name == "kotlin.coroutines.Continuation") {
                Continuation<Unit>(object : CoroutineContext {
                    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
                        TODO("Not yet implemented")
                    }

                    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
                        TODO("Not yet implemented")
                    }

                    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
                        TODO("Not yet implemented")
                    }
                },
                    {})
            } else {
                val serializer = serializer(type)
                Json.decodeFromJsonElement(serializer, any)
            }
//            gson.fromJson(any, type)
        }.toTypedArray()

        return try {
            val result = method.invoke(service, *results)
            NarpcResponseDto(result?.let { Json.encodeToJsonElement(serializer(result::class.starProjectedType), result) })
        } catch (e: NarpcBaseException) {
            NarpcResponseDto(null, status = e.status, message = e.message?:"")
        } catch (t: InvocationTargetException) {
            val targetException = t.targetException
            if (targetException is NarpcBaseException) {
                NarpcResponseDto(null, status = targetException.status, message = targetException.message?:"")
            } else throw t
        }catch (t: Throwable){
            t.printStackTrace()
            throw  t
        }

    }

    fun process(files: List<FileDescriptor>, requestDto: NarpcServerRequestDto): NarpcResponseDto {

        val method = getMethod(requestDto.functionName)
        println("NarpcServer::process: received files are ${files.mapNotNull { it.originalName }.joinToString()}")

        val firstArgument = (method.genericParameterTypes.first() as? ParameterizedType)
        val firstArgumentIsFileList =
            firstArgument?.rawType == List::class.java && firstArgument.actualTypeArguments.first() == FileContainer::class.java

        val args = (if (firstArgumentIsFileList) listOf(files) else files) + requestDto.args
        val results = args.mapIndexed { index, arg ->
            val typesList = method.parameterTypes
            val type = typesList[index]
            if (arg is FileDescriptor) {
                val file = Files.createTempFile(arg.originalName, null).toFile()
                arg.stream.use { input -> file.outputStream().buffered().use { output -> input.copyTo(output) } }
                FileContainer(file)
            } else if (arg is List<*>) {
                if (arg.isNotEmpty() && arg.first() is FileDescriptor) {
                    arg.map { fileItem ->
                        fileItem as FileDescriptor

                        val file = Files.createTempFile(fileItem.originalName, null).toFile()
                        fileItem.stream.use { input ->
                            file.outputStream().buffered().use { output -> input.copyTo(output) }
                        }
                        FileContainer(file)
                    }
                } else {//This case shouldn't happen in the current schema because all the other arguments should be wrapped within a dto
                    arg
                }
            } else {
                val serializer = serializer(type)
                Json.decodeFromJsonElement(serializer, arg as JsonElement)
            }
        }.toTypedArray()
        println("NarpcServer::process: results are ${results.joinToString()}")
        return try {
            val result = method.invoke(service, *results)
            NarpcResponseDto(result?.let { Json.encodeToJsonElement(result) })
        } catch (e: NarpcBaseException) {
            NarpcResponseDto(null, status = e.status, message = e.message?:"")
        }

    }

    private fun getMethod(functionName: String): Method {
        val matchingMethods = serviceClass.methods.filter { it.name == functionName }
        if (matchingMethods.isNullOrEmpty()) {
            val message = "noMatchingMethodError"
            println(message)
            throw NarpcKtorHandler.InvalidRequestException(message)
        } else if (matchingMethods.size > 1) {
            val message = "more than one matching method error"
            println(message)
            throw NarpcKtorHandler.InvalidRequestException(message)
        }
        return matchingMethods.single()
    }

}