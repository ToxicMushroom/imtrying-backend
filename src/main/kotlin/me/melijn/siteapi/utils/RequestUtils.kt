package me.melijn.siteapi.utils

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.models.SessionInfo
import me.melijn.siteapi.models.UserInfo
import me.melijn.siteapi.objectMapper
import me.melijn.siteapi.router.IRouteContext
import kotlin.reflect.jvm.jvmName

suspend fun getBodyNMessage(context: IRouteContext): JsonNode? {
    return try {
        objectMapper.readTree(context.body)
    } catch (t: Throwable) {
        t.printStackTrace()
        context.replyError(HttpStatusCode.BadRequest)
        null
    }
}

suspend fun validateJWTNMessage(context: IRouteContext, jwt: String): Boolean? {
    try {
        context.container.jwtParser.parsePlaintextJws(jwt).body // Parser knows about key and will validate contents
    } catch (e: Throwable) {
        e.printStackTrace()
        context.replyError(HttpStatusCode.Unauthorized)
        return null
    }

    return true
}

suspend fun getJWTNMessage(context: IRouteContext, validate: Boolean = true): String? {
    val postBody = getBodyNMessage(context) ?: return null
    val jwt = postBody.getSafeString("jwt", context) ?: return null
    if (validate) validateJWTNMessage(context, jwt) ?: return null
    return jwt
}

suspend fun getUserInfo(context: IRouteContext): UserInfo? {
    val jwt = getJWTNMessage(context) ?: return null
    return context.daoManager.userWrapper.getUserInfo(jwt)
}

suspend fun getGuildId(context: IRouteContext): Long? {
    val body = getBodyNMessage(context) ?: return null
    return getSafeLong("id", body, context)
}

suspend fun getGuildInfo(context: IRouteContext): GuildsInfo.GuildInfo? {
    val jwt = getJWTNMessage(context) ?: return null
    val guildId = getGuildId(context) ?: return null
    return context.daoManager.guildWrapper.getGuildInfo(jwt, guildId)
}

suspend fun getSessionInfo(context: IRouteContext): SessionInfo? {
    val jwt = getJWTNMessage(context) ?: return null
    return context.daoManager.sessionWrapper.getSessionInfo(jwt)
}


fun String.json(): JsonNode {
    return objectMapper.readTree(this)
}

suspend fun JsonNode.getSafeNode(key: String, context: IRouteContext): JsonNode? {
    val value = try {
        this.get(key)
    } catch (T: Throwable) {
        null
    }
    if (value == null) {
        context.replyError(HttpStatusCode.BadRequest, "Bad Request\n$key is missing from body or null")
        return null
    }

    return value
}

suspend fun JsonNode.getSafeString(key: String, context: IRouteContext): String? {
    val value: String? = try {
        this.get(key)?.asText()
    } catch (T: Throwable) {
        null
    }
    if (value == null) {
        context.replyError(HttpStatusCode.BadRequest, "Bad Request\n$key is missing from body or null")
        return null
    }

    return value
}

suspend inline fun <reified T> JsonNode.getSafeType(key: String, context: IRouteContext, transform: (String) -> T?): T? {
    val value: String = this.getSafeString(key, context) ?: return null
    val result = transform(value)
    if (result == null) {
        val reason = "Bad Request\n$key couldn't be transformed into ${T::class.jvmName}"
        context.replyError(HttpStatusCode.BadRequest, reason)
        return null
    }
    return result
}

private suspend fun getSafeLong(
    key: String,
    body: JsonNode,
    context: IRouteContext
) = body.getSafeType(key, context) { it.toLongOrNull() }