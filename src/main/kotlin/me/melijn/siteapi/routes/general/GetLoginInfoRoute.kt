package me.melijn.siteapi.routes.general

import io.ktor.http.*
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.utils.getUserInfo

class GetLoginInfoRoute : AbstractRoute("/cookie/decrypt/user", HttpMethod.Post) {

    override suspend fun execute(context: IRouteContext) {
        val userInfo = getUserInfo(context) ?: return
        val avatar = userInfo.avatar
        val id = userInfo.idLong
        val tag = userInfo.userName + "#" + userInfo.discriminator
        val defaultAvatarId = userInfo.discriminator.toInt() % 5
        val isGif = avatar.startsWith("a_")
        val isDefault = avatar == "null"
        val avatarPath = if (isDefault) "embed/avatars/${defaultAvatarId}.png"
        else "avatars/${id}/$avatar"

        context.replyJson {
            put("status", "success")
            put("tag", tag)
            put("isGif", isGif)
            put("isDefault", isDefault)
            put("avatar", "https://cdn.discordapp.com/$avatarPath")
        }
    }
}