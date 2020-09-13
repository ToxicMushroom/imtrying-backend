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