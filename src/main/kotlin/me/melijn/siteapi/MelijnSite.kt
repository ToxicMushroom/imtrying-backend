package me.melijn.siteapi

import me.melijn.siteapi.database.DaoManager


class MelijnSite {

    init {
        val settings = Settings.initSettings()
        val database = DaoManager(settings.redis)
        val restServer = RestServer(settings, database)
        restServer.start()
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