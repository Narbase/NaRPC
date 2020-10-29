package e2e

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import narpc.client.NarpcClient
import narpc.client.decodeNarpcResponse
import narpc.exceptions.NarpcBaseException
import narpc.exceptions.ServerException
import narpc.exceptions.UnknownErrorException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Serializable
data class TestResponse(val success: String)

internal class NarpcTests {
    val service: NarpcTestUtils.TestService = NarpcClient.build(
        "http://localhost:8010/test",
        headers = mapOf(
            "Authorization" to "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJqd3QtYXVkaWVuY2UiLCJpc3MiOiJodHRwczovL2p3dC1wcm92aWRlci1kb21haW4vIiwibmFtZSI6InRlc3RfdXNlciJ9.K8xaC12VQrg8k3jwDAhMihbc98oPBmqrpEQ0oFSN4Cc"
        ),
    )
    val unauthenticatedService: NarpcTestUtils.TestService = NarpcClient.build("http://localhost:8010/test")


/*
    @Test
    fun emptyTest() = GlobalScope.promise {
        val client = HttpClient{
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        val response = client.post<TestResponse>("http://localhost:8010/ktor_client_test"){
            contentType(ContentType.Application.Json)
        }
        assertTrue { response.success == "true" }
    }.then({}, {
        it.printStackTrace()
        throw it
    })
*/


    @Test
    fun emptyResponse_shouldBeReceived_whenCallReturnValueIsUnit() = GlobalScope.promise {
//        val response = service.empty().await().decodeNarpcResponse() //this is unstable
        val json = service.empty().await()
        console.log("json is $json\n")
        val response = json.decodeNarpcResponse() // but this works somehow. TODO: figure out the difference
//        val jsonElement = service.empty().await().asDynamic() as JsonElement
//        val response = Json.decodeFromJsonElement(Unit.serializer(),jsonElement)
        assertTrue {
            response == Unit
        }
    }


    @Test
    fun greetingResponse_shouldBeReceived_whenHelloIsSent() = GlobalScope.promise {
        val greeting = "Hello"
        val json = service.hello(greeting).await()
        val response = Json.decodeFromJsonElement(String.serializer(), json.asDynamic() as JsonElement)
        val other = NarpcTestUtils.greetingResponse(greeting)
        console.log("\n${response}\n$other\n")
        assertTrue {
            response.equals(other, true)
        }
    }


    @Test
    fun narpc_shouldSupport_sendingAndReceivingComplexItems() = GlobalScope.promise {
        val greeting = NarpcTestUtils.TestService.Greeting("Hello", listOf(1, 3))
        val response = service.wrappedHello(greeting).await().decodeNarpcResponse()

        console.log("\n${response}\n$greeting\n")
        assertTrue {
            response == greeting
        }
    }


    @Test
    fun narpc_shouldSupport_sendingAndReceivingListsOfItems() = GlobalScope.promise {
        val array = listOf(
            NarpcTestUtils.TestService.SimpleTestItem("item_1", listOf(1, 2)),
            NarpcTestUtils.TestService.SimpleTestItem("item_2", listOf(2, 3)),
            NarpcTestUtils.TestService.SimpleTestItem("item_3", listOf(3, 4))
        )
        val response = service.reverse(array).await().decodeNarpcResponse()
        assertTrue {
            response.equals(array.reversed())
        }
    }

    @Test
    fun fileSend_shouldReturnTrue_whenCalledWithAValidFile() = GlobalScope.promise {
//        Todo: figure out how to handle creating client side files for testing
//
/*  val file = Files.createTempFile("testFile", null).toFile()
    file.writeText("test")
        val response = service.sendFile(FileContainer(file))
        assertTrue {
            response
        }
*/
    }


    @Test
    fun listOfFilesAndNumber_shouldReturnNumber_whenCalled() = GlobalScope.promise {
/*
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
*/
    }


    @Test
    fun unauthenticatedService_shouldGet401ServerException_whenHelloIsSent() = GlobalScope.promise {
        val greeting = "Hello"
        assertFailsWith(ServerException::class) {
            try {
                unauthenticatedService.hello(greeting).await().decodeNarpcResponse()
            } catch (e: ServerException) {
                console.log("did catch a  server exception! What is wrong with you js!!")
                assertTrue { e.httpStatus == 401 }
                throw e
            }
        }
    }

    private interface PointlessInterface {
        @JsName("doSomething")
        fun doSomething(i: Int): Deferred<Unit>
    }

    @Test
    fun unhandledServiceServerSide_shouldGet404ServerException_whenAnyOfItsFunctionsAreCalled() = GlobalScope.promise {
        assertFailsWith(ServerException::class) {
            try {
                val client = NarpcClient.build<PointlessInterface>("http://localhost:8010/nonexisitingpath")
                client.doSomething(2).await()
            } catch (e: ServerException) {
                assertTrue { e.httpStatus == 404 }
                throw e
            }
        }
    }


    @Test
    fun unknownErrorException_shouldBeReceived_whenItIsThrownServerSide() = GlobalScope.promise {
        assertFailsWith(UnknownErrorException::class) {
            service.throwUnknownErrorException().await()
        }
    }


    @Test
    fun theErrorCodeUsedInCustomException_shouldBeReceived_whenItIsThrownServerSide() = GlobalScope.promise {
        assertFailsWith(NarpcBaseException::class) {
            val exceptionStatus = 2
            try {
                service.throwCustomException(exceptionStatus).await()
            } catch (e: NarpcBaseException) {
                assertTrue { e.status.toIntOrNull() == exceptionStatus }
                throw e
            }
        }
    }


    @Test
    fun deferredRPCs_ShouldReturnDeferred_whenCalled() = GlobalScope.promise {
//        val results = service.deferredIntsAsync(1, 32).await().decodeNarpcResponse()
        val json = service.deferredIntsAsync(1, 32).await() as JsonElement
        val results = Json.decodeFromJsonElement(ListSerializer(Int.serializer()), json)
        console.log("results: $results are ${results::class}")
        assertEquals(results, (1..32).toList())
    }

}

