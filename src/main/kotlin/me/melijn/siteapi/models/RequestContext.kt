package me.melijn.siteapi.models

import io.ktor.application.*

data class RequestContext(
    private val contextContainer: ContextContainer,
    val call: ApplicationCall,
    val now: Long = System.currentTimeMillis()
) {
    val discordApi = contextContainer.settings.discordOauth.host
    val melijnApi = contextContainer.settings.melijnApi.host
    val melijnApiKey = contextContainer.settings.melijnApi.token
    val recaptchaSecret = contextContainer.settings.recaptcha.secret
    val jwtParser = contextContainer.jwtParser
    val jwtKey = contextContainer.settings.restServer.jwtKey

    val settings = contextContainer.settings

    val daoManager = contextContainer.daoManager
}