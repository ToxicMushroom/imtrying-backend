package me.melijn.siteapi.routes.verify

import io.ktor.client.request.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.RateLimiter
import me.melijn.siteapi.utils.getBodyNMessage
import me.melijn.siteapi.utils.getUserInfo

class VerifyGuildRoute : AbstractRoute("/verifyguild") {

    init {
        rateLimiter = RateLimiter(10, 5)
    }

    override suspend fun execute(context: IRouteContext) {
        val body = getBodyNMessage(context) ?: return
        val recaptcha = body.get("recaptcha")?.asText() ?: return
        val guildId = body.get("guild")?.asText()?.toLongOrNull() ?: return
        val userInfo = getUserInfo(context) ?: return

        httpClient.post<String>("https://www.google.com/recaptcha/api/siteverify") {
            parameter("secret", context.settings.recaptcha.secret)
            parameter("response", recaptcha)
        }

        // Includes info like: is melijn a member, does the user have permission to the dashboard
        httpClient.post<String>("${context.getMelijnHost(guildId)}/unverified/verify") {
            this.body = objectMapper.createObjectNode()
                .put("userId", userInfo.idLong.toString())
                .put("guildId", guildId.toString())
                .toString()
            headers {
                append("Authorization", context.melijnApiKey)
            }
        }

        context.replyJson {
            put("status", "success")
        }
    }
}