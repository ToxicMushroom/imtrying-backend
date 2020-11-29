package me.melijn.siteapi.routes.general

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.routes.general.CookieDecryptGuildsHandler.rateLimitInfo
import me.melijn.siteapi.routes.general.CookieDecryptGuildsHandler.requestMap
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.RateLimitUtils.getValidatedRouteRateLimitNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage
import me.melijn.siteapi.utils.validateJWTNMessage

object CookieDecryptGuildsHandler {
    val requestMap = mutableMapOf<String, RateLimitUtils.RequestInfo>()
    val rateLimitInfo = RateLimitUtils.RateLimitInfo(5, 5000)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuilds(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return
    validateJWTNMessage(context, jwt) ?: return

    getValidatedRouteRateLimitNMessage(context, requestMap, rateLimitInfo) ?: return

    val token = context.daoManager.sessionWrapper.getSessionInfo(jwt)?.oauthToken ?: return

    val guildsInfo = getCachedGuildsOrRefresh(context, jwt, token) ?: return


    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return
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

    val responseObj = GetGuildsResponse(
        tag,
        isGif,
        isDefault,
        avatarUrl,
        guildsInfo.guilds
    )

    call.respondText(objectMapper.writeValueAsString(responseObj))
}

data class GetGuildsResponse(
    val tag: String,
    val isGif: Boolean,
    val isDefault: Boolean,
    val avatar: String,
    val guilds: List<GuildsInfo.GuildInfo>
)

suspend fun getCachedGuildsOrRefresh(context: RequestContext, jwt: String, token: String): GuildsInfo? {
    val guildsWrapper = context.daoManager.guildsWrapper
    val cached = guildsWrapper.getGuildsInfo(jwt)
    if (cached != null) return cached

    val partialGuilds = objectMapper.readTree(
        httpClient.get<String>("${context.discordApi}/users/@me/guilds") {
            this.headers {
                this.append("Authorization", "Bearer $token")
                this.append("user-agent", "Melijn dashboard")
            }
        }
    )

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGuilds = httpClient.post<String>("${context.melijnApi}/upgradeGuilds") {
        this.body = partialGuilds.toString()
        this.headers {
            this.append("Authorization", context.melijnApiKey)
        }
    }


    val list = objectMapper.readValue<List<GuildsInfo.GuildInfo>>(melijnGuilds)

    val guildsInfo = GuildsInfo(list)
    guildsWrapper.setGuildsInfo(jwt, guildsInfo)
    return guildsInfo
}
