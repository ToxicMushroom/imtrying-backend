package me.melijn.siteapi.routes

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.ktor.client.request.*
import io.ktor.http.*
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
            val tokenResponse = httpClient.post<String>("${context.discordApi}/oauth2/token") {
                this.body = encodedUrlParams
                headers {
                    append("Content-Type", "application/x-www-form-urlencoded")
                }
            }.json()

            val token = tokenResponse.get("access_token")?.asText()
            if (token == null) {
                logger.info("unsuccessful login, discord response: " + tokenResponse.toPrettyString())
                context.replyError(HttpStatusCode.BadRequest, "Unsuccessful login, contact support if this keeps occurring")
                return
            }
            val refreshToken = tokenResponse.get("refresh_token").asText()
            val lifeTime = tokenResponse.get("expires_in").asLong()

            val scope = tokenResponse.get("scope").asText()
            val required = listOf("identify", "guilds")
            if (required.any { !scope.contains(it) }) {
                context.replyError(
                    HttpStatusCode.BadRequest,
                    "Missing one or more of the following discord scopes in the code parameter you supplied." +
                            "\nRequired scopes: $required"
                )
                return
            }

            val user = httpClient.get<String>("${context.discordApi}/users/@me") {
                this.headers {
                    append("Authorization", "Bearer $token")
                }
            }.json()

            val avatar = user.get("avatar").asText()
            val userName = user.get("username").asText()
            val discriminator = user.get("discriminator").asText()
            val userId = user.get("id").asLong()
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