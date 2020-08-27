package me.melijn.siteapi.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.utils.getJWTPayloadNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecrypt(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    val json = getJWTPayloadNMessage(context, jwt) ?: return

    call.respondText {
        json.toString()
    }
}