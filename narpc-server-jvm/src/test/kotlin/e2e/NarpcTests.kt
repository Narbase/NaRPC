package e2e

import io.ktor.client.request.*
import jvm_library_test.e2e.getToken
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import narpc.client.NarpcClient
import narpc.client.NarpcKtorClient
import narpc.dto.FileContainer
import narpc.exceptions.NarpcBaseException
import narpc.exceptions.ServerException
import narpc.exceptions.UnknownErrorException
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class NarpcTests {
    val service: NarpcTestUtils.TestService = NarpcClient.build(
        "http://localhost:8010/test",
        headers = mapOf(
            "Authorization" to "Bearer ${getToken("test_user")}"
        )
    )
    val unauthenticatedService: NarpcTestUtils.TestService = NarpcClient.build("http://localhost:8010/test")

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupAll() {
            NarpcTestUtils.runServer()
        }
    }

    @Before
    fun setup() {
        NarpcTestUtils.deleteAllTestFiles()
    }

/*
    @Test
    fun emptyTest() = runBlocking {
        @Serializable
        class TestResponse(val success: String)
        val response = NarpcKtorClient.client.get<TestResponse>("https://reqbin.com/echo/get/json")
        assertTrue { response.success == "true" }
    }
*/


    @Test
    fun emptyResponse_shouldBeReceived_whenCallReturnValueIsUnit() {
        runBlocking {
            val response = service.empty()
            assertTrue {
                response == Unit
            }
        }
    }

    @Test
    fun greetingResponse_shouldBeReceived_whenHelloIsSent() {
        runBlocking {
            val greeting = "Hello"
            val response = service.hello(greeting)
            assertTrue {
                response == NarpcTestUtils.greetingResponse(greeting)
            }
        }
    }

    @Test
    fun narpc_shouldSupport_sendingAndReceivingComplexItems() {
        runBlocking {
            val greeting = NarpcTestUtils.TestService.Greeting("Hello", listOf(1, 3))
            val response = service.wrappedHello(greeting)
            assertTrue {
                response.equals(greeting)
            }
        }
    }

    @Test
    fun narpc_shouldSupport_sendingAndReceivingListsOfItems() {
        runBlocking {
            val array = listOf(
                NarpcTestUtils.TestService.SimpleTestItem("item_1", listOf(1, 2)),
                NarpcTestUtils.TestService.SimpleTestItem("item_2", listOf(2, 3)),
                NarpcTestUtils.TestService.SimpleTestItem("item_3", listOf(3, 4))
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

    @Test
    fun unauthenticatedService_shouldGet401ServerException_whenHelloIsSent() {
        runBlocking {
            val greeting = "Hello"
            assertFailsWith(ServerException::class) {
                try {
                    unauthenticatedService.hello(greeting)
                } catch (e: ServerException) {
                    assertTrue { e.httpStatus == 401 }
                    throw e
                }
            }
        }
    }

    private interface PointlessInterface {
        fun doSomething(i: Int)
    }

    @Test
    fun unhandledServiceServerSide_shouldGet404ServerException_whenAnyOfItsFunctionsAreCalled() {
        runBlocking {
            assertFailsWith(ServerException::class) {
                try {

                    val client = NarpcClient.build<PointlessInterface>("http://localhost:8010/nonexisitingpath",)
                    client.doSomething(2)
                } catch (e: ServerException) {
                    assertTrue { e.httpStatus == 404 }
                    throw e
                }
            }
        }
    }

    @Test
    fun unknownErrorException_shouldBeReceived_whenItIsThrownServerSide() {
        runBlocking {
            assertFailsWith(UnknownErrorException::class) {
                service.throwUnknownErrorException()
            }
        }
    }

    @Test
    fun theErrorCodeUsedInCustomException_shouldBeReceived_whenItIsThrownServerSide() {
        runBlocking {
            assertFailsWith(NarpcBaseException::class) {
                val exceptionStatus = 2
                try {
                    service.throwCustomException(exceptionStatus)
                } catch (e: NarpcBaseException) {
                    assertTrue { e.status.toIntOrNull() == exceptionStatus }
                    throw e
                }
            }
        }
    }

    @Test
    fun deferredRPCs_ShouldReturnDeferred_whenCalled() {
        runBlocking {
            val results = service.deferredIntsAsync(1, 32).await()
            assertEquals(results, (1..32).toList())
        }
    }


}