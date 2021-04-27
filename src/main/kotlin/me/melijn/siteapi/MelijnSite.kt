package me.melijn.siteapi

import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import me.melijn.siteapi.database.DaoManager
import me.melijn.siteapi.models.PodInfo
import kotlin.system.exitProcess


class MelijnSite {

    init {
        val settings = Settings.initSettings()
        val database = DaoManager(settings.redis)
        val podInfo = runBlocking { fetchPodInfo(settings.melijnApi) }
        val restServer = RestServer(settings, database, podInfo)
        restServer.start()
    }

    private suspend fun fetchPodInfo(melijnApi: Settings.MelijnApi): PodInfo {
        return try {
            httpClient.get(melijnApi.host.replace("{podId}", "0") + "/podinfo")
        } catch (t: Throwable) {
            t.printStackTrace()
            exitProcess(404)
        }
    }

}

fun main() {
    MelijnSite()
}

/* Keygen */
//fun main(args: Array<String>) {
//    val key = Keys.secretKeyFor(SignatureAlgorithm.HS256) //or HS384 or HS512
//    val secretString = Encoders.BASE64.encode(key.encoded)
//    print(secretString)
//}