package me.melijn.siteapi.routes

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.melijnApi
import me.melijn.siteapi.melijnApiKey
import me.melijn.siteapi.models.RequestContext
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.utils.getJWTPayloadNMessage
import me.melijn.siteapi.utils.getPostBodyNMessage


suspend fun PipelineContext<Unit, ApplicationCall>.handleCookieDecryptGuild(context: RequestContext) {
    val postBody = getPostBodyNMessage(call) ?: return

    val jwt = postBody.get("jwt")?.asText() ?: return

    val json = getJWTPayloadNMessage(context, jwt) ?: return
    val guildId = postBody.get("id")?.asText() ?: return


    // Includes info like: is melijn a member, does the user have permission to the dashboard
    val melijnGuild = objectMapper.readTree(
        httpClient.post<String>("$melijnApi/guild/$guildId") {
            this.body = json.get("id").asText()
            this.headers {
                this.append("Authorization", "Bearer $melijnApiKey")
            }
        }
    )

    val node = objectMapper.createObjectNode()
        .put("name", melijnGuild.get("name").asText())
        .put("isGif", melijnGuild.get("icon").asText().startsWith("a_"))
        .put("icon", melijnGuild.get("icon").asText())
        .put("id", guildId)

    call.respondText(node.toString())

}
