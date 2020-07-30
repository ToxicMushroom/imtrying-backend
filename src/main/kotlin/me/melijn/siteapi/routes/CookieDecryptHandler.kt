package me.melijn.siteapi.routes

import io.jsonwebtoken.Jwts
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.keyString
import me.melijn.siteapi.objectMapper
import java.security.SignatureException

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecrypt() {
    val parser = Jwts.parserBuilder()
        .setSigningKey(keyString)
        .build()


    val postBody = call.receiveText()

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

        call.respondText {
            json.toString()
        }
    } catch (e: SignatureException) {
        val resp = node.put("error", "\uD83D\uDD95")
            .put("status", "stinky")
            .toString()
        call.respondText { resp }
    }
}