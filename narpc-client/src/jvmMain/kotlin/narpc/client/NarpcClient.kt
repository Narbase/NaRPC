package narpc.client

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.fromJson
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
    actual inline fun <reified T : Any> build(endpoint: String, headers: Map<String, String>): T {
        val serviceClass = T::class
        val classLoader = serviceClass.java.classLoader
        val interfaces = arrayOf(serviceClass.java)
        val proxy = NarpcProxyListener(endpoint, serviceClass, headers)
        return Proxy.newProxyInstance(classLoader, interfaces, proxy) as T
    }
}

class NarpcProxyListener<T : Any>(
    private val endpoint: String,
    private val service: KClass<T>,
    val globalHeaders: Map<String, String>
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
        return runBlocking {
            val result = makeCall(method, service, args)
            result
        }

    }

    @OptIn(InternalSerializationApi::class)
    private suspend fun makeCall(method: Method, service: KClass<T>, args: Array<Any>): Any {
        var result: Any = Unit
        val methodName = method.name
        val myMethod = service.members.first { it.name == methodName }//We mustn't repeat methodNames
        val firstArgumentIsFile = method.parameterTypes.contains(FileContainer::class.java)
        val firstArgument = (method.genericParameterTypes.first() as? ParameterizedType)
        val firstArgumentIsFileList =
            firstArgument?.rawType == List::class.java && firstArgument.actualTypeArguments.first() == FileContainer::class.java
        println("method: $methodName, args: ${args.joinToString()}")
        val jsonValue = if (firstArgumentIsFile || firstArgumentIsFileList) {
            NarpcKtorClient.sendMultipartRequest(endpoint, methodName, args, globalHeaders)
        } else {
            NarpcKtorClient.sendRequest(endpoint, methodName, args, globalHeaders)
        }

        val nrpcResponse = Json.decodeFromString<NarpcResponseDto>(jsonValue)
//        val nrpcResponse = gson.fromJson(jsonValue, NarpcResponseDto::class.java)
        if (nrpcResponse.status != CommonCodes.BASIC_SUCCESS){
            when(nrpcResponse.status){
                CommonCodes.UNKNOWN_ERROR-> throw UnknownErrorException(nrpcResponse.message)
                CommonCodes.INVALID_REQUEST-> throw InvalidRequestException(nrpcResponse.message)
                CommonCodes.UNAUTHENTICATED-> throw UnauthenticatedException(nrpcResponse.message)
                else -> throw NarpcBaseException(nrpcResponse.status, nrpcResponse.message)
            }
        }
        val dto = nrpcResponse.dto
        println("before desirialization: dto = $dto")
        if (dto != null) {
//            result = gson.fromJson<Any>(dto, myMethod.returnType.javaType)
            result = Json.decodeFromJsonElement<Any>(dto)
        }
        return result
    }

}