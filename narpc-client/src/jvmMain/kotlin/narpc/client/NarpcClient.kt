package narpc.client

import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import narpc.dto.FileContainer
import narpc.dto.NarpcResponseDto
import narpc.exceptions.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaType

actual object NarpcClient {
    actual inline fun <reified T : Any> build(
        endpoint: String,
        noinline block: NarpcClientRequestBuilder.()-> Unit,
    ): T {
        val serviceClass = T::class
        val classLoader = serviceClass.java.classLoader
        val interfaces = arrayOf(serviceClass.java)
        val narpcKtorClient = NarpcKtorClient()
        val proxy = NarpcProxyListener(endpoint, serviceClass, block, narpcKtorClient)
        return Proxy.newProxyInstance(classLoader, interfaces, proxy) as T
    }

    actual val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory> = mutableMapOf()

}

class NarpcProxyListener<T : Any>(
    private val endpoint: String,
    private val service: KClass<T>,
    val block: NarpcClientRequestBuilder.() -> Unit,
    val narpcKtorClient: NarpcKtorClient
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>): Any {
        return runBlocking {
            val result = makeCall(method, service, args)
            result
        }

    }

    private suspend fun makeCall(method: Method, service: KClass<T>, args: Array<Any?>): Any {
        var result: Any = Unit
        val methodName = method.name
        val myMethod = service.members.first { it.name == methodName }//We mustn't repeat methodNames
        val firstArgumentIsFile = method.parameterTypes.contains(FileContainer::class.java)
        val firstArgument = (method.genericParameterTypes.first() as? ParameterizedType)
        val firstArgumentIsFileList =
            firstArgument?.rawType == List::class.java && firstArgument.actualTypeArguments.first() == FileContainer::class.java
        println("method: $methodName, args: ${args.joinToString()}")
        val jsonValue = if (firstArgumentIsFile || firstArgumentIsFileList) {
            narpcKtorClient.sendMultipartRequest(endpoint, methodName, args, block)
        } else {
            narpcKtorClient.sendRequest(endpoint, methodName, args, block)
        }

        val nrpcResponse = Gson().fromJson<NarpcResponseDto>(jsonValue, NarpcResponseDto::class.java)
//        val nrpcResponse = gson.fromJson(jsonValue, NarpcResponseDto::class.java)
        if (nrpcResponse.status != CommonCodes.BASIC_SUCCESS) {
            when (nrpcResponse.status) {
                CommonCodes.UNKNOWN_ERROR -> throw UnknownErrorException(nrpcResponse.message)
                CommonCodes.INVALID_REQUEST -> throw InvalidRequestException(nrpcResponse.message)
                CommonCodes.UNAUTHENTICATED -> throw UnauthenticatedException(nrpcResponse.message)
                else -> NarpcClient.exceptionsMap[nrpcResponse.status]?.let { exceptionFactory ->
                    throw exceptionFactory.newInstance(nrpcResponse.message)
                } ?: throw NarpcException(nrpcResponse.status, nrpcResponse.message)
            }
        }
        val gson = Gson()
        val dto = nrpcResponse.dto
        println("before desirialization: dto = $dto")
        if (dto != null) {

            result = if ((myMethod.returnType as Any).toString().contains("kotlinx.coroutines.Deferred")) {
                val type = (myMethod.returnType.javaType as ParameterizedType).actualTypeArguments.first()
                gson.fromJson<Any>(dto, type).deferred()
            } else {
                gson.fromJson<Any>(dto, myMethod.returnType.javaType)
            }

        }
        return result
    }

    @Suppress("DeferredIsResult")
    private fun <T> T.deferred(): Deferred<T> = CompletableDeferred(this)
}