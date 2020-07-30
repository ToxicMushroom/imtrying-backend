package me.melijn.siteapi.routes

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


suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuild() {
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
    val melijnGuild = objectMapper.readTree(
        httpClient.post<String>("$melijnApi/guild/$guildId") {
            this.body = json.get("id").asText()
            this.headers {
                this.append("Authorization", "Bearer $melijnApiKey")
            }
        }
    )

    node.put("name", melijnGuild.get("name").asText())
    node.put("isGif", melijnGuild.get("icon").asText().startsWith("a_"))
    node.put("icon", melijnGuild.get("icon").asText())
    node.put("id", guildId)

    call.respondText(node.toString())

}
