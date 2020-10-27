package e2e

import com.narbase.narpc.server.InjectApplicationCall
import io.ktor.application.*
import jvm_library_test.e2e.TestServer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import narpc.dto.FileContainer
import narpc.exceptions.NarpcBaseException
import narpc.exceptions.UnknownErrorException
import java.io.File
import kotlin.random.Random

object NarpcTestUtils {
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
        fun deferredIntsAsync(start: Int, end: Int): Deferred<List<Int>>

        @Serializable
        data class Greeting(val greeting: String, val recepientIds: List<Int>)

        @Serializable
        data class SimpleTestItem(val name: String, val numbersList: List<Int>)
    }


    private const val receivedFilesDirectory = "nrpcReceivedTestFiles/"

    class RemoteTestService : TestService {
        //        override lateinit var call: ApplicationCall
        @InjectApplicationCall
        lateinit var call: ApplicationCall

        override suspend fun empty() {

        }

        override suspend fun hello(greeting: String): String {
            println(call)
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
            class MyCustomException(message: String) : NarpcBaseException("$exceptionCode", message)
            throw MyCustomException("throwing custom exception")
        }

        override fun deferredIntsAsync(start: Int, end: Int): Deferred<List<Int>> = GlobalScope.async {
            (start..end).toList()
        }
    }

    fun greetingResponse(greeting: String) = "$greeting to you too"
}


