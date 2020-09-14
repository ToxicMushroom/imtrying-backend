package me.melijn.siteapi.routes.raw

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.models.RequestContext

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieEncrypt(requestContext: RequestContext) {
    val key = Keys.hmacShaKeyFor(requestContext.jwtKey)
    val data = call.receiveText().removeSurrounding("{", "}") // Unjson the json object
    val jwt = DefaultJwtBuilder()
        .setPayload(data)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact()
    call.respondText { jwt }
}