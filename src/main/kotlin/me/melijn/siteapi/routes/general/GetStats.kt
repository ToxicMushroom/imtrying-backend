package me.melijn.siteapi.routes.general

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.jsonType
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.routes.general.GetStats.cacheRefreshTime
import me.melijn.siteapi.routes.general.GetStats.cachedValue
import me.melijn.siteapi.routes.general.GetStats.lastCacheRefresh

object GetStats {
    var lastCacheRefresh = 0L
    const val cacheRefreshTime = 1_000
    var cachedValue = ""
}

// TODO: fetch all stats
suspend inline fun PipelineContext<Unit, ApplicationCall>.handleGetStats(context: RequestContext) {
    if (context.now - lastCacheRefresh > cacheRefreshTime) {
        val json = httpClient.get<String>("${context.getRandomHost()}/publicStats")
        cachedValue = json
        lastCacheRefresh = context.now
    }
    call.response.header("cache-control", "max-age=1000")

    call.respondText(jsonType, HttpStatusCode.OK) {
        cachedValue
    }
}