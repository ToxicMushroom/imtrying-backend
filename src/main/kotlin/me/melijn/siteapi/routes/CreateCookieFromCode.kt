package me.melijn.siteapi.routes

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.models.SessionInfo
import me.melijn.siteapi.models.UserInfo
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.Base58
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.RateLimitUtils.getValidatedRouteRateLimitNMessage
import java.nio.ByteBuffer
import kotlin.random.Random

object CreateCookieFromCodeHandler {
    val requestMap = mutableMapOf<String, RateLimitUtils.RequestInfo>()
    val rateLimitInfo = RateLimitUtils.RateLimitInfo(10, 60_000)
    var lastSubId = System.currentTimeMillis()
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieFromCode(context: RequestContext) {
    val code = try {
        objectMapper.readTree(call.receiveText())?.get("code")?.asText()
            ?: throw IllegalStateException()
    } catch (t: Throwable) {
        val json = objectMapper.createObjectNode()
        json.put("error", "bad request")
        call.respondText { json.toString() }
        return
    }

    val oauth = context.settings.discordOauth
    val encodedUrlParams = Parameters.build {
        append("client_id", oauth.botId)
        append("client_secret", oauth.botSecret)
        append("grant_type", "authorization_code")
        append("code", code)
        append("redirect_uri", oauth.redirectUrl)
    }.formUrlEncode()

    getValidatedRouteRateLimitNMessage(
        context,
        CreateCookieFromCodeHandler.requestMap,
        CreateCookieFromCodeHandler.rateLimitInfo
    )
    val json = objectMapper.createObjectNode()

    try {
        val tokenResponse = objectMapper.readTree(
            httpClient.post<String>("${context.discordApi}/oauth2/token") {
                this.body = encodedUrlParams
                this.headers {
                    this.append("Content-Type", "application/x-www-form-urlencoded")
                    this.append("user-agent", "Melijn dashboard")
                }
            }
        )

        val token = tokenResponse.get("access_token").asText()
        val refreshToken = tokenResponse.get("refresh_token").asText()
        val lifeTime = tokenResponse.get("expires_in").asLong()

        val scope = tokenResponse.get("scope").asText()
        if (!scope.contains("identify") || !scope.contains("guilds")) {
            json.put("error", "incomplete scope")
            call.respondText { json.toString() }
            return
        }

        val user = objectMapper.readTree(
            httpClient.get<String>("${context.discordApi}/users/@me") {
                this.headers {
                    this.append("Authorization", "Bearer $token")
                    this.append("user-agent", "poopoo")
                }
            }
        )

        val avatar = user.get("avatar").asText()
        val userName = user.get("username").asText()
        val discriminator = user.get("discriminator").asText()
        val userId = user.get("id").asLong()
        val tag = "$userName#$discriminator"

        val key = Keys.hmacShaKeyFor(context.jwtKey)
        val prevUid = CreateCookieFromCodeHandler.lastSubId++
        val newUid = (prevUid + 1).toString() + Random.nextLong()

        val jwt = DefaultJwtBuilder()
            .setPayload(newUid)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        json
            .put("jwt", jwt)
            .put("lifeTime", lifeTime)
            .put("avatar", avatar)
            .put("tag", tag)

        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES * 2)
            .putLong(prevUid)
            .putLong(Random.nextLong())

        context.daoManager.sessionWrapper.setSessionInfo(
            jwt, SessionInfo(
                Base58.encode(buffer.array()),
                context.now + lifeTime,
                userId,
                token,
                refreshToken
            )
        )

        context.daoManager.userWrapper.setUserInfo(
            jwt, UserInfo(
                userId,
                userName,
                discriminator,
                avatar
            ),
            lifeTime
        )

        call.respondText { json.toString() }

    } catch (t: Throwable) {
        json.put("error", t.message)
        call.respondText { json.toString() }
    }
}