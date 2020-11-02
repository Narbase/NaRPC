package e2e

import com.narbase.narpc.server.InjectApplicationCall
import io.ktor.application.*
import jvm_library_test.e2e.TestServer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import narpc.client.NarpcClient
import narpc.dto.FileContainer
import narpc.exceptions.NarpcException
import narpc.exceptions.NarpcBaseExceptionFactory
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
        suspend fun reverse(listToBeReversed: List<SimpleTestItem>): List<SimpleTestItem>
        suspend fun wrappedHello(greeting: Greeting): Greeting
        suspend fun sendFile(file: FileContainer): Boolean
        suspend fun sendFiles(files: List<FileContainer>, firstNumber: Int, secondNumber: Int): Int
        suspend fun throwUnknownErrorException()
        suspend fun throwCustomException(exceptionCode: Int)
        suspend fun throwExceptionMapExampleException(message: String)
        fun deferredIntsAsync(start: Int, end: Int): Deferred<List<Int>>
        suspend fun getFirstEnum(): TestEnum
        suspend fun getAnimals(animals: String): List<Animal>

        @Serializable
        data class Greeting(val greeting: String, val recepientIds: List<Int>)

        @Serializable
        data class SimpleTestItem(val name: String, val numbersList: List<Int>)

        enum class TestEnum{First, Second, Third}

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

        override suspend fun reverse(listToBeReversed: List<TestService.SimpleTestItem>): List<TestService.SimpleTestItem> {
            return listToBeReversed.reversed()
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

        override fun deferredIntsAsync(start: Int, end: Int): Deferred<List<Int>> = GlobalScope.async {
            (start..end).toList()
        }

        override suspend fun getFirstEnum(): TestService.TestEnum {
            return TestService.TestEnum.First
        }

        override suspend fun getAnimals(animals: String): List<TestService.Animal> {
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
            }
        }
    }


    fun greetingResponse(greeting: String) = "$greeting to you too"
}


