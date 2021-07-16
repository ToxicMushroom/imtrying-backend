package me.melijn.siteapi.routes.commands

import io.ktor.client.request.*
import io.ktor.response.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext

class CommandsRoute : AbstractRoute("/commands") {

    companion object {
        const val CACHE_REFRESH_TIME = 1 * 60 * 60 * 1000
        var lastCacheRefresh = 0L
        var cachedValue = ""
    }

    override suspend fun execute(context: IRouteContext) {
        if (context.now - lastCacheRefresh > CACHE_REFRESH_TIME) {
            val json = httpClient.get<String>("${context.getRandomHost()}/fullCommands")
            cachedValue = json
            lastCacheRefresh = context.now
        }

        context.response.header("cache-control", "max-age=3600")
        context.replyJson(cachedValue)
    }
}