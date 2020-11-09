package e2e

import kotlinx.coroutines.Deferred
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
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
            val format =
                Json {
                    //copy from DefaultJson
                    isLenient = false
                    ignoreUnknownKeys = false
                    allowSpecialFloatingPointValues = true
                    useArrayPolymorphism = false

                    serializersModule = SerializersModule {
                        polymorphic(Animal::class) {
                            subclass(Mammal::class, Mammal.serializer())
                            subclass(Bird::class, Bird.serializer()
                            )
                        }
                    }
                }

            fun functionsReturnsMap(name: String): (it: String) -> Any =
                when (name) {
                    "empty" -> {
                        { Unit }
                    }
                    "hello" -> {
                        {
//                            it
                            Json.decodeFromString(String.serializer(), it)
//                            JSON.parse<String>(it)
                        }
                    }
                    "reverse" -> {
                        { Json.decodeFromString(ListSerializer(SimpleTestItem.serializer()), it) }
                    }
                    "wrappedHello" -> {
                        {
                            val greeting = Json.decodeFromString(Greeting.serializer(), it)
                            greeting
                        }
                    }
                    "sendFile" -> {
                        { Json.decodeFromString(Boolean.serializer(), it) }
                    }
                    "sendFiles" -> {
                        { Json.decodeFromString(Int.serializer(), it) }
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
                        { Json.decodeFromString(ListSerializer(Int.serializer()), it) }
                    }
                    "getFirstEnum" -> {
                        { json ->
                            Json.decodeFromString(TestEnum.serializer(), json)
                        }
                    }
                    "getAnimals" -> {
                        { format.decodeFromString(ListSerializer(PolymorphicSerializer(Animal::class)), it) }
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
        suspend fun reverse(listToBeReversed: List<SimpleTestItem>): Deferred<List<SimpleTestItem>>

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
        fun deferredIntsAsync(start: Int, end: Int): Deferred<List<Int>>

        @JsName("getFirstEnum")
        suspend fun getFirstEnum(): Deferred<TestEnum>

        @JsName("getAnimals")
        suspend fun getAnimals(animals: String): Deferred<List<Animal>>

        @Serializable
        data class Greeting(val greeting: String, val recipientIds: List<Int>)

        @Serializable
        data class SimpleTestItem(val name: String, val numbersList: List<Int>)

        @Serializable
        enum class TestEnum { First, Second, Third }


        @Serializable
        abstract class Animal {
            abstract val name: String
        }

        @Serializable
        @SerialName("mammal")
        class Mammal(override val name: String, val legs: Int) : Animal()

        @Serializable
        @SerialName("bird")
        class Bird(override val name: String, val wings: Int) : Animal()

    }

    fun greetingResponse(greeting: String) = "$greeting to you too"

}


