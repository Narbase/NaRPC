package com.narbase.narpc.server

import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.Deferred
import narpc.dto.FileContainer
import narpc.exceptions.NarpcException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext


@Suppress("FunctionName")
inline fun <reified C : Any> NarpcServer(service: C) = NarpcServer(service, service::class.java)

class NarpcServer<C>(val service: C, private val serviceClass: Class<out C>) {

    var gson = Gson()

//    val format = Json { this.serializersModule = module }

    //    @OptIn(ExperimentalSerializationApi::class)
    suspend fun process(requestDto: NarpcServerRequestDto): NarpcResponseDto {
        val method = getMethod(requestDto.functionName)
        val typesList = method.parameterTypes
        val args = if (requestDto.args.size > method.parameterCount)
            requestDto.args.sliceArray(0 until method.parameterCount)
        else requestDto.args
        val results = args.mapIndexed { index, any ->
            deserializeArgument(typesList, index, any)
//            gson.fromJson(any, type)
        }.toTypedArray()

        return try {
//            val result = returnType.javaClass.cast(method.invoke(service, *results))
            val nullArraySize = method.parameterCount - results.size

            val result = when {
                nullArraySize > 0 -> method.invoke(service, *results, *arrayOfNulls<Any>(nullArraySize))
                nullArraySize < 0 -> method.invoke(service, *(results.sliceArray(0 until method.parameterCount)))
                else -> method.invoke(service, *results)
            }

            val res = if (result is Deferred<*>) result.await()
            else result

            NarpcResponseDto(res?.let {
                gson.toJson(res)
//                val serializer = serializerForSending(res, module) as KSerializer<Any>
//                format.encodeToJsonElement(serializer(returnType), res)
//                format.encodeToString(serializer, res)
            })
        } catch (e: NarpcException) {
            NarpcResponseDto(null, status = e.status, message = e.message ?: "")
        } catch (t: InvocationTargetException) {
            val targetException = t.targetException
            if (targetException is NarpcException) {
                NarpcResponseDto(null, status = targetException.status, message = targetException.message ?: "")
            } else throw t
        } catch (t: Throwable) {
            t.printStackTrace()
            throw  t
        }

    }

    private fun deserializeArgument(
        typesList: Array<Class<*>>,
        index: Int,
        any: JsonElement
    ): Any? {
        val type = typesList[index]
        return if (type.name == "kotlin.coroutines.Continuation") {
            createContinuation()
        } else {
            try {
                gson.fromJson(any, type)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun createContinuation(): Continuation<Unit> {
        return Continuation<Unit>(object : CoroutineContext {
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
    }

    suspend fun process(files: List<FileDescriptor>, requestDto: NarpcServerRequestDto): NarpcResponseDto {

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
                if (type.name == "kotlin.coroutines.Continuation") {
                    createContinuation()
                } else {
                    if (arg is JsonElement) deserializeArgument(typesList, index, arg)
                    else {
                        gson.fromJson((arg as JsonElement), type)
                    }
                }
            }
        }.toTypedArray()
        println("NarpcServer::process: results are ${results.joinToString()}")
        return try {
            val result = method.invoke(service, *results)
            NarpcResponseDto(result?.let { gson.toJson(result) })
        } catch (e: NarpcException) {
            NarpcResponseDto(null, status = e.status, message = e.message ?: "")
        }

    }

    private fun getMethod(functionName: String): Method {
        val matchingMethods = serviceClass.methods.filter { (it.name == functionName) && it.isSynthetic.not() }
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