package narpc.client

import com.narbase.narnic.narpc.cilent.js.Proxy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import narpc.dto.FileContainer
import narpc.exceptions.*
import utils.json

actual object NarpcClient {
    actual inline fun <reified T : Any> build(
        endpoint: String,
        headers: Map<String, String>,
        crossinline deserializerGetter: (name: String) -> KSerializer<out Any>?
    ): T {

        val proxy = Proxy(json { }, json {
            "get" to { target: dynamic, prop: String, receiver: dynamic ->
                console.log("Inside get $prop\n")
                val pr = Proxy({}, json {

                    val applyFun: (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Unit =
                        { target: dynamic, thisArgs: dynamic, args: Array<Any> ->
                            console.log("Inside applyFun $prop")
//                            makeCall(endpoint, prop, args)

                        }

                    val suspendApplyFun: suspend (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Unit =
                        { target: dynamic, thisArgs: dynamic, args: Array<Any> ->
                            console.log("Inside applyFun ${prop}")
                        }

                    val asyncApplyFun: suspend (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Unit =
                        { target: dynamic, thisArgs: dynamic, args: Array<Any> ->
                            console.log("Inside asyncApplyFun $prop")
//                            makeCall(endpoint, prop, args)
                        }

                    val callAsyncApplyFun: (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Any =
                        { target, thisArgs, args ->

                            val p = GlobalScope.async {
                                val json = makeCall(endpoint, prop, args, headers)
                                val deserializer = deserializerGetter(prop)
                                deserializer?.let {
                                    Json.decodeFromJsonElement(deserializer, json as JsonElement)
                                }?: json
                            }
                            p

                        }

//                   console.log(applyFun)
                    console.log(suspendApplyFun)
//                   console.log(asyncApplyFun)
//                   console.log(callAsyncApplyFun)

                    "apply" to callAsyncApplyFun
                })
                console.log(pr)
                pr
            }
        })



        return proxy.unsafeCast<T>()
    }


    suspend fun makeCall(
        endpoint: String,
        methodName: String,
        args: Array<Any>,
        headers: Map<String, String>
    ): Any {
        console.log("makeCall endpoint = [${endpoint}], methodName = [${methodName}], args = [${args}], headers = [${headers}]\n")
        val firstArgumentIsFile = args.firstOrNull() is FileContainer
        val firstArgumentIsFileArray = (args.firstOrNull() as? Array<*>)?.firstOrNull() is FileContainer
        val firstArgumentIsFileCollection = (args.firstOrNull() as? Collection<*>)?.firstOrNull() is FileContainer
        val nrpcResponse = if (firstArgumentIsFile || firstArgumentIsFileArray || firstArgumentIsFileCollection) {
            NarpcJsClient.sendMultipartRequest(endpoint, methodName, args, headers)
        } else {
            NarpcJsClient.sendRequest(endpoint, methodName, args, headers)
        }

        if (nrpcResponse.status != CommonCodes.BASIC_SUCCESS) {
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
        if (dto != null) {
            return dto
        }
        return Unit
    }

    actual val exceptionsMap: MutableMap<String, NarpcBaseExceptionFactory> = mutableMapOf()

}

