package me.melijn.siteapi.routes.general

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.getPostBodyNMessage
import me.melijn.siteapi.utils.validateJWTNMessage


suspend fun PipelineContext<Unit, ApplicationCall>.handleGetGuild(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    validateJWTNMessage(context, jwt) ?: return
    val guildId = postBody.get("id")?.asText() ?: return

    val userInfo = context.daoManager.userWrapper.getUserInfo(jwt) ?: return
    val guildInfoN = context.daoManager.guildWrapper.getGuildInfo(jwt)
    val guildInfo = if (guildInfoN == null) {
        // Includes info like: is melijn a member, does the user have permission to the dashboard
        val melijnGuild = httpClient.post<String>("${context.melijnApi}/guild/$guildId") {
            this.body = userInfo.idLong.toString()
            this.headers {
                this.append("Authorization", "Bearer ${context.melijnApiKey}")
            }
        }

        val guildInf = objectMapper.readValue<GuildsInfo.GuildInfo>(melijnGuild)
        context.daoManager.guildWrapper.setGuildInfo(jwt, guildId, guildInf)
        guildInf
    } else {
        guildInfoN
    }

    val node = objectMapper.createObjectNode()
        .put("name", guildInfo.name)
        .put("isGif", guildInfo.icon.startsWith("a_"))
        .put("icon", guildInfo.icon)
        .put("id", guildId)

    call.respondText(node.toString())

}
