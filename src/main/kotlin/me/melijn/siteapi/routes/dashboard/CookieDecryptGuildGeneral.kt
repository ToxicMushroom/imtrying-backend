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


suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuildGeneral() {
    val parser = Jwts.parserBuilder()
        .setSigningKey(keyString)
        .build()

    val postBody = try {
        objectMapper.readTree(call.receiveText())
    } catch (t: Throwable) {
        val json = objectMapper.createObjectNode()
        json.put("error", "bad request")
        call.respondText { json.toString() }
        return
    }

    val jwt = postBody.get("jwt").asText()
    val guildId = postBody.get("id").asText()

    val node = objectMapper.createObjectNode()


    val unjsonedPayload = try {
        parser.parsePlaintextJws(jwt).body
    } catch (e: SignatureException) {
        val resp = node.put("error", "\uD83D\uDD95")
            .put("status", "stinky")
            .toString()
        call.respondText { resp }
        return
    }


    val payload: String? = "{$unjsonedPayload}"
    val json = payload?.let {
        try {
            objectMapper.readTree(it)
        } catch (t: Throwable) {
            null
        }
    }

    if (json == null) {
        node.put("status", "invalid_body $node")
        call.respondText { node.toString() }
        return
    }


    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGeneralSettings = objectMapper.readTree(
        httpClient.post<String>("$melijnApi/getsettings/general/$guildId") {
            this.body = json.get("id").asText()
            this.headers {
                this.append("Authorization", "Bearer $melijnApiKey")
            }
        }
    )

    val guildInfo = melijnGeneralSettings.get("guild")
    val settings = melijnGeneralSettings.get("settings")

    node.set<JsonNode>("guild", guildInfo)
    node.set<JsonNode>("settings", settings)

    call.respondText(node.toString())

}
