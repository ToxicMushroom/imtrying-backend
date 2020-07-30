package me.melijn.siteapi

class MelijnSite {

    init {
        val restServer = RestServer()
        restServer.start()
    }

}

fun main() {
    MelijnSite()
}