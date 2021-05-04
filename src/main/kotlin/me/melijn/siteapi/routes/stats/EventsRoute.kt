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

        // Cache content
        var totalEvents = mutableMapOf<String, Long>()
        var totalCommands = mutableMapOf<Int, Int>()
        var totalGuilds = mutableListOf<Pair<Long, Int>>()
        var lastRequest = 0L
    }

    override suspend fun execute(context: IRouteContext) {
        if (context.now - lastCacheRefresh > CACHE_REFRESH_TIME) {
            totalEvents.clear()
            totalCommands.clear()
            totalGuilds.clear()
            for (podId in 0 until context.podInfo.podCount) {
                val url = "${context.melijnHostPattern.replace("{podId}", "$podId")}/events"
                val json = httpClient.get<String>(url).json()
                val events = json.get("events")
                val commandUses = json.get("commandUses")
                val highestGuilds = json.get("highestGuilds")
                lastRequest = json.get("lastPoint").asLong()

                val eventsMap = objectMapper.readValue<Map<String, Int>>(events.asText())
                val commandUsesMap = objectMapper.readValue<Map<Int, Int>>(commandUses.asText())
                val highestGuildsMap = objectMapper.readValue<List<Pair<Long, Int>>>(highestGuilds.asText())
                eventsMap.forEach { (t, u) -> totalEvents[t] = totalEvents.getOrDefault(t, 0) + u }
                commandUsesMap.forEach { (t, u) -> totalCommands[t] = totalCommands.getOrDefault(t, 0) + u }
                highestGuildsMap.forEach { (t, u) ->
                    if (totalGuilds.size > 10) {
                        val (key, value) = totalGuilds.firstOrNull { it.second < u } ?: return@forEach
                        totalGuilds.remove(key to value)
                    }
                    totalGuilds.add(t to u)
                }
            }
            lastCacheRefresh = context.now
        }

        context.response.header("cache-control", "max-age=5000")
        context.replyJson {
            put("events", objectMapper.writeValueAsString(totalEvents))
            put("commandUses", objectMapper.writeValueAsString(totalCommands))
            put("highestGuilds", objectMapper.writeValueAsString(totalGuilds))
            put("lastPoint", lastRequest)
        }
    }
}