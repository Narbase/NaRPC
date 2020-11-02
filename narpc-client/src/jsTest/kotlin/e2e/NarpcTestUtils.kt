package e2e

import kotlinx.coroutines.Deferred
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import narpc.client.NarpcClient
import narpc.dto.FileContainer
import narpc.exceptions.NarpcException
import narpc.exceptions.NarpcBaseExceptionFactory

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
            @OptIn(InternalSerializationApi::class)
            fun functionsReturnsMap(name: String) =
                when (name) {
                    "empty" -> Unit.serializer()
                    "hello" -> String.serializer()
                    "reverse" -> ListSerializer(SimpleTestItem.serializer())
                    "wrappedHello" -> Greeting.serializer()
                    "sendFile" -> Boolean.serializer()
                    "sendFiles" -> Int.serializer()
                    "throwUnknownErrorException" -> Unit.serializer()
                    "throwCustomException" -> Unit.serializer()
                    "throwExceptionMapExampleException" -> Unit.serializer()
                    "deferredIntsAsync" -> ListSerializer(Int.serializer())
                    "getFirstEnum" -> TestEnum::class.serializer()
                    "getAnimals" -> PolymorphicSerializer(Animal::class)
                    else -> null
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
        data class Greeting(val greeting: String, val recepientIds: List<Int>)

        @Serializable
        data class SimpleTestItem(val name: String, val numbersList: List<Int>)

        @Serializable
        enum class TestEnum { First, Second, Third }

        @Serializable
        abstract class Animal {
            abstract val name: String
        }

        @Serializable
        class Mammal(override val name: String, val legs: Int) : Animal()

        @Serializable
        class Bird(override val name: String, val wings: Int) : Animal()

    }

    fun greetingResponse(greeting: String) = "$greeting to you too"

}


