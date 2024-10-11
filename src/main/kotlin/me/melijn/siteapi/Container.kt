package me.melijn.siteapi

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import me.melijn.siteapi.database.DaoManager
import me.melijn.siteapi.models.PodInfo
import me.melijn.siteapi.router.RestServer
import kotlin.system.exitProcess

class Container {
    val settings: Settings = Settings.initSettings()

    val jwtParser: JwtParser = Jwts.parserBuilder()
        .setSigningKey(settings.restServer.jwtKey)
        .build()

    val daoManager by lazy { DaoManager(settings.redis) }
    val restServer by lazy { RestServer(this) }

    val podInfo: PodInfo by lazy {
        runBlocking {
            try {
                val hostPod0 = settings.melijnApi.host.replace("{podId}", "0")
                httpClient.get("$hostPod0/podinfo").body<PodInfo>()
            } catch (t: Throwable) {
                t.printStackTrace()
                exitProcess(404)
            }
        }
    }
}