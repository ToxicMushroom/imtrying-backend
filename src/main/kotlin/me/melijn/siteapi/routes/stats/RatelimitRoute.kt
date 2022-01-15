package me.melijn.siteapi.routes.stats

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.get
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.set

class RatelimitRoute : AbstractRoute("/ratelimit") {

    init {
        authorization = true
    }

    var prevMap:Map<String, Map<Int, Int>> = emptyMap()
    var prevCodes: Set<Int> = mutableSetOf()

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
            val node = objectMapper.readValue<Map<Int, Int>>(tree["errorCounts"].asText()).toMutableMap()
            prevCodes.forEach { c -> node[c] = node[c] ?: 0 }
            prevCodes = node.filter { it.value != 0 }.map { it.key }.toSet()

            // set last posted data points back to 0. makes graphs look better
            val pathNode = objectMapper.readValue<Map<String, Map<Int, Int>>>(tree["pathErrorCounts"].asText()).toMutableMap()
            prevMap.forEach { (path, codeCounts) ->
                val map = HashMap(pathNode[path] ?: emptyMap())
                codeCounts.forEach { (code, zero) -> map[code] = map.getOrDefault(code, zero) }
                pathNode[path] = map
            }
            prevMap = pathNode.mapValues { pathEntry -> pathEntry.value.filter { it.value != 0 }.mapValues { 0 } }

            node.entries.forEach { botCounts[it.key] = (botCounts[it.key] ?: 0) + it.value }
            pathNode.entries.forEach {
                val current = botRouteCounts[it.key] ?: mutableMapOf()
                it.value.forEach { it2 -> current[it2.key] = (current[it2.key] ?: 0) + it2.value }
                botRouteCounts[it.key] = current
            }
        }
        val returning = objectMapper.createObjectNode()
            .put("botCounts", objectMapper.writeValueAsString(botCounts))
            .put("botRouteCounts", objectMapper.writeValueAsString(botRouteCounts))
            .toString()

        logger.info("ratelimit route returning: $returning")
        context.replyJson(returning)
    }
}