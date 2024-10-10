package me.melijn.siteapi.routes

import com.fasterxml.jackson.annotation.JsonProperty
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.SessionInfo
import me.melijn.siteapi.models.UserInfo
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.RateLimiter
import me.melijn.siteapi.utils.Base58
import me.melijn.siteapi.utils.getBodyNMessage
import me.melijn.siteapi.utils.getSafeString
import me.melijn.siteapi.utils.json
import java.nio.ByteBuffer
import kotlin.random.Random

@Serializable
data class Oauth2TokenResp(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    val scope: String,
)

@Serializable
data class Oauth2UserResp(
    val avatar: String,
    val username: String,
    val discriminator: String,
    val id: Long,
)

class CreateCookieRoute : AbstractRoute("/cookie/encrypt/code", HttpMethod.Post) {

    init {
        rateLimiter = RateLimiter(10, 60)
    }

    companion object {
        var lastSubId = System.currentTimeMillis()
    }

    override suspend fun execute(context: IRouteContext) {
        val body = getBodyNMessage(context) ?: return
        val routePart = body.getSafeString("route", context) ?: return
        val code = body.getSafeString("code", context) ?: return

        val oauth = context.settings.discordOauth
        val encodedUrlParams = Parameters.build {
            append("client_id", oauth.botId)
            append("client_secret", oauth.botSecret)
            append("grant_type", "authorization_code")
            append("code", code)
            append("redirect_uri", oauth.redirectUrl + routePart)
        }.formUrlEncode()


        try {
            val tokenResponse = httpClient.post("${context.discordApi}/oauth2/token") {
                setBody(encodedUrlParams)
                headers {
                    append("Content-Type", "application/x-www-form-urlencoded")
                }
            }.body<Oauth2TokenResp>()

            val token = tokenResponse.accessToken
            val refreshToken = tokenResponse.refreshToken
            val lifeTime = tokenResponse.expiresIn
            val scope = tokenResponse.scope

            val required = listOf("identify", "guilds")
            if (required.any { !scope.contains(it) }) {
                context.replyError(
                    HttpStatusCode.BadRequest,
                    "Missing one or more of the following discord scopes in the code parameter you supplied." +
                            "\nRequired scopes: $required"
                )
                return
            }

            val user = httpClient.get("${context.discordApi}/users/@me") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }.body<Oauth2UserResp>()

            val avatar = user.avatar
            val userName = user.username
            val discriminator = user.discriminator
            val userId = user.id
            val tag = "$userName#$discriminator"

            val key = Keys.hmacShaKeyFor(context.settings.restServer.jwtKey)
            val prevUid = lastSubId++
            val newUid = (prevUid + 1).toString() + Random.nextLong()

            val jwt = DefaultJwtBuilder()
                .setPayload(newUid)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()

            val json = objectMapper.createObjectNode()
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

            context.replyJson(json.toString())

        } catch (t: Throwable) {
            t.printStackTrace()
            context.replyError(HttpStatusCode.InternalServerError)
        }
    }
}