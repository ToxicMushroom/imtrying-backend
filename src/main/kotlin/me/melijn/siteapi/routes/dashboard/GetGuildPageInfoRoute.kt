package me.melijn.siteapi.routes.dashboard

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.http.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.utils.getBodyNMessage
import me.melijn.siteapi.utils.getJWTNMessage

class GetGuildPageInfoRoute : AbstractRoute("/cookie/decrypt/guild", HttpMethod.Post) {

    override suspend fun execute(context: IRouteContext) {
        val body = getBodyNMessage(context) ?: return
        val guildId = body.get("id")?.asText()?.toLongOrNull() ?: return
        val jwt = getJWTNMessage(context) ?: return
        val daoManager = context.daoManager

        val userInfo = daoManager.userWrapper.getUserInfo(jwt) ?: return
        val guildInfoN = daoManager.guildWrapper.getGuildInfo(jwt, guildId)
        val guildInfo = if (guildInfoN == null) {
            // Includes info like: is melijn a member, does the user have permission to the dashboard
            val melijnGuild = httpClient.post<String>("${context.getMelijnHost(guildId)}/guild/$guildId") {
                this.body = userInfo.idLong.toString()
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }

            val guildInf = objectMapper.readValue<GuildsInfo.GuildInfo>(melijnGuild)
            daoManager.guildWrapper.setGuildInfo(jwt, guildId, guildInf)
            guildInf
        } else {
            guildInfoN
        }

        context.replyJson {
            put("name", guildInfo.name)
            put("isGif", guildInfo.icon?.startsWith("a_") ?: false)
            put("icon", guildInfo.icon)
            put("id", guildInfo.guildId)
        }
    }
}
