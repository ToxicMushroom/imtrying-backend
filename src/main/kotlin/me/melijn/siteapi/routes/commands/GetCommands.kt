package me.melijn.siteapi.routes.commands

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.jsonType
import me.melijn.siteapi.models.RequestContext

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleGetCommands(context: RequestContext) {
    val json = httpClient.get<String>("${context.melijnApi}/fullCommands")
    call.response.header("cache-control", "max-age=3600")
    call.respondText(jsonType, HttpStatusCode.OK) {
        json
    }
}