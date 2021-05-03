package me.melijn.siteapi.routes.stats

import io.ktor.client.request.*
import io.ktor.response.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext

class GetPublicStatsRoute : AbstractRoute("/publicStats") {

    companion object{
        const val CACHE_REFRESH_TIME = 1_000
        var lastCacheRefresh = 0L
        var cachedValue = ""
    }

    override suspend fun execute(context: IRouteContext) {
        if (context.now - lastCacheRefresh > CACHE_REFRESH_TIME) {
            val json = httpClient.get<String>("${context.getRandomHost()}/publicStats")
            cachedValue = json
            lastCacheRefresh = context.now
        }

        context.response.header("cache-control", "max-age=1000")
        context.replyJson(cachedValue)
    }
}