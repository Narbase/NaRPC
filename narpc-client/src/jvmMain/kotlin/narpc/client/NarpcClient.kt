package narpc.client

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import narpc.dto.FileContainer
import narpc.dto.NarpcResponseDto
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

    private suspend fun makeCall(method: Method, service: KClass<T>, args: Array<Any>): Any {
        var result: Any = Unit
        val methodName = method.name
        try {
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

            val gson = Gson()
            val nrpcResponse = gson.fromJson(jsonValue, NarpcResponseDto::class.java)
            val dto = nrpcResponse.dto
            println("before desirialization: dto = $dto")
            if (dto != null) {
                result = gson.fromJson<Any>(dto, myMethod.returnType.javaType)
            }

        } catch (e: Throwable) {
            throw RuntimeException("unexpected invocation exception: ${e.message}")
        }
        return result
    }

}