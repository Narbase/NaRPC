package e2e

import e2e.NarpcTestUtils.TestService.Bird
import e2e.NarpcTestUtils.TestService.Mammal
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import narpc.client.NarpcClient
import narpc.exceptions.NarpcException
import narpc.exceptions.ServerException
import narpc.exceptions.UnknownErrorException
import narpc.utils.nlog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


/***
 * Before running these tests. run the main function in narpc-serer-jvm test Main.kt to allow the testing server to run
 */
internal class NarpcTests {
    val service: NarpcTestUtils.TestService = NarpcClient.build(
        "http://localhost:8010/test",
        headers = mapOf(
            "Authorization" to "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJqd3QtYXVkaWVuY2UiLCJpc3MiOiJodHRwczovL2p3dC1wcm92aWRlci1kb21haW4vIiwibmFtZSI6InRlc3RfdXNlciJ9.K8xaC12VQrg8k3jwDAhMihbc98oPBmqrpEQ0oFSN4Cc"
        )
    ) {
        NarpcTestUtils.TestService.functionsReturnsMap(it)
    }
    val unauthenticatedService: NarpcTestUtils.TestService = NarpcClient.build("http://localhost:8010/test")


    @Test
    fun emptyResponse_shouldBeReceived_whenCallReturnValueIsUnit() = GlobalScope.promise {
//        val response = service.empty().await().decodeNarpcResponse() //this is unstable
//        val json = service.empty().await()
        val response = service.empty().await()
//        log("json is $json\n")
//        val response = json.decodeNarpcResponse() // but this works somehow. TODO: figure out the difference
//        val jsonElement = service.empty().await().asDynamic() as JsonElement
//        val response = Json.decodeFromJsonElement(Unit.serializer(),jsonElement)
        assertTrue {
            response == Unit
        }
    }


    @Test
    fun greetingResponse_shouldBeReceived_whenHelloIsSent() = GlobalScope.promise {
        val greeting = "Hello"
        val response = service.hello(greeting).await()
//        val response = Json.decodeFromJsonElement(String.serializer(), json.asDynamic() as JsonElement)
        val other = NarpcTestUtils.greetingResponse(greeting)
        nlog("\n${response}\n$other\n")
        assertTrue {
            response.equals(other, true)
        }
    }


    @Test
    fun narpc_shouldSupport_sendingAndReceivingComplexItems() = GlobalScope.promise {
        val greetingString = "Hello"
        val recipientIds = arrayOf(1, 3)
        val greeting = NarpcTestUtils.TestService.Greeting(greetingString, recipientIds)
        val response = service.wrappedHello(greeting).await()

        nlog("\n${response}\n$greeting\n")
        assertTrue {
            greeting.greeting == greetingString
            greeting.recipientIds.contentEquals(recipientIds)
        }
    }


    @Test
    fun narpc_shouldSupport_sendingAndReceivingListsOfItems() = GlobalScope.promise {
        val array = arrayOf(
            NarpcTestUtils.TestService.SimpleTestItem("item_1", arrayOf(1, 2)),
            NarpcTestUtils.TestService.SimpleTestItem("item_2", arrayOf(2, 3)),
            NarpcTestUtils.TestService.SimpleTestItem("item_3", arrayOf(3, 4))
        )
        val response = service.reverse(array).await()
        assertTrue {
            response.contentDeepEquals(array.reversed().toTypedArray())
        }
    }

    //        Todo: figure out how to handle creating client side files for testing
/*
    @Test
    fun fileSend_shouldReturnTrue_whenCalledWithAValidFile() = GlobalScope.promise {
//        Todo: figure out how to handle creating client side files for testing
//
*/
/*  val file = Files.createTempFile("testFile", null).toFile()
    file.writeText("test")
        val response = service.sendFile(FileContainer(file))
        assertTrue {
            response
        }
*//*

    }
*/


/*
    @Test
    fun listOfFilesAndNumber_shouldReturnNumber_whenCalled() = GlobalScope.promise {
*/
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
*//*

    }
*/


    @Test
    fun unauthenticatedService_shouldGet401ServerException_whenHelloIsSent() = GlobalScope.promise {
        val greeting = "Hello"
        assertFailsWith(ServerException::class) {
            try {
                unauthenticatedService.hello(greeting).await()
            } catch (e: ServerException) {
                nlog("did catch a  server exception! What is wrong with you js!!")
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
        assertFailsWith(NarpcException::class) {
            val exceptionStatus = 2
            try {
                service.throwCustomException(exceptionStatus).await()
            } catch (e: NarpcException) {
                assertTrue { e.status.toIntOrNull() == exceptionStatus }
                throw e
            }
        }
    }

    @Test
    fun exceptionInExceptionsMap_shouldBeReceived_whenItIsThrownServerSide() = GlobalScope.promise {
        assertFailsWith(NarpcException::class) {
            val exceptionMessage = "agprejgiwogpw"
            try {
                service.throwExceptionMapExampleException(exceptionMessage).await()
            } catch (e: NarpcException) {
                assertTrue { e.status == NarpcTestUtils.EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS }
                assertTrue { e.message == exceptionMessage }
                throw e
            }
        }
    }


    @Test
    fun deferredRPCs_ShouldReturnDeferred_whenCalled() = GlobalScope.promise {
        val results = service.deferredIntsAsync(1, 32).await()
//        val json = service.deferredIntsAsync(1, 32).await() as JsonElement
//        val results = Json.decodeFromJsonElement(ListSerializer(Int.serializer()), json)
        nlog("results: $results are ${results::class}")
        assertTrue {
            results.contentDeepEquals((1..32).toList().toTypedArray())
        }
    }

    @Test
    fun testEnum_ShouldBeReturned_whenGetFirstEnumIsCalled() = GlobalScope.promise {
        val enum = service.getFirstEnum().await()
        assertEquals(enum, NarpcTestUtils.TestService.TestEnum.First)
    }

    @Test
    fun enumTest(){
        val nameTest = NarpcTestUtils.TestService.TestEnum.values().firstOrNull { it.name == "First" }
        val stringTest = NarpcTestUtils.TestService.TestEnum.values().firstOrNull { it.toString() == "First" }
        assertTrue { nameTest == NarpcTestUtils.TestService.TestEnum.First }
        assertTrue { stringTest == NarpcTestUtils.TestService.TestEnum.First }
        val valueOfTest = NarpcTestUtils.TestService.TestEnum.valueOf("First")
        assertTrue { valueOfTest == NarpcTestUtils.TestService.TestEnum.First }
    }

    @Test
    fun subTypes_ShouldBeParsedCorrectly_whenServerReturnsAListOfBaseType() = GlobalScope.promise {
        val animals = service.getAnimals("mLion4,bEagle2,mHuman2").await()
        assertTrue { animals.size == 3 }
        assertTrue { animals.first() is Mammal && (animals.first() as Mammal).legs == 4 }
        assertTrue { animals[1] is Bird && (animals[1] as Bird).wings == 2 }
    }

}

