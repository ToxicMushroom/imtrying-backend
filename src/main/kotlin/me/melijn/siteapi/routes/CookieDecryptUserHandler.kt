package me.melijn.siteapi.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.getJWTPayloadNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage


suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptUser(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    val json = getJWTPayloadNMessage(context, jwt) ?: return

    val avatar = json.get("avatar").asText()
    val id = json.get("id").asLong()
    val tag = json.get("tag").asText()
    val defaultAvatarId = tag.takeLast(4).toInt() % 5
    val isGif = avatar.startsWith("a_")
    val isDefault = avatar == "null"

    val node = objectMapper.createObjectNode()
        .put("tag", tag)
        .put("isGif", isGif)
        .put("isDefault", isDefault)
        .put(
            "avatar",
            "https://cdn.discordapp.com/" + if (isDefault) {
                "embed/avatars/${defaultAvatarId}.png"
            } else {
                "avatars/${id}/$avatar"
            }
        )

    call.respondText {
        node.toString()
    }
}
