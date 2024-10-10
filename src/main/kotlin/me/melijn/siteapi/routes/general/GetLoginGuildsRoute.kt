package me.melijn.siteapi.routes.general

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.RateLimiter
import me.melijn.siteapi.utils.getJWTNMessage
import me.melijn.siteapi.utils.getSessionInfo
import me.melijn.siteapi.utils.getUserInfo
import me.melijn.siteapi.utils.json

class GetLoginGuildsRoute : AbstractRoute("/cookie/decrypt/guilds", HttpMethod.Post) {

    init {
        rateLimiter = RateLimiter(5, 5)
    }

    override suspend fun execute(context: IRouteContext) {
        val discordOauth = getSessionInfo(context)?.oauthToken ?: return
        val guildsInfo = getCachedGuildsOrRefresh(context, discordOauth) ?: return

        val userInfo = getUserInfo(context) ?: return
        val avatar = userInfo.avatar
        val id = userInfo.idLong
        val tag = userInfo.userName + "#" + userInfo.discriminator
        val defaultAvatarId = userInfo.discriminator.toInt() % 5
        val isGif = avatar.startsWith("a_")
        val isDefault = avatar == "null"
        val avatarUrl = "https://cdn.discordapp.com/" + if (isDefault) {
            "embed/avatars/${defaultAvatarId}.png"
        } else {
            "avatars/${id}/$avatar"
        }

        val response =  objectMapper.writeValueAsString(GetGuildsResponse(
            tag,
            isGif,
            isDefault,
            avatarUrl,
            guildsInfo.guilds
        ))

        context.replyJson(response)
    }

    private suspend fun getCachedGuildsOrRefresh(context: IRouteContext, oauthToken: String): GuildsInfo? {
        val guildsWrapper = context.daoManager.guildsWrapper
        val jwt = getJWTNMessage(context)
        if (jwt == null) {
            return jwt
        }
        val cached = guildsWrapper.getGuildsInfo(jwt)
        if (cached != null) return cached

        val partialGuilds = httpClient.get("${context.discordApi}/users/@me/guilds") {
            headers {
                append("Authorization", "Bearer $oauthToken")
            }
        }.bodyAsText()


        val list = mutableListOf<GuildsInfo.GuildInfo>()
        for (id in context.getPodIds()) {
            // Includes info like: is melijn a member, does the user have permission to the dashboard
            val base = context.melijnHostPattern.replace("{podId}", "$id")
            val melijnGuilds = httpClient.post("$base/upgradeGuilds") {
                setBody(partialGuilds)
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }.bodyAsText()

            val subList = try { objectMapper.readValue<List<GuildsInfo.GuildInfo>>(melijnGuilds) } catch (t: Throwable) { null }
            subList?.let { list.addAll(it) }
        }

        val guildsInfo = GuildsInfo(list)
        guildsWrapper.setGuildsInfo(jwt, guildsInfo)
        return guildsInfo
    }

    data class GetGuildsResponse(
        val tag: String,
        val isGif: Boolean,
        val isDefault: Boolean,
        val avatar: String,
        val guilds: List<GuildsInfo.GuildInfo>
    )
}

