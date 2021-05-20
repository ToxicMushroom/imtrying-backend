package me.melijn.siteapi.routes.stats

import io.ktor.response.*
import me.melijn.siteapi.models.MelijnStat
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.get

class GetPublicStatsRoute : AbstractRoute("/publicStats") {

    companion object {
        const val CACHE_REFRESH_TIME = 1_000
        var lastCacheRefresh = 0L
        var cachedValue = ""
    }

    override suspend fun execute(context: IRouteContext) {
        if (context.now - lastCacheRefresh > CACHE_REFRESH_TIME) {
            var statsSum: MelijnStat? = null
            for (id in context.getPodIds()) {
                val stat = try {
                    context.get<MelijnStat>("${context.getPodHostUrl(id)}/publicStats")
                } catch (t: Throwable) {
                    null
                } ?: continue

                if (statsSum == null) statsSum = stat
                else {
                    statsSum.bot.cpuUsage += stat.bot.cpuUsage
                    statsSum.bot.ramUsage += stat.bot.ramUsage
                    statsSum.bot.jvmThreads += stat.bot.jvmThreads
                    statsSum.bot.melijnThreads += stat.bot.melijnThreads
                    statsSum.bot.ramTotal += stat.bot.ramTotal

                    statsSum.shards += stat.shards
                }
            }

            cachedValue = objectMapper.writeValueAsString(statsSum)
            lastCacheRefresh = context.now
        }

        context.response.header("cache-control", "max-age=5000")
        context.replyJson(cachedValue)
    }
}