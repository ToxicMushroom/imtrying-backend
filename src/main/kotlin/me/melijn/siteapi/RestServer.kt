package me.melijn.siteapi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.io.Decoders
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import me.melijn.siteapi.routes.*
import me.melijn.siteapi.routes.dashboard.handleCookieDecryptGuildGeneral
import java.util.concurrent.TimeUnit

val objectMapper = jacksonObjectMapper()
val httpClient = HttpClient()
val discordApi = "https://discord.com/api/v6"
val melijnApi = System.getenv("MELIJN_API")
val melijnApiKey = System.getenv("MELIJN_API_KEY")
val keyString = Decoders.BASE64.decode(System.getenv("KEY"))
val jsonType = ContentType.parse("Application/JSON")

class RestServer {

    private val server: NettyApplicationEngine = embeddedServer(Netty, 2607) {
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
                this.handleCookieDecrypt()
            }

            // Cookie -> user info
            post("/cookie/decrypt/user") {
                this.handleCookieDecryptUser()
            }

            // Cookie -> discord guilds & user info
            post("/cookie/decrypt/guilds") {
                this.handleCookieDecryptGuilds()
            }

            // Cookie -> discord guilds & user info
            post("/cookie/decrypt/guild") {
                this.handleCookieDecryptGuild()
            }

            // Cookie -> discord guilds & user info & general info
            post("/cookie/decrypt/guild/general") {
                this.handleCookieDecryptGuildGeneral()
            }

            // DISCORD CODE => COMPLETE COOKIE
            post("/cookie/encrypt/code") {
                this.handleCookieEncryptCode()
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