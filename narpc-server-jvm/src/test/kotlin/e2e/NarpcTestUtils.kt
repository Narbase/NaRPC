package e2e

import com.narbase.narpc.server.InjectApplicationCall
import io.ktor.application.*
import io.ktor.auth.*
import jvm_library_test.e2e.AuthorizedClientData
import jvm_library_test.e2e.TestServer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import narpc.client.NarpcClient
import narpc.dto.FileContainer
import narpc.exceptions.NarpcBaseExceptionFactory
import narpc.exceptions.NarpcException
import narpc.exceptions.UnknownErrorException
import java.io.File
import kotlin.random.Random

object NarpcTestUtils {
    const val EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS = "EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS"

    class ExceptionMapExampleException(message: String) :
        NarpcException(EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS, message)

    init {
        NarpcClient.exceptionsMap[EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS] =
            NarpcBaseExceptionFactory { message -> ExceptionMapExampleException(message) }
    }

    fun runServer() {
        TestServer.run()
    }


    fun deleteAllTestFiles() {
        val dir = File(receivedFilesDirectory)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }


    interface TestService {
        suspend fun empty()
        suspend fun hello(greeting: String): String
        suspend fun reverse(listToBeReversed: Array<SimpleTestItem>): Array<SimpleTestItem>
        suspend fun wrappedHello(greeting: Greeting): Greeting
        suspend fun sendFile(file: FileContainer): Boolean
        suspend fun sendFiles(files: List<FileContainer>, firstNumber: Int, secondNumber: Int): Int
        suspend fun throwUnknownErrorException()
        suspend fun throwCustomException(exceptionCode: Int)
        suspend fun throwExceptionMapExampleException(message: String)
        fun deferredIntsAsync(start: Int, end: Int): Deferred<Array<Int>>
        suspend fun getFirstEnum(): TestEnum
        suspend fun getAnimals(animals: String): Array<Animal>
        suspend fun getUsername(): String?

        data class Greeting(val greeting: String, val recipientIds: Array<Int>) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Greeting

                if (greeting != other.greeting) return false
                if (!recipientIds.contentEquals(other.recipientIds)) return false

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
                if (javaClass != other?.javaClass) return false

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

        enum class TestEnum{First, Second, Third}

        abstract class Animal {
            abstract val name: String
        }

        class Mammal(override val name: String, val legs: Int) : Animal()

        class Bird(override val name: String, val wings: Int) : Animal()


    }


    private const val receivedFilesDirectory = "nrpcReceivedTestFiles/"

    class RemoteTestService : TestService {
        //        override lateinit var call: ApplicationCall
        @InjectApplicationCall
        lateinit var call: ApplicationCall

        override suspend fun empty() {

        }

        override suspend fun hello(greeting: String): String {
            println("RemoteTestService.hello: greeting = [${greeting}]")
            return greetingResponse(greeting)
        }

        override suspend fun reverse(listToBeReversed: Array<TestService.SimpleTestItem>): Array<TestService.SimpleTestItem> {
            return listToBeReversed.reversed().toTypedArray()
        }

        override suspend fun wrappedHello(greeting: TestService.Greeting): TestService.Greeting {
            return greeting
        }

        override suspend fun sendFile(file: FileContainer): Boolean {
            try {
                val newFile = File("$receivedFilesDirectory${file.file.name}")
                newFile.parentFile.mkdirs()
                newFile.createNewFile()
                newFile.bufferedWriter().use { writer ->
                    file.file.bufferedReader().lines().forEach {
                        writer.write(it)
                        writer.newLine()
                    }
                    writer.flush()
                }
                return true
            } catch (e: Exception) {
                println(e.message)
                println(e.stackTrace)
                return false
            }
        }

        override suspend fun sendFiles(files: List<FileContainer>, firstNumber: Int, secondNumber: Int): Int {
            try {
                files.forEach { file ->
                    val newFile = File("$receivedFilesDirectory${file.file.name}")
                    newFile.parentFile.mkdirs()
                    newFile.createNewFile()
                    newFile.bufferedWriter().use { writer ->
                        file.file.bufferedReader().lines().forEach {
                            writer.write(it)
                            writer.newLine()
                        }
                        writer.flush()
                    }
                }
                return firstNumber - secondNumber
            } catch (e: Exception) {
                println(e.message)
                println(e.stackTrace)
                return Random(134).nextInt()
            }
        }

        override suspend fun throwUnknownErrorException() {
            throw  UnknownErrorException("this method throws UnknownErrorException")
        }

        override suspend fun throwCustomException(exceptionCode: Int) {
            class MyCustomException(message: String) : NarpcException("$exceptionCode", message)
            throw MyCustomException("throwing custom exception")
        }

        override suspend fun throwExceptionMapExampleException(message: String) {
            throw ExceptionMapExampleException(message)
        }

        override fun deferredIntsAsync(start: Int, end: Int): Deferred<Array<Int>> = GlobalScope.async {
            (start..end).toList().toTypedArray()
        }

        override suspend fun getFirstEnum(): TestService.TestEnum {
            return TestService.TestEnum.First
        }

        override suspend fun getAnimals(animals: String): Array<TestService.Animal> {
            return animals.split(",").mapNotNull {
                if (it.first() == 'm'){
                    val legs = it.last().toString().toInt()
                   TestService.Mammal(it.drop(1).dropLast(1), legs)
                }else if (it.first() == 'b'){
                    val wings = it.last().toString().toInt()
                   TestService.Bird(it.drop(1).dropLast(1), wings)
                }else{
                    null
                }
            }.toTypedArray()
        }

        override suspend fun getUsername(): String? {
            return call.principal<AuthorizedClientData>()?.name
        }
    }


    fun greetingResponse(greeting: String) = "$greeting to you too"
}


