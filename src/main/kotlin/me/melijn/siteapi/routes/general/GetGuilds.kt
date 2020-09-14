package me.melijn.siteapi.routes.general

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.routes.general.CookieDecryptGuildsHandler.rateLimitInfo
import me.melijn.siteapi.routes.general.CookieDecryptGuildsHandler.requestMap
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.RateLimitUtils.getValidatedRouteRateLimitNMessage
import me.melijn.siteapi.utils.getJWTPayloadNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage

object CookieDecryptGuildsHandler {
    val requestMap = mutableMapOf<String, RateLimitUtils.RequestInfo>()
    val rateLimitInfo = RateLimitUtils.RateLimitInfo(2, 5000)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuilds(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    val json = getJWTPayloadNMessage(context, jwt) ?: return

    getValidatedRouteRateLimitNMessage(context, requestMap, rateLimitInfo) ?: return

    val token = json.get("token").asText()

    val partialGuilds = objectMapper.readTree(
        httpClient.get<String>("${context.discordApi}/users/@me/guilds") {
            this.headers {
                this.append("Authorization", "Bearer $token")
                this.append("user-agent", "Melijn dashboard")
            }
        }
    )

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGuilds = objectMapper.readTree(
        httpClient.post<String>("${context.melijnApi}/upgradeGuilds") {
            this.body = partialGuilds.toString()
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }
    )

    val avatar = json.get("avatar").asText()
    val id = json.get("id").asLong()
    val tag = json.get("tag").asText()
    val defaultAvatarId = tag.takeLast(4).toInt() % 5
    val isGif = avatar.startsWith("a_")
    val isDefault = avatar == "null"

    val node = objectMapper.createObjectNode()
        .put("tag", tag)
        .put("isGif", isGif)
        .put("isDefault", isDefault)
        .put(
            "avatar",
            "https://cdn.discordapp.com/" + if (isDefault) {
                "embed/avatars/${defaultAvatarId}.png"
            } else {
                "avatars/${id}/$avatar"
            }
        )
        .set<JsonNode>("guilds", melijnGuilds)

    call.respondText {
        node.toString()
    }
}