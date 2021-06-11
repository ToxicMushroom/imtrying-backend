package me.melijn.siteapi.routes.melijn

import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.get

class SleepRoute : AbstractRoute("/sleep") {

    override suspend fun execute(context: IRouteContext) {
        for (podId in 0 until context.podInfo.podCount) {
            val url = "${context.melijnHostPattern.replace("{podId}", "$podId")}/shutdown"
            val resList = mutableListOf<String>()
            try {
                val res = context.get<String>(url)
                resList.add("$url -> $res")
            } catch (t: Throwable) {
                resList.add("$url -> null")
                null
            } ?: continue
            context.reply("Slept all")
        }
    }
}