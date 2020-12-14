package jvm_library_test.e2e

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.narbase.narpc.server.NarpcKtorHandler
import com.narbase.narpc.server.NarpcServer
import e2e.NarpcTestUtils
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.event.Level
import java.time.Duration

object TestServer {
    fun run() {
        embeddedServer(Netty, port = 8010, module = { testModule() }).apply { start(false) }
    }

    private fun Application.testModule() {

        install(ContentNegotiation) {
            gson {
//                registerTypeAdapterFactory(RuntTyp) // Use RuntimeTypeAdapterFactory
/*
                    polymorphic(Animal::class) {
                        subclass(Mammal::class, Mammal.serializer())
                        subclass(Bird::class, Bird.serializer())
                    }
*/

            }
//            serialization(ContentType.Any)
//            gson {
//                setPrettyPrinting()
//            }

        }
        install(CallLogging) {
            level = Level.INFO
        }

        install(CORS) {
            method(HttpMethod.Options)
            method(HttpMethod.Put)
            anyHost()
            header("Authorization")
            header("DNT")
            header("X-CustomHeader")
            header("Keep-Alive")
            header("User-Agent")
            header("X-Requested-With")
            header("If-Modified-Since")
            header("Cache-Control")
            header("Content-Type")
            header("Content-Range")
            header("Range")
            header("Client-Language")

            allowCredentials = true
            maxAgeInSeconds = Duration.ofDays(1).seconds
        }

        setupAuthenticators("jwt", jwtIssuer, jwtAudience)

        routing {

            authenticate("JwtAuth") {
                post("/test") {
                    NarpcKtorHandler(NarpcServer(NarpcTestUtils.RemoteTestService())).handle(call)
                }
            }
        }
    }

}

fun Application.setupAuthenticators(jwtRealm: String, jwtIssuer: String, jwtAudience: String) {
    install(Authentication) {

        jwt(name = "JwtAuth") {
            realm = jwtRealm
            verifier(makeJwtVerifier(jwtIssuer, jwtAudience))
            myJwtVerifier = makeJwtVerifier(jwtIssuer, jwtAudience)
            validate { credential ->

                val clientId = credential.payload.claims["name"]?.asString() ?: return@validate null

                AuthorizedClientData(
                    clientId
                )
            }
        }
    }
}

data class AuthorizedClientData(val name: String) : Principal

val algorithm: Algorithm = Algorithm.HMAC256("JWT_SECRET")
var myJwtVerifier: JWTVerifier? = null
private fun makeJwtVerifier(issuer: String, audience: String): JWTVerifier = JWT
    .require(algorithm)
    .withAudience(audience)
    .withIssuer(issuer)
    .build()

private const val jwtIssuer = "https://jwt-provider-domain/"
private const val jwtAudience = "jwt-audience"
fun getToken(name: String): String = JWT.create()
    .withIssuer(jwtIssuer)
    .withAudience(jwtAudience)
    .withClaim("name", name)
    .sign(algorithm)
