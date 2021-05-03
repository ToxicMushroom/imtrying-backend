package me.melijn.siteapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

val httpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(JsonFeature) {
        serializer = JacksonSerializer(objectMapper)
    }
    install(UserAgent) {
        agent = "Melijn Backend / 1.0.0 Website backend"
    }
}

class MelijnSite {

    init {
        val container = Container()
        container.restServer.start()
    }
}

fun main() {
    MelijnSite()
}

/* Keygen */
//fun main(args: Array<String>) {
//    val key = Keys.secretKeyFor(SignatureAlgorithm.HS256) //or HS384 or HS512
//    val secretString = Encoders.BASE64.encode(key.encoded)
//    print(secretString)
//}