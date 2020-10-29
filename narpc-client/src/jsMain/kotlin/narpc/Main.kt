package narpc

/*
import com.narbase.kunafa.core.components.Page
import com.narbase.kunafa.core.components.TextView
import com.narbase.kunafa.core.components.page
import com.narbase.kunafa.core.components.textView
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.serialization.Serializable
import kotlin.test.assertTrue

@Serializable
data class TestResponse(val success: String)

fun main(args: Array<String>){
//    window.onload = { document.body?.sayHello() }
    var callResultView: TextView?= null
    page {
        Page.title = "Test"
        callResultView = textView {
            text = "waiting"
        }
    }
    GlobalScope.promise {
        val client = HttpClient{
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        val response = client.post<TestResponse>("http://localhost:8010/ktor_client_test"){
            contentType(ContentType.Application.Json)
        }
        println(response)
        assertTrue { response.success == "true" }
        callResultView?.text = response.toString()
    }.then({}, {
        console.log(it.stackTraceToString())
    })
}
*/

/*
fun Node.sayHello() {
    append {
        div {
            +"Hello from JS"
        }
    }
}*/
