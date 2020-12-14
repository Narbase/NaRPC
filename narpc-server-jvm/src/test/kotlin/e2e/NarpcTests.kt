package e2e

import e2e.NarpcTestUtils.TestService.Bird
import e2e.NarpcTestUtils.TestService.Mammal
import jvm_library_test.e2e.getToken
import kotlinx.coroutines.runBlocking
import narpc.client.NarpcClient
import narpc.client.headers
import narpc.dto.FileContainer
import narpc.exceptions.NarpcException
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
    private var username = "test_user"

    val service: NarpcTestUtils.TestService = NarpcClient.build(
        "http://localhost:8010/test",
    ) {
        headers(
            mapOf(
                "Authorization" to "Bearer ${getToken(username)}"
            )
        )

    }
    val unauthenticatedService: NarpcTestUtils.TestService = NarpcClient.build("http://localhost:8010/test")

    val DIVISION_BY_ZERO_STATUS = "DIVISION_BY_ZERO_STATUS"


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
            val greeting = NarpcTestUtils.TestService.Greeting("Hello", arrayOf(1, 3))
            val response = service.wrappedHello(greeting)
            assertTrue {
                response.equals(greeting)
            }
        }
    }

    @Test
    fun narpc_shouldSupport_sendingAndReceivingListsOfItems() {
        runBlocking {
            val array = arrayOf(
                NarpcTestUtils.TestService.SimpleTestItem("item_1", arrayOf(1, 2)),
                NarpcTestUtils.TestService.SimpleTestItem("item_2", arrayOf(2, 3)),
                NarpcTestUtils.TestService.SimpleTestItem("item_3", arrayOf(3, 4))
            )
            val response = service.reverse(array)
            assertTrue {
                response.contentDeepEquals(array.reversed().toTypedArray())
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

                    val client = NarpcClient.build<PointlessInterface>("http://localhost:8010/nonexisitingpath")
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
            assertFailsWith(NarpcException::class) {
                val exceptionStatus = 2
                try {
                    service.throwCustomException(exceptionStatus)
                } catch (e: NarpcException) {
                    assertTrue { e.status.toIntOrNull() == exceptionStatus }
                    throw e
                }
            }
        }
    }

    @Test
    fun exceptionInExceptionsMap_shouldBeReceived_whenItIsThrownServerSide() {
        runBlocking {
            assertFailsWith(NarpcException::class) {
                val exceptionMessage = "agprejgiwogpw"
                try {
                    service.throwExceptionMapExampleException(exceptionMessage)
                } catch (e: NarpcException) {
                    assertTrue { e.status == NarpcTestUtils.EXCEPTION_MAP_EXAMPLE_EXCEPTION_STATUS }
                    assertTrue { e.message == exceptionMessage }
                    throw e
                }
            }
        }
    }

    @Test
    fun deferredRPCs_ShouldReturnDeferred_whenCalled() {
        runBlocking {
            val results = service.deferredIntsAsync(1, 32).await()
            assertTrue {
                results.contentDeepEquals((1..32).toList().toTypedArray())
            }
        }
    }

    @Test
    fun testEnum_ShouldBeReturned_whenGetFirstEnumIsCalled() {
        runBlocking {
            val enum = service.getFirstEnum()
            assertEquals(enum, NarpcTestUtils.TestService.TestEnum.First)
        }
    }

    @Test
    fun testPolymorphicSerialization() {
//        val animals = listOf<Animal>(Mammal("lion", 4), Mammal("Human", 2))
//        val format = Json {
//            serializersModule = SerializersModule {
//                polymorphic(Animal::class) {
//                    subclass(Mammal::class, Mammal.serializer())
//                    subclass(Bird::class, Bird.serializer())
//                }
//            }
//        }
//        println(format.encodeToJsonElement(animals))
//        println(format.encodeToJsonElement(ListSerializer(Animal.serializer()), animals))
//        println(format.encodeToJsonElement(ListSerializer(Mammal.serializer()), animals as List<Mammal>))
        assertTrue { true }
    }

    @Test
    fun listOfASingleSubType_ShouldBeParsedCorrectly_whenServerReturnsAListOfBaseType() {
        runBlocking {
            val animals = service.getAnimals("mLion4,mHuman2")
            assertTrue { animals.size == 2 }
            assertTrue { animals.first() is Mammal && (animals.first() as Mammal).legs == 4 }
            assertTrue { animals.last() is Mammal && (animals.last() as Mammal).legs == 2 }
        }
    }

    @Test
    fun listOfDifferentSubTypes_ShouldBeParsedCorrectly_whenServerReturnsAListOfBaseType() {
        //This is failing due to an issue with kotlinx serialization
        runBlocking {
            val animals = service.getAnimals("mLion4,bEagle2,mHuman2")
            assertTrue { animals.size == 3 }
            assertTrue { animals.first() is Mammal && (animals.first() as Mammal).legs == 4 }
            assertTrue { animals[1] is Bird && (animals[1] as Bird).wings == 2 }
        }
    }

    @Test
    fun differentClientName_ShouldBeReturned_whenServerIsCalledAfterTokenIsUpdated() {
        //This is failing due to an issue with kotlinx serialization
        runBlocking {
            val firstUsername = service.getUsername()
            assertTrue { firstUsername == username }
            val anotherUsername = "second_username"
            username = anotherUsername
            val secondUsername = service.getUsername()
            assertTrue { secondUsername == anotherUsername }

        }
    }


}