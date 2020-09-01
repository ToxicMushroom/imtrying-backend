package me.melijn.siteapi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.routes.*
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptGuildGeneral
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptPostGuildGeneral
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptPostUserSettings
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptUserSettings
import me.melijn.siteapi.utils.RateLimitUtils
import me.melijn.siteapi.utils.RateLimitUtils.getValidatedRouteRateLimitNMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val objectMapper = jacksonObjectMapper()
val httpClient = HttpClient()
const val discordApi = "https://discord.com/api/v6"
val melijnApi: String = System.getenv("MELIJN_API")
val melijnApiKey: String = System.getenv("MELIJN_API_KEY")
val keyString: ByteArray = Decoders.BASE64.decode(System.getenv("KEY"))
val jsonType = ContentType.parse("Application/JSON")

class RestServer {
    private val jwtParser: JwtParser = Jwts.parserBuilder()
        .setSigningKey(keyString)
        .build()
    private val logger = LoggerFactory.getLogger(RestServer::class.java)

    private val requestMap = ConcurrentHashMap<String, RateLimitUtils.RequestInfo>()
    private val rateLimitInfo = RateLimitUtils.RateLimitInfo(100, 60_000)
    private val blackList = mutableListOf<String>()

    private val server: NettyApplicationEngine = embeddedServer(Netty, 2607, configure = {
        this.runningLimit = 50
        this.requestQueueLimit = 50
    }) {
        intercept(ApplicationCallPipeline.Call) {
//            call.request.headers.forEach { s, list ->
//                println("'$s': " + list.joinToString("', '", "'", "'") { it })
//            }
            if (getValidatedRouteRateLimitNMessage(
                    RequestContext(jwtParser, call),
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
                this.handleCommands()
            }

            // cookie body -> Cookie
            post("/cookie/encrypt") {
                this.handleCookieEncrypt()
            }

            // Cookie -> cookie body
            post("/cookie/decrypt") {
                this.handleCookieDecrypt(RequestContext(jwtParser, call))
            }

            // Cookie -> user info
            post("/cookie/decrypt/user") {
                this.handleCookieDecryptUser(RequestContext(jwtParser, call))
            }

            // Cookie -> user info & settings
            post("/cookie/decrypt/user/settings") {
                this.handleCookieDecryptUserSettings(RequestContext(jwtParser, call))
            }

            // Cookie -> discord guilds & user info
            post("/cookie/decrypt/guilds") {
                this.handleCookieDecryptGuilds(RequestContext(jwtParser, call))
            }

            // Cookie -> discord guilds & user info
            post("/cookie/decrypt/guild") {
                this.handleCookieDecryptGuild(RequestContext(jwtParser, call))
            }

            // DISCORD CODE => COMPLETE COOKIE
            post("/cookie/encrypt/code") {
                this.handleCookieEncryptCode(RequestContext(jwtParser, call))
            }

            // ---=== SETTINGS ===---
            // Cookie -> discord guilds & user info & general info
            post("/cookie/decrypt/guild/general") {
                this.handleCookieDecryptGuildGeneral(RequestContext(jwtParser, call))
            }

            // Cookie & general info -> saved or not saved
            post("/postsettings/general") {
                this.handleCookieDecryptPostGuildGeneral(RequestContext(jwtParser, call))
            }

            // Cookie & general info -> saved or not saved
            post("/postsettings/user") {
                this.handleCookieDecryptPostUserSettings(RequestContext(jwtParser, call))
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