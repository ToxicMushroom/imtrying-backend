package me.melijn.siteapi.utils

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.models.UserInfo
import me.melijn.siteapi.objectMapper

suspend fun validateJWTNMessage(context: RequestContext, jwt: String): Boolean? {
    try {
        context.jwtParser.parsePlaintextJws(jwt).body // Parser knows about key and will validate contents
    } catch (e: Throwable) {
        val node = objectMapper.createObjectNode()
        val resp = node.put("error", "\uD83D\uDD95")
            .put("status", "stinky")
            .toString()
        context.call.respondText { resp }
        return null
    }

    return true
}


suspend fun getUserInfoNMessage(context: RequestContext, jwt: String): UserInfo? {
    val ui = context.daoManager.userWrapper.getUserInfo(jwt)
    if (ui == null) {
        context.call.respondText(
            objectMapper.createObjectNode()
                .put("status", "invalidated")
                .toString()
        )
    }

    return ui
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

