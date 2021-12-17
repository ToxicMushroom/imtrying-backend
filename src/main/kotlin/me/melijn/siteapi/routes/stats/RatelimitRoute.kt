package me.melijn.siteapi.routes.stats

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.get
import kotlin.collections.set

class RatelimitRoute : AbstractRoute("/ratelimit") {

    init {
        authorization = true
    }

    override suspend fun execute(context: IRouteContext) {
        val botCounts = mutableMapOf<Int, Int>()
        val botRouteCounts = mutableMapOf<String, MutableMap<Int, Int>>()
        for (id in context.getPodIds()) {
            val info = try {
                context.get<String>("${context.getPodHostUrl(id)}/ratelimitinfo")
            } catch (t: Throwable) {
                null
            } ?: continue
            val tree = objectMapper.readTree(info)
            val node = objectMapper.readValue<Map<Int, Int>>(tree["errorCounts"].asText())
            val pathNode = objectMapper.readValue<Map<String, Map<Int, Int>>>(tree["pathErrorCounts"].asText())
            node.entries.forEach { botCounts[it.key] = (botCounts[it.key] ?: 0) + it.value }
            pathNode.entries.forEach {
                val current = botRouteCounts[it.key] ?: mutableMapOf()
                it.value.forEach { it2 -> current[it2.key] = (current[it2.key] ?: 0) + it2.value }
                botRouteCounts[it.key] = current
            }
        }
        context.replyJson(
            objectMapper.createObjectNode()
                .put("botCounts", objectMapper.writeValueAsString(botCounts))
                .put("botRouteCounts", objectMapper.writeValueAsString(botRouteCounts))
                .toString()
        )
    }
}