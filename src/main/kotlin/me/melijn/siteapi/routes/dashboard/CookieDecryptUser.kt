package me.melijn.siteapi.routes.dashboard

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.getJWTPayloadNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage

suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptUserSettings(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    val jwtJson = getJWTPayloadNMessage(context, jwt) ?: return

    val userId = jwtJson.get("id")?.asText() ?: return

    // Includes info like: does the user have premium
    val userSettings = objectMapper.readTree(
        httpClient.post<String>("${context.melijnApi}/getsettings/user/$userId") {
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }
    )

    val userInfo = userSettings.get("user")
    val settings = userSettings.get("settings")
    val provided = userSettings.get("provided")

    val node = objectMapper.createObjectNode()

    node.set<JsonNode>("user", userInfo)
    node.set<JsonNode>("settings", settings)
    node.set<JsonNode>("provided", provided)

    call.respondText(node.toString())
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptPostUserSettings(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return
    val settings = postBody.get("settings") ?: return

    val jwtJson = getJWTPayloadNMessage(context, jwt) ?: return

    val userId = jwtJson.get("id").asText()

    val node = objectMapper.createObjectNode()
        .set<JsonNode>("settings", settings)

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnPostUserSettings = objectMapper.readTree(
        httpClient.post<String>("${context.melijnApi}/postsettings/user/$userId") {
            this.body = node.toString()
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }
    )

    call.respondText { melijnPostUserSettings.toString() }
}
