package me.melijn.siteapi.routes.melijn

import io.ktor.client.request.*
import io.ktor.http.*
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.get

class IsInGuildRoute : AbstractRoute("/isinguild") {

    init {
        authorization = true
    }

    override suspend fun execute(context: IRouteContext) {
        val guildId = context.getQueryParm("guildId").toLongOrNull()
        if (guildId == null) {
            context.reply("invalid guildId", statusCode = HttpStatusCode.BadRequest)
            return
        }

        val podId = ((guildId shr 22) % context.podInfo.shardCount) / context.podInfo.shardsPerPod
        val url = "${context.melijnHostPattern.replace("{podId}", "$podId")}/guild/${guildId}"

        try {
            val res = context.get<String>(url) {
                header("Authorization", context.settings.melijnApi.token)
            } ?: run {
                context.reply("false")
                return
            }

            val responseJson = objectMapper.readTree(res)
            context.replyJson {
                put("isInGuild", responseJson.get("isBotMember").asBoolean())
            }
        } catch (t: Throwable) {
            context.reply("false")
        }
    }
}