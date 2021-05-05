package me.melijn.siteapi.routes.verify

import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.request.*
import io.ktor.http.*
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext
import me.melijn.siteapi.router.RateLimiter
import me.melijn.siteapi.router.post
import me.melijn.siteapi.utils.getUserInfo
import me.melijn.siteapi.utils.json

class GetVerifiableGuildsRoute : AbstractRoute("/cookie/decrypt/verifyguilds", HttpMethod.Post) {

    init {
        rateLimiter = RateLimiter(3, 5)
    }

    override suspend fun execute(context: IRouteContext) {
        val userInfo = getUserInfo(context) ?: return

        val allGuilds = objectMapper.createArrayNode()
        for (id in context.getPodIds()) {
            val base = context.melijnHostPattern.replace("{podId}", "$id")
            val melijnGuilds = context.post<String>("${base}/unverified/guilds") {
                this.body = userInfo.idLong.toString()
                headers {
                    append("Authorization", context.melijnApiKey)
                }
            }?.json()

            if (melijnGuilds is ArrayNode)
                allGuilds.addAll(melijnGuilds)
        }

        context.replyJson {
            set("guilds", allGuilds)
        }
    }
}