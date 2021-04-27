package me.melijn.siteapi.routes.verify

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
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
import me.melijn.siteapi.utils.getPostBodyNMessage
import me.melijn.siteapi.utils.validateJWTNMessage


object CookieDecryptVerifyGuilds {
    val requestMap = mutableMapOf<String, RateLimitUtils.RequestInfo>()
    val rateLimitInfo = RateLimitUtils.RateLimitInfo(3, 5000)
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleVerifyGuilds(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    validateJWTNMessage(context, jwt) ?: return

    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return

    RateLimitUtils.getValidatedRouteRateLimitNMessage(context, requestMap, rateLimitInfo) ?: return

    val allGuilds = objectMapper.createArrayNode()
    for (id in context.getPodIds()) {
        // Includes info like: is melijn a member, does the user have permission to the dashboard
        val base = context.hostPattern.replace("{podId}", "$id")
        val melijnGuilds = objectMapper.readTree(
            httpClient.post<String>("${base}/unverified/guilds") {
                this.body = userInfo.idLong.toString()
                this.headers {
                    this.append("Authorization", context.melijnApiKey)
                }
            }
        )
        if (melijnGuilds is ArrayNode)
            allGuilds.addAll(melijnGuilds)
    }

    val node = objectMapper.createObjectNode()
        .set<JsonNode>("guilds", allGuilds)

    call.respondText {
        node.toString()
    }
}