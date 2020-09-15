package me.melijn.siteapi.routes.general

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.getPostBodyNMessage
import me.melijn.siteapi.utils.validateJWTNMessage


suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptUser(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return
    validateJWTNMessage(context, jwt) ?: return

    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return
    val avatar = userInfo.avatar
    val id = userInfo.idLong
    val tag = userInfo.userName + "#" + userInfo.discriminator
    val defaultAvatarId = userInfo.discriminator.toInt() % 5
    val isGif = avatar.startsWith("a_")
    val isDefault = avatar == "null"

    val node = objectMapper.createObjectNode()
        .put("tag", tag)
        .put("isGif", isGif)
        .put("isDefault", isDefault)
        .put(
            "avatar", "https://cdn.discordapp.com/" + if (isDefault) {
                "embed/avatars/${defaultAvatarId}.png"
            } else {
                "avatars/${id}/$avatar"
            }
        )

    call.respondText {
        node.toString()
    }
}
