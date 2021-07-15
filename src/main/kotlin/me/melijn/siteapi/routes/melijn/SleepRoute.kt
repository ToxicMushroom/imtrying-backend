package me.melijn.siteapi.routes.melijn

import io.ktor.client.request.*
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.get

class SleepRoute : AbstractRoute("/sleep") {

    init {
        authorization = true
    }

    override suspend fun execute(context: IRouteContext) {
        val resList = mutableListOf<String>()
        for (podId in 0 until context.podInfo.podCount) {
            val url = "${context.melijnHostPattern.replace("{podId}", "$podId")}/shutdown"
            try {
                val res = context.get<String>(url) {
                    header("Authorization", context.settings.melijnApi.token)
                }
                resList.add("$url -> $res")
            } catch (t: Throwable) {
                resList.add("$url -> null")
                null
            } ?: continue
        }
        context.reply("$resList")
    }
}