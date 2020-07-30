package me.melijn.siteapi.routes

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.keyString

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieEncrypt() {
    val key = Keys.hmacShaKeyFor(keyString)
    val data = call.receiveText().removeSurrounding("{", "}") // Unjson the json object
    val jwt = DefaultJwtBuilder()
        .setPayload(data)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact()
    call.respondText { jwt }
}