package me.melijn.siteapi.routes.commands

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.jsonType
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.routes.commands.GetCommands.cacheRefreshTime
import me.melijn.siteapi.routes.commands.GetCommands.cachedValue
import me.melijn.siteapi.routes.commands.GetCommands.lastCacheRefresh

object GetCommands {
    var lastCacheRefresh = 0L
    const val cacheRefreshTime = 3600_000
    var cachedValue = ""
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleGetCommands(context: RequestContext) {
    if (context.now - lastCacheRefresh > cacheRefreshTime) {
        val json = httpClient.get<String>("${context.melijnApi}/fullCommands")
        cachedValue = json
        lastCacheRefresh = context.now
    }
    call.response.header("cache-control", "max-age=3600")

    call.respondText(jsonType, HttpStatusCode.OK) {
        cachedValue
    }
}