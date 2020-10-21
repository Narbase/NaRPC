package com.narbase.narnic.main.rpc

import e2e.NrpcTestUtils
import jvm_library_test.e2e.getToken
import kotlinx.coroutines.runBlocking
import narpc.client.NarpcClient
import narpc.dto.FileContainer
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.assertFails
import kotlin.test.assertTrue

internal class NarpcTests {
    private val service: NrpcTestUtils.TestService = NarpcClient.build(
        "http://localhost:8010/test",
        headers = mapOf(
            "Authorization" to "Bearer ${getToken("test_user")}"
        )
    )
    private val unauthenticatedService: NrpcTestUtils.TestService = NarpcClient.build("http://localhost:8010/test")

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupAll() {
            NrpcTestUtils.runServer()
        }
    }

    @Before
    fun setup() {
        NrpcTestUtils.deleteAllTestFiles()
    }

    @Test
    fun unauthenticatedService_shouldGet_whenHelloIsSent() {
        runBlocking {
            val greeting = "Hello"
            assertFails {
                unauthenticatedService.hello(greeting)
            }
        }
    }

    @Test
    fun greetingResponse_shouldBeReceived_whenHelloIsSent() {
        runBlocking {
            val greeting = "Hello"
            val response = service.hello(greeting)
            assertTrue {
                response == NrpcTestUtils.greetingResponse(greeting)
            }
        }
    }

    @Test
    fun nrpc_shouldSupport_sendingAndReceivingComplexItems() {
        runBlocking {
            val greeting = NrpcTestUtils.TestService.Greeting("Hello", listOf(1, 3))
            val response = service.wrappedHello(greeting)
            assertTrue {
                response.equals(greeting)
            }
        }
    }

    @Test
    fun nrpc_shouldSupport_sendingAndReceivingListsOfItems() {
        runBlocking {
            val array = listOf(
                NrpcTestUtils.TestService.SimpleTestItem("item_1", listOf(1, 2)),
                NrpcTestUtils.TestService.SimpleTestItem("item_2", listOf(2, 3)),
                NrpcTestUtils.TestService.SimpleTestItem("item_3", listOf(3, 4))
            )
            val response = service.reverse(array)
            assertTrue {
                response.equals(array.reversed())
            }
        }
    }

    @Test
    fun fileSend_shouldReturnTrue_whenCalledWithAValidFile() {
        runBlocking {
            val file = Files.createTempFile("testFile", null).toFile()
            file.writeText("test")
            val response = service.sendFile(FileContainer(file))
            assertTrue {
                response
            }
        }
    }

    @Test
    fun listOfFilesAndNumber_shouldReturnNumber_whenCalled() {
        runBlocking {
            val testFiles = (1..5).map { number ->
                val file = Files.createTempFile("testFile_$number", null).toFile()
                file.writeText("test_$number")
                FileContainer(file)
            }
            val random = Random(1242)
            val firstNumber = random.nextInt()
            val secondNumber = random.nextInt()
            val response = service.sendFiles(testFiles, firstNumber, secondNumber)
            assertTrue {
                response == (firstNumber - secondNumber)
            }
        }
    }

}