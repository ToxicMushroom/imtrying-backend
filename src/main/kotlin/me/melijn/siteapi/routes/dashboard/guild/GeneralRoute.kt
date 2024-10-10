package me.melijn.siteapi.routes.dashboard.guild

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.utils.getBodyNMessage
import me.melijn.siteapi.utils.getGuildId
import me.melijn.siteapi.utils.getSafeNode
import me.melijn.siteapi.utils.getUserInfo

class GeneralRoute {

    class Set : AbstractRoute("/postsettings/general", HttpMethod.Post) {

        override suspend fun execute(context: IRouteContext) {
            val body = getBodyNMessage(context) ?: return

            val guildId = getGuildId(context) ?: return
            val userId = getUserInfo(context)?.idLong ?: return
            val settings = body.getSafeNode("settings", context) ?: return

            val node = objectMapper.createObjectNode()
                .put("userId", userId)
                .set<JsonNode>("settings", settings)

            val url = "${context.getMelijnHost(guildId)}/postsettings/general/$guildId"
            val forward = httpClient.post(url) {
                setBody(node.toString())
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }.bodyAsText()

            context.replyJson(forward)
        }
    }

    class Get : AbstractRoute("/cookie/decrypt/guild/general", HttpMethod.Post) {

        override suspend fun execute(context: IRouteContext) {
            val guildId = getGuildId(context) ?: return
            val userId = getUserInfo(context)?.idLong ?: return

            val forward = httpClient.post("${context.getMelijnHost(guildId)}/getsettings/general/$guildId") {
                setBody("$userId")
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }.bodyAsText()

            context.replyJson(forward)
        }
    }
}