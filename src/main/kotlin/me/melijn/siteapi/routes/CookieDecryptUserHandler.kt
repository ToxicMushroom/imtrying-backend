package me.melijn.siteapi.routes

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.SignatureException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.keyString
import me.melijn.siteapi.objectMapper


suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptUser() {
    val parser = Jwts.parserBuilder()
        .setSigningKey(keyString)
        .build()

    val postBody = try {
        objectMapper.readTree(call.receiveText()).get("jwt")?.asText() ?: throw IllegalStateException()
    } catch (t: Throwable) {
        val json = objectMapper.createObjectNode()
            .put("error", "bad request")
        call.respondText { json.toString() }
        return
    }

    val node = objectMapper.createObjectNode()
    try {

        val unjsonedPayload = parser.parsePlaintextJws(postBody).body
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

        node.put("tag", json.get("tag").asText())
        val avatar = json.get("avatar").asText()

        node.put("isGif", avatar.startsWith("a_"))
        node.put("avatar", "https://cdn.discordapp.com/avatars/${json.get("id").asLong()}/$avatar")

        call.respondText {
            node.toString()
        }
    } catch (e: Throwable) {
        val resp = node.put("error", "\uD83D\uDD95")
            .put("status", "stinky")
            .toString()
        call.respondText { resp }
    }
}
