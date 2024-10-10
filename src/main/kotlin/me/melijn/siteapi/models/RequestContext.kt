package me.melijn.siteapi.models


import io.ktor.server.application.ApplicationCall
import kotlin.random.Random

data class RequestContext(
    private val contextContainer: ContextContainer,
    val call: ApplicationCall,
    val now: Long = System.currentTimeMillis()
) {
    val discordApi = contextContainer.settings.discordOauth.host

    fun getMelijnHost(guildId: Long): String {
        val podId = ((guildId ushr 22) % contextContainer.podInfo.shardCount)
        return hostPattern.replace("{podId}", "$podId")
    }

    fun getRandomHost(): String {
        val podId = Random.nextInt(contextContainer.podInfo.podCount)
        return hostPattern.replace("{podId}", "$podId")
    }

    fun getPodIds(): IntRange = 0 until contextContainer.podInfo.podCount

    val hostPattern = contextContainer.settings.melijnApi.host
    val melijnApiKey = contextContainer.settings.melijnApi.token
    val recaptchaSecret = contextContainer.settings.recaptcha.secret
    val jwtParser = contextContainer.jwtParser
    val jwtKey = contextContainer.settings.restServer.jwtKey

    val settings = contextContainer.settings

    val daoManager = contextContainer.daoManager
}