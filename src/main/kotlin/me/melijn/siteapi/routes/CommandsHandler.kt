package me.melijn.siteapi.routes

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.jsonType
import me.melijn.siteapi.melijnApi

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCommands() {
    val json = httpClient.get<String>("$melijnApi/fullCommands")
    call.response.header("cache-control", "max-age=3600")
    call.respondText(jsonType, HttpStatusCode.OK) {
        json
    }
}