package me.melijn.siteapi.routes.verify

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.getPostBodyNMessage
import me.melijn.siteapi.utils.validateJWTNMessage

object VerifyGuild {
    val requestMap = mutableMapOf<String, RateLimitUtils.RequestInfo>()
    val rateLimitInfo = RateLimitUtils.RateLimitInfo(10, 5000)
}


suspend fun PipelineContext<Unit, ApplicationCall>.handleVerifyGuild(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    validateJWTNMessage(context, jwt) ?: return

    val recaptcha = postBody.get("recaptcha")?.asText() ?: return
    val guildId = postBody.get("guild")?.asText()?.toLongOrNull() ?: return

    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return

    RateLimitUtils.getValidatedRouteRateLimitNMessage(context,
        VerifyGuild.requestMap,
        VerifyGuild.rateLimitInfo
    ) ?: return

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val lol = httpClient.post<String>("https://www.google.com/recaptcha/api/siteverify") {
        this.parameter("secret", context.recaptchaSecret)
        this.parameter("response", recaptcha)
        this.headers {
            this.append("Authorization", context.melijnApiKey)
        }
    }

    // Includes info like: is melijn a member, does the user have permission to the dashboard
    httpClient.post<String>("${context.melijnApi}/unverified/verify") {
        this.body = objectMapper.createObjectNode()
            .put("userId", userInfo.idLong.toString())
            .put("guildId", guildId.toString())
            .toString()
        this.headers {
            this.append("Authorization", context.melijnApiKey)
        }
    }

    call.respondText { "verified" }
}