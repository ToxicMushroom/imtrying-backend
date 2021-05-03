package me.melijn.siteapi.routes.dashboard.guild

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.*
import io.ktor.http.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.utils.getBodyNMessage
import me.melijn.siteapi.utils.getGuildId
import me.melijn.siteapi.utils.getSafeNode
import me.melijn.siteapi.utils.getUserInfo


class StarboardRoute {

    class Set : AbstractRoute("/setsettings/starboard", HttpMethod.Post) {

        override suspend fun execute(context: IRouteContext) {
            val body = getBodyNMessage(context) ?: return
            val guildId = getGuildId(context) ?: return
            val settings = body.getSafeNode("settings", context) ?: return

            val userId = getUserInfo(context)?.idLong ?: return

            val node = objectMapper.createObjectNode()
            node.put("userId", userId)
            node.set<JsonNode>("settings", settings)

            val url = "${context.getMelijnHost(guildId)}/setsettings/starboard/$guildId"
            val forward = httpClient.post<String>(url) {
                this.body = node.toString()
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }

            context.replyJson(forward)
        }
    }

    class Get : AbstractRoute("/getsettings/starboard", HttpMethod.Post) {

        override suspend fun execute(context: IRouteContext) {
            val guildId = getGuildId(context) ?: return
            val userId = getUserInfo(context)?.idLong ?: return

            val url = "${context.getMelijnHost(guildId)}/getsettings/starboard/$guildId"
            val response = httpClient.post<String>(url) {
                this.body = "$userId"
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }

            context.replyJson(response)
        }
    }
}