package e2e

import kotlinx.coroutines.Deferred
import narpc.client.NarpcClient
import narpc.dto.FileContainer
import narpc.exceptions.NarpcBaseExceptionFactory
import narpc.exceptions.NarpcException
import narpc.utils.nlog

object NarpcTestUtils {

    const val EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS = "EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS"

    class ExceptionMapExampleException(message: String) :
        NarpcException(EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS, message)

    init {
        NarpcClient.exceptionsMap[EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS] =
            NarpcBaseExceptionFactory { message -> ExceptionMapExampleException(message) }
    }


    interface TestService {
        companion object {
            fun functionsReturnsMap(name: String): (it: String) -> Any =
                when (name) {
                    "empty" -> {
                        { Unit }
                    }
                    "hello" -> {
                        {
//                            it
                            JSON.parse<String>(it)
                        }
                    }
                    "reverse" -> {
                        { JSON.parse<Array<SimpleTestItem>>(it) }
                    }
                    "wrappedHello" -> {
                        {
                            nlog("dto to parse is $it\n")
//                            val s = it.escapeIfNeeded()
//                            log("escaped dto to parse is $s")
                            val greeting = JSON.parse<Greeting>(it)
/*
                            { key, value ->
                                (if (value is Array<*>) {
                                    log("value $value is Array<*>\n")
                                    value.toList()
                                } else {
                                    log("value $value is  ${if (value != null) value::class.simpleName else "null"}\n")
                                    value
                                })
                            }
*/
                            nlog("parsed greeting is ${greeting}\n")
                            nlog("parsed greeting.greeting is ${greeting.greeting}\n")
                            nlog("parsed greeting.recepientIds is ${greeting.recipientIds}\n")
//                            log("parsed greeting.recepientIds toString is ${greeting.recepientIds.joinToString()}")
                            greeting
                        }
                    }
                    "sendFile" -> {
                        { JSON.parse<Boolean>(it) }
                    }
                    "sendFiles" -> {
                        { JSON.parse<Int>(it) }
                    }
                    "throwUnknownErrorException" -> {
                        { Unit }
                    }
                    "throwCustomException" -> {
                        { Unit }
                    }
                    "throwExceptionMapExampleException" -> {
                        { Unit }
                    }
                    "deferredIntsAsync" -> {
                        { JSON.parse<Array<Int>>(it) }
                    }
                    "getFirstEnum" -> {
                        { json ->
                            val processedJson = json.removeSurrounding("\"")
                            nlog("enum json is $json. processed enum json is $processedJson. TestEnum.First.name = ${TestEnum.First.name}. TestEnum.First.toString() = ${TestEnum.First.toString()}\n")
                            TestEnum.values().first { it.name == processedJson }
//                            JSON.parse<TestEnum>(json)

                        }
                    }
                    "getAnimals" -> {
                        { JSON.parse<Array<Animal>>(it) }
                    }
                    else -> {
                        { it }
                    }
                }
        }

        @JsName("empty")
        suspend fun empty(): Deferred<Unit>

        @JsName("hello")
        suspend fun hello(greeting: String): Deferred<String>

        @JsName("reverse")
        suspend fun reverse(listToBeReversed: Array<SimpleTestItem>): Deferred<Array<SimpleTestItem>>

        @JsName("wrappedHello")
        suspend fun wrappedHello(greeting: Greeting): Deferred<Greeting>

        @JsName("sendFile")
        suspend fun sendFile(file: FileContainer): Deferred<Boolean>

        @JsName("sendFiles")
        suspend fun sendFiles(files: List<FileContainer>, firstNumber: Int, secondNumber: Int): Deferred<Int>

        @JsName("throwUnknownErrorException")
        suspend fun throwUnknownErrorException(): Deferred<Unit>

        @JsName("throwCustomException")
        suspend fun throwCustomException(exceptionCode: Int): Deferred<Unit>

        @JsName("throwExceptionMapExampleException")
        suspend fun throwExceptionMapExampleException(message: String): Deferred<Unit>

        @JsName("deferredIntsAsync")
        fun deferredIntsAsync(start: Int, end: Int): Deferred<Array<Int>>

        @JsName("getFirstEnum")
        suspend fun getFirstEnum(): Deferred<TestEnum>

        @JsName("getAnimals")
        suspend fun getAnimals(animals: String): Deferred<Array<Animal>>

        class Greeting(val greeting: String, val recipientIds: Array<Int>) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class.js != other::class.js) return false

                other as Greeting

                if (greeting != other.greeting) return false
                if (!recipientIds.contentDeepEquals(other.recipientIds)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = greeting.hashCode()
                result = 31 * result + recipientIds.contentHashCode()
                return result
            }
        }

        data class SimpleTestItem(val name: String, val numbersList: Array<Int>) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class.js != other::class.js) return false

                other as SimpleTestItem

                if (name != other.name) return false
                if (!numbersList.contentEquals(other.numbersList)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + numbersList.contentHashCode()
                return result
            }
        }


        enum class TestEnum { First, Second, Third }


        abstract class Animal {
            abstract val name: String
        }


        class Mammal(override val name: String, val legs: Int) : Animal()


        class Bird(override val name: String, val wings: Int) : Animal()

    }

    fun greetingResponse(greeting: String) = "$greeting to you too"

}


