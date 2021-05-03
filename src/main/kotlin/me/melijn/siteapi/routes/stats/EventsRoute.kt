package me.melijn.siteapi.routes.stats

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.response.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.utils.json

class EventsRoute : AbstractRoute("/events") {

    companion object {
        const val CACHE_REFRESH_TIME = 15_000
        var lastCacheRefresh = 0L
        var cache = mutableMapOf<String, Long>()
    }

    override suspend fun execute(context: IRouteContext) {
        if (context.now - lastCacheRefresh > CACHE_REFRESH_TIME) {
            for (podId in 0 until context.podInfo.podCount) {
                cache.clear()
                val url = "${context.melijnHostPattern.replace("{podId}", "$podId")}/publicStats"
                val json = httpClient.get<String>(url).json()
                val events = json.get("events")
                val commandUses = json.get("commandUses")
                val highestGuilds = objectMapper.readValue<>()json.get("highestGuilds")
                val lastRequest = json.get("lastRequest").asLong()
                val dataSet = objectMapper.readValue<Map<String, Int>>(json)
                cache.putAll(dataSet)
            }
            lastCacheRefresh = context.now
        }

        context.response.header("cache-control", "max-age=1000")
        context.replyJson(cachedValue)
    }
}