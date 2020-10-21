package com.narbase.narnic.main.rpc

import com.narbase.narpc.server.InjectApplicationCall
import com.narbase.narpc.server.NarpcKtorHandler
import com.narbase.narpc.server.NarpcServer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import narpc.dto.FileContainer
import org.slf4j.event.Level
import java.io.File
import kotlin.random.Random

object NrpcTestUtils {
    fun runServer() {
        println("run server called")
        embeddedServer(Netty, port = 8010, module = { testModule() }).apply { start(false) }
        println("run server ended")
    }

    private fun Application.testModule() {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }

        }
        install(CallLogging) {
            level = Level.INFO
        }


        routing {

            post("/test") {
                NarpcKtorHandler(NarpcServer(RemoteTestService())).handle(call)
            }
        }
    }


    fun deleteAllTestFiles() {
        val dir = File(receivedFilesDirectory)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }


    interface TestService {
        suspend fun hello(greeting: String): String
        suspend fun reverse(listToBeReversed: List<SimpleTestItem>): List<SimpleTestItem>
        suspend fun wrappedHello(greeting: Greeting): Greeting
        suspend fun sendFile(file: FileContainer): Boolean
        suspend fun sendFiles(files: List<FileContainer>, firstNumber: Int, secondNumber: Int): Int
        data class Greeting(val greeting: String, val recepientIds: List<Int>)
        data class SimpleTestItem(val name: String, val numbersList: List<Int>)
    }


    private const val receivedFilesDirectory = "nrpcReceivedTestFiles/"

    class RemoteTestService : TestService {
        //        override lateinit var call: ApplicationCall
        @InjectApplicationCall
        lateinit var call: ApplicationCall

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
    }

    fun greetingResponse(greeting: String) = "$greeting to you too"
}
