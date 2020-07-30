package me.melijn.siteapi.routes

import com.fasterxml.jackson.databind.JsonNode
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.SignatureException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.*

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuilds() {
    val parser = Jwts.parserBuilder()
        .setSigningKey(keyString)
        .build()

    val postBody = try {
        objectMapper.readTree(call.receiveText()).get("jwt").asText()
    } catch (t: Throwable) {
        val json = objectMapper.createObjectNode()
        json.put("error", "bad request")
        call.respondText { json.toString() }
        return
    }

    val node = objectMapper.createObjectNode()


    val unjsonedPayload = try {
        parser.parsePlaintextJws(postBody).body
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

    val token = json.get("token").asText()

    val partialGuilds = objectMapper.readTree(
        httpClient.get<String>("$discordApi/users/@me/guilds") {
            this.headers {
                this.append("Authorization", "Bearer $token")
                this.append("user-agent", "poopoo")
            }
        }
    )

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGuilds = objectMapper.readTree(
        httpClient.post<String>("$melijnApi/upgradeGuilds") {
            this.body = partialGuilds.toString()
            this.headers {
                this.append("Authorization", "Bearer $melijnApiKey")
            }
        }
    )

    node.set<JsonNode>("guilds", melijnGuilds)
    node.put("tag", json.get("tag").asText())
    val avatar = json.get("avatar").asText()

    node.put("isGif", avatar.startsWith("a_"))
    node.put("id", json.get("id").asText())
    node.put("avatar", "https://cdn.discordapp.com/avatars/${json.get("id").asLong()}/$avatar")

    call.respondText {
        node.toString()
    }

}
