package narpc.client

import com.narbase.narnic.narpc.cilent.js.Proxy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import narpc.dto.FileContainer
import utils.json
import kotlin.js.Promise

actual object NarpcClient {
    actual inline fun <reified T : Any> build(endpoint: String, headers: Map<String, String>): T {
        val p = Promise<Int> { resolve, reject ->

        }

        val proxy = Proxy(json { }, json {
            "get" to { target: dynamic, prop: String, receiver: dynamic ->
                console.log("Inside get $prop")
                val pr = Proxy({}, json {

                    val applyFun: (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Unit = { target: dynamic, thisArgs: dynamic, args: Array<Any> ->
                        console.log("Inside applyFun $prop")
//                            makeCall(endpoint, prop, args)

                    }

                    val suspendApplyFun: suspend (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Unit = { target: dynamic, thisArgs: dynamic, args: Array<Any> ->
                        console.log("Inside applyFun ${prop}")
                    }

                    val asyncApplyFun: suspend (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Unit = { target: dynamic, thisArgs: dynamic, args: Array<Any> ->
                        console.log("Inside asyncApplyFun $prop")
//                            makeCall(endpoint, prop, args)
                    }

                    val callAsyncApplyFun: (target: dynamic, thisArgs: dynamic, args: Array<Any>) -> Any = { target, thisArgs, args ->

                        val p = GlobalScope.async {
                            makeCall(endpoint, prop, args, headers)
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


    suspend fun makeCall(endpoint: String, methodName: String, args: Array<Any>, headers: Map<String, String>): Any {
        try {
            val firstArgumentIsFile = args.firstOrNull() is FileContainer
            val firstArgumentIsFileArray = (args.firstOrNull() as? Array<*>)?.firstOrNull() is FileContainer
            val firstArgumentIsFileCollection = (args.firstOrNull() as? Collection<*>)?.firstOrNull() is FileContainer
            val nrpcResponse = if (firstArgumentIsFile || firstArgumentIsFileArray || firstArgumentIsFileCollection) {

                NarpcJsClient.sendMultipartRequest(endpoint, methodName, args, headers)
            } else {
                NarpcJsClient.sendRequest(endpoint, methodName, args, headers)
            }

            val dto = nrpcResponse.dto
            if (dto != null) {
                return dto
            }

        } catch (e: Throwable) {
            throw RuntimeException("unexpected invocation exception: ${e.message}")
        }
        return Unit
    }

}

