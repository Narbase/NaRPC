package e2e

import kotlinx.coroutines.Deferred
import narpc.client.NarpcClient
import narpc.dto.FileContainer
import narpc.exceptions.NarpcBaseExceptionFactory
import narpc.exceptions.NarpcException

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


        @JsName("getUsername")
        suspend fun getUsername(): Deferred<String?>

        data class Greeting(val greeting: String, val recipientIds: Array<Int>)

        data class SimpleTestItem(val name: String, val numbersList: Array<Int>)

        enum class TestEnum { First, Second, Third }


        abstract class Animal {
            abstract val name: String
        }

        class Mammal(override val name: String, val legs: Int) : Animal()

        class Bird(override val name: String, val wings: Int) : Animal()

    }

    interface TestServiceForServerV2 {
        @JsName("hello")
        suspend fun hello(greeting: String, age: Int?): Deferred<String>

    }

    fun greetingResponse(greeting: String) = "$greeting to you too"

}


