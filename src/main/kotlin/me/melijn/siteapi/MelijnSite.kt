package me.melijn.siteapi

import me.melijn.siteapi.database.DriverManager

class MelijnSite {

    init {
        val settings = Settings.initSettings()
        val database = DriverManager(settings.redis)
        val restServer = RestServer(settings, database)
        restServer.start()
    }

}

fun main() {
    MelijnSite()
}