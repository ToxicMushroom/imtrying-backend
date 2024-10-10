package me.melijn.siteapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

val httpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(UserAgent) {
        agent = "Melijn Backend / 1.0.0 Website backend"
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 4000
        connectTimeoutMillis = 4000
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