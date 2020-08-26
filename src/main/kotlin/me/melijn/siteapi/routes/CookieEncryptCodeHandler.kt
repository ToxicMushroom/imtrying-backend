package me.melijn.siteapi.routes

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import me.melijn.siteapi.discordApi
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.keyString
import me.melijn.siteapi.objectMapper

suspend inline fun PipelineContext<Unit, ApplicationCall>.handleCookieEncryptCode() {
    val code = try {
        objectMapper.readTree(call.receiveText())?.get("code")?.asText()
            ?: throw IllegalStateException()
    } catch (t: Throwable) {
        val json = objectMapper.createObjectNode()
        json.put("error", "bad request")
        call.respondText { json.toString() }
        return
    }
    val encodedUrlParams = Parameters.build {
        append("client_id", System.getenv("MELIJN_ID"))
        append("client_secret", System.getenv("MELIJN_SECRET"))
        append("grant_type", "authorization_code")
        append("code", code)
        append("redirect_uri", System.getenv("REDIRECT_URI"))
    }.formUrlEncode()

    val json = objectMapper.createObjectNode()

    try {
        val tokenResponse = objectMapper.readTree(
            httpClient.post<String>("$discordApi/oauth2/token") {
                this.body = encodedUrlParams
                this.headers {
                    this.append("Content-Type", "application/x-www-form-urlencoded")
                    this.append("user-agent", "poopoo")
                }
            }
        )

        val token = tokenResponse.get("access_token").asText()
        val lifeTime = tokenResponse.get("expires_in").asLong()

        val scope = tokenResponse.get("scope").asText()
        if (!scope.contains("identify") || !scope.contains("guilds")) {
            json.put("error", "incomplete scope")
            call.respondText { json.toString() }
            return
        }

        val user = objectMapper.readTree(
            httpClient.get<String>("$discordApi/users/@me") {
                this.headers {
                    this.append("Authorization", "Bearer $token")
                    this.append("user-agent", "poopoo")
                }
            }
        )

        val avatar = user.get("avatar").asText()
        val tag = user.get("username").asText() + "#" + user.get("discriminator").asText()

        json.put("token", token)
            .put("avatar", avatar)
            .put("tag", tag)
            .put("id", user.get("id").asLong())

        val payload = json.toString()
        val key = Keys.hmacShaKeyFor(keyString)
        val data = payload.removeSurrounding("{", "}") // Unjson the json object

        val jwt = DefaultJwtBuilder()
            .setPayload(data)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        json.removeAll()
            .put("jwt", jwt)
            .put("lifeTime", lifeTime)
            .put("avatar", avatar)
            .put("tag", tag)

        call.respondText { json.toString() }

    } catch (t: Throwable) {
        json.put("error", t.message)
        call.respondText { json.toString() }
    }
}