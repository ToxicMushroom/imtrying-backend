package me.melijn.siteapi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.melijn.siteapi.database.DaoManager
import me.melijn.siteapi.models.ContextContainer
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.routes.commands.handleGetCommands
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptPostGuildGeneral
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptPostUserSettings
import me.melijn.siteapi.routes.dashboard.handleGetGeneralSettings
import me.melijn.siteapi.routes.dashboard.handleGetUserSettings
import me.melijn.siteapi.routes.general.handleCookieDecryptGuilds
import me.melijn.siteapi.routes.general.handleCookieDecryptUser
import me.melijn.siteapi.routes.general.handleGetGuild
import me.melijn.siteapi.routes.handleCookieFromCode
import me.melijn.siteapi.routes.raw.handleCookieDecrypt
import me.melijn.siteapi.routes.raw.handleCookieEncrypt
import me.melijn.siteapi.routes.verify.handleVerifyGuilds
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.RateLimitUtils.getValidatedRouteRateLimitNMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val objectMapper = jacksonObjectMapper()
val httpClient = HttpClient()
val jsonType = ContentType.parse("Application/JSON")

class RestServer(settings: Settings, val database: DaoManager) {


    private val jwtParser: JwtParser = Jwts.parserBuilder()
        .setSigningKey(settings.restServer.jwtKey)
        .build()
    private val requestMap = ConcurrentHashMap<String, RateLimitUtils.RequestInfo>()
    private val rateLimitInfo = RateLimitUtils.RateLimitInfo(100, 60_000)
    private val blackList = mutableListOf<String>()
    private val contextContainer = ContextContainer(jwtParser, settings)

    private val server: NettyApplicationEngine = embeddedServer(Netty, 2607, configure = {
        this.runningLimit = 50
        this.requestQueueLimit = 50
    }) {
        intercept(ApplicationCallPipeline.Call) {
            if (getValidatedRouteRateLimitNMessage(
                    RequestContext(contextContainer, call),
                    requestMap,
                    rateLimitInfo,
                    blackList,
                    5
                ) == true
            ) {
                this.proceed()
            } else {
                this.finish()
            }
        }
        routing {
            // full command list
            get("/commands") {
                this.handleGetCommands(RequestContext(contextContainer, call))
            }

            // cookie body -> Cookie
            post("/cookie/encrypt") {
                this.handleCookieEncrypt(RequestContext(contextContainer, call))
            }

            // Cookie -> cookie body
            post("/cookie/decrypt") {
                this.handleCookieDecrypt(RequestContext(contextContainer, call))
            }

            // Cookie -> user info
            post("/cookie/decrypt/user") {
                this.handleCookieDecryptUser(RequestContext(contextContainer, call))
            }

            // Cookie -> user info & settings
            post("/cookie/decrypt/user/settings") {
                this.handleGetUserSettings(RequestContext(contextContainer, call))
            }

            // Cookie -> discord guilds & user info
            post("/cookie/decrypt/guilds") {
                this.handleCookieDecryptGuilds(RequestContext(contextContainer, call))
            }

            // Cookie -> discord guilds & user info
            post("/cookie/decrypt/guild") {
                this.handleGetGuild(RequestContext(contextContainer, call))
            }

            // DISCORD CODE => COMPLETE COOKIE
            post("/cookie/encrypt/code") {
                this.handleCookieFromCode(RequestContext(contextContainer, call))
            }

            // ---=== Verification ===---
            post("/cookie/decrypt/verifyguilds") {
                this.handleVerifyGuilds(RequestContext(contextContainer, call))
            }

            // ---=== SETTINGS ===---
            // Cookie -> discord guilds & user info & general info
            post("/cookie/decrypt/guild/general") {
                this.handleGetGeneralSettings(RequestContext(contextContainer, call))
            }

            // Cookie & general info -> saved or not saved
            post("/postsettings/general") {
                this.handleCookieDecryptPostGuildGeneral(RequestContext(contextContainer, call))
            }

            // Cookie & general info -> saved or not saved
            post("/postsettings/user") {
                this.handleCookieDecryptPostUserSettings(RequestContext(contextContainer, call))
            }
        }
    }

    fun stop() {
        server.stop(0, 2, TimeUnit.SECONDS)
    }

    fun start() {
        server.start(false)
    }
}