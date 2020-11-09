package narpc.client

import com.narbase.narnic.narpc.cilent.js.Proxy
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import narpc.dto.FileContainer
import narpc.exceptions.*
import narpc.utils.nlog
import utils.json

actual object NarpcClient {
    @OptIn(InternalAPI::class)
    actual inline fun <reified T : Any> build(
        endpoint: String,
        headers: Map<String, String>,
        crossinline deserializerGetter: (name: String) -> (it: String) -> Any
    ): T {

        val proxy = Proxy(json { }, json {
            "get" to { _: dynamic, prop: String, _: dynamic ->
                nlog("Inside get $prop\n")
                val pr = Proxy({}, json {

                    val callAsyncApplyFun: (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Any =
                        { target, thisArgs, args ->

                            val narpcJsClient = NarpcJsClient()

                            val p = GlobalScope.async {
                                val dto = makeCall(endpoint, prop, args, headers, narpcJsClient)
                                try {
                                    if (dto is Unit)
                                        dto
                                    else {
                                        val deserializer = deserializerGetter(prop)
                                        deserializer(dto as String)
                                    }
//                                    deserializer?.let {
//                                        Json.decodeFromJsonElement(deserializer, json as JsonElement)
//                                    }?: Json.decodeFromJsonElement(json as JsonElement)
                                } catch (e: Exception) {
                                    nlog(e.message)
                                    nlog(e.stackTraceToString())
                                    dto
                                }
                            }
                            p

                        }

                    "apply" to callAsyncApplyFun
                })
                nlog(pr)
                pr
            }
        })



        return proxy.unsafeCast<T>()
    }


    suspend fun makeCall(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        headers: Map<String, String>,
        narpcJsClient: NarpcJsClient
    ): Any {
        nlog("makeCall endpoint = [${endpoint}], methodName = [${methodName}], args = [${args}], headers = [${headers}]\n")
        val firstArgumentIsFile = args.firstOrNull() is FileContainer
        val firstArgumentIsFileArray = (args.firstOrNull() as? Array<*>)?.firstOrNull() is FileContainer
        val firstArgumentIsFileCollection = (args.firstOrNull() as? Collection<*>)?.firstOrNull() is FileContainer
        val nrpcResponse = if (firstArgumentIsFile || firstArgumentIsFileArray || firstArgumentIsFileCollection) {
            narpcJsClient.sendMultipartRequest(endpoint, methodName, args, headers)
        } else {
            narpcJsClient.sendRequest(endpoint, methodName, args, headers)
        }
        nlog("makeCall nrpcResponse is $nrpcResponse\n")
        nlog("makeCall nrpcResponse.status is ${nrpcResponse.status}\n")
        nlog("makeCall nrpcResponse.dto is ${nrpcResponse.dto}\n")

        if (nrpcResponse.status != CommonCodes.BASIC_SUCCESS) {
            nlog("makeCall nrpcResponse ${nrpcResponse.status} status != ${CommonCodes.BASIC_SUCCESS} \n")
            when (nrpcResponse.status) {
                CommonCodes.UNKNOWN_ERROR -> throw UnknownErrorException(nrpcResponse.message)
                CommonCodes.INVALID_REQUEST -> throw InvalidRequestException(nrpcResponse.message)
                CommonCodes.UNAUTHENTICATED -> throw UnauthenticatedException(nrpcResponse.message)
                else -> exceptionsMap[nrpcResponse.status]?.let { exceptionFactory ->
                    throw exceptionFactory.newInstance(nrpcResponse.message)
                } ?: throw NarpcException(nrpcResponse.status, nrpcResponse.message)
            }
        }

        val dto = nrpcResponse.dto
        nlog("received dto is :$dto\n")
        if (dto != null) {
            val json = JSON.stringify(dto)
            nlog("received dto serialized is :$json\n")
            return json
        }
        return Unit
    }

    actual val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory> = mutableMapOf()

}

