package me.melijn.siteapi.utils

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper

suspend fun getJWTPayloadNMessage(context: RequestContext, jwt: String): JsonNode? {
    val rawPayload = try {
        context.jwtParser.parsePlaintextJws(jwt).body
    } catch (e: Throwable) {
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

