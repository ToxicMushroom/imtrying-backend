package me.melijn.siteapi.routes.verify

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.routes.verify.CookieDecryptVerifyGuilds.rateLimitInfo
import me.melijn.siteapi.routes.verify.CookieDecryptVerifyGuilds.requestMap
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.getJWTPayloadNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage


object CookieDecryptVerifyGuilds {
    val requestMap = mutableMapOf<String, RateLimitUtils.RequestInfo>()
    val rateLimitInfo = RateLimitUtils.RateLimitInfo(4, 5000)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleVerifyGuilds(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    val json = getJWTPayloadNMessage(context, jwt) ?: return

    RateLimitUtils.getValidatedRouteRateLimitNMessage(context, requestMap, rateLimitInfo) ?: return

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGuilds = objectMapper.readTree(
        httpClient.post<String>("${context.melijnApi}/upgradeGuilds") {
            this.body = json.get("id").asLong()
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }
    )

    val node = objectMapper.createObjectNode()
        .set<JsonNode>("guilds", melijnGuilds)

    call.respondText {
        node.toString()
    }
}