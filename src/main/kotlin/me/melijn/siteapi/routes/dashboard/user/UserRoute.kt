package me.melijn.siteapi.routes.dashboard.user

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.*
import io.ktor.http.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.utils.getBodyNMessage
import me.melijn.siteapi.utils.getUserInfo

class UserRoute {
    class Set : AbstractRoute("/postsettings/user", HttpMethod.Post) {

        override suspend fun execute(context: IRouteContext) {
            val userId = getUserInfo(context)?.idLong ?: return
            val body = getBodyNMessage(context) ?: return
            val settings = body.get("settings") ?: return
            val node = objectMapper.createObjectNode()
                .set<JsonNode>("settings", settings)
                .toString()

            // Includes info like: does the user have premium
            val url = "${context.getRandomHost()}/postsettings/user/$userId"
            val response = httpClient.post<String>(url) {
                this.body = node
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }

            context.replyJson(response)
        }
    }

    class Get : AbstractRoute("/cookie/decrypt/user/settings", HttpMethod.Post) {

        override suspend fun execute(context: IRouteContext) {
            val userId = getUserInfo(context)?.idLong ?: return
            val url = "${context.getRandomHost()}/getsettings/user/$userId"

            val userSettings = httpClient.post<String>(url) {
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }

            context.replyJson(userSettings)
        }
    }
}