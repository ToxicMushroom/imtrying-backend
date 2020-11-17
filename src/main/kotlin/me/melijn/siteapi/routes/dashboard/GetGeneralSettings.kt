package me.melijn.siteapi.routes.dashboard

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.getPostBodyNMessage
import me.melijn.siteapi.utils.validateJWTNMessage


suspend fun PipelineContext<Unit, ApplicationCall>.handleGetGeneralSettings(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return
    val guildId = postBody.get("id")?.asText() ?: return

    validateJWTNMessage(context, jwt) ?: return

    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return
    val userId = userInfo.idLong

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGeneralSettings = objectMapper.readTree(
        httpClient.post<String>("${context.melijnApi}/getsettings/general/$guildId") {
            this.body = "$userId"
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }
    )

    val guildInfo = melijnGeneralSettings.get("guild")
    val settings = melijnGeneralSettings.get("settings")
    val provided = melijnGeneralSettings.get("provided")

    val node = objectMapper.createObjectNode()

    node.set<JsonNode>("guild", guildInfo)
    node.set<JsonNode>("settings", settings)
    node.set<JsonNode>("provided", provided)

    call.respondText(node.toString())
}


suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptPostGuildGeneral(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return
    val guildId = postBody.get("id")?.asText() ?: return
    val settings = postBody.get("settings") ?: return

    validateJWTNMessage(context, jwt) ?: return

    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return

    val node = objectMapper.createObjectNode()
    node.put("userId", userInfo.idLong)
    node.set<JsonNode>("settings", settings)

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnPostGeneralSettings = objectMapper.readTree(
        httpClient.post<String>("${context.melijnApi}/postsettings/general/$guildId") {
            this.body = node.toString()
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }
    )

    call.respondText { melijnPostGeneralSettings.toString() }
}
