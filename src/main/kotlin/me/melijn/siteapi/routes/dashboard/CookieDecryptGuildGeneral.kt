package me.melijn.siteapi.routes.dashboard

import com.fasterxml.jackson.databind.JsonNode
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.SignatureException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.*
import me.melijn.siteapi.models.RequestContext


suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuildGeneral(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return
    val guildId = postBody.get("id")?.asText() ?: return

    val jwtJson = getJWTPayloadNMessage(context, jwt) ?: return

    val userId = jwtJson.get("id")?.asText() ?: return

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGeneralSettings = objectMapper.readTree(
        httpClient.post<String>("$melijnApi/getsettings/general/$guildId") {
            this.body = userId
            this.headers {
                this.append("Authorization", "Bearer $melijnApiKey")
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

    val jwt = postBody.get("jwt").asText()
    val guildId = postBody.get("id").asText()
    val settings = postBody.get("settings")

    val jwtJson = getJWTPayloadNMessage(context, jwt) ?: return

    val userId = jwtJson.get("id").asText()

    val node = objectMapper.createObjectNode()
    node.put("userId", userId)
    node.set<JsonNode>("settings", settings)

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnPostGeneralSettings = objectMapper.readTree(
        httpClient.post<String>("$melijnApi/postsettings/general/$guildId") {
            this.body = node.toString()
            this.headers {
                this.append("Authorization", "Bearer $melijnApiKey")
            }
        }
    )

    call.respondText { melijnPostGeneralSettings.toString() }
}

suspend fun getJWTPayloadNMessage(context: RequestContext, jwt: String): JsonNode? {
    val rawPayload = try {
        context.jwtParser.parsePlaintextJws(jwt).body
    } catch (e: SignatureException) {
        val node = objectMapper.createObjectNode()
        val resp = node.put("error", "\uD83D\uDD95")
            .put("status", "stinky")
            .toString()
        context.call.respondText { resp }
        return null
    }


    val payload: String? = "{$rawPayload}"
    val json = payload?.let {
        try {
            objectMapper.readTree(it)
        } catch (t: Throwable) {
            null
        }
    }

    if (json == null) {
        val node = objectMapper.createObjectNode()
        node.put("status", "invalid_body $node")
        context.call.respondText { node.toString() }
        return null
    }

    return json
}

suspend fun getPostBodyNMessage(call: ApplicationCall): JsonNode? {
    return try {
        objectMapper.readTree(call.receiveText())
    } catch (t: Throwable) {
        val json = objectMapper.createObjectNode()
        json.put("error", "bad request")
        call.respondText { json.toString() }
        null
    }
}

