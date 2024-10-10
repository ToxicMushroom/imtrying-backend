package me.melijn.siteapi.router

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import me.melijn.siteapi.Container
import me.melijn.siteapi.Settings
import me.melijn.siteapi.database.DaoManager
import me.melijn.siteapi.httpClient
import me.melijn.siteapi.models.PodInfo
import me.melijn.siteapi.objectMapper
import kotlin.random.Random

interface IRouteContext {

    val settings: Settings

    val call: ApplicationCall
    val response: ApplicationResponse
    val request: ApplicationRequest
    val path: String
    var body: String
    val headers: Map<String, String>
    val queryParams: Map<String, String>
    val contentType: ContentType

    val container: Container
    val daoManager: DaoManager
    val podInfo: PodInfo

    val now: Long

    suspend fun init()

    suspend fun reply(
        content: String,
        contentType: ContentType = ContentType.Text.Plain,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ) {
        response.header("Access-Control-Allow-Origin", "*")
        call.respondText(content, contentType, statusCode)
    }

    suspend fun replyJson(
        json: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ) {
        reply(json, ContentType.Application.Json, statusCode)
    }

    suspend fun replyJson(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        builder: ObjectNode.() -> Any
    ) {
        val node = objectMapper.createObjectNode()
        builder(node)
        reply(node.toString(), ContentType.Application.Json, statusCode)
    }

    suspend fun replyError(
        statusCode: HttpStatusCode,
        reason: String = "${statusCode.value} - ${statusCode.description}"
    ) {
        replyJson(statusCode) {
            put("error", reason)
        }
    }

    val melijnHostPattern: String
        get() = settings.melijnApi.host

    val melijnApiKey: String
        get() = settings.melijnApi.token

    val discordApi: String
        get() = settings.discordOauth.host


    fun getMelijnHost(guildId: Long): String {
        val shardId = ((guildId ushr 22) % podInfo.shardCount).toInt()
        val podId = shardId / podInfo.shardsPerPod
        return getPodHostUrl(podId)
    }

    fun getRandomHost(): String {
        val podId = Random.nextInt(podInfo.podCount)
        return getPodHostUrl(podId)
    }

    fun getPodIds(): IntRange = 0 until podInfo.podCount

    fun getQueryParm(key: String): String {
        return queryParams[key].toString()
    }

    fun getPodHostUrl(id: Int): String {
        return melijnHostPattern.replace("{podId}", "$id")
    }

}

suspend inline fun <reified T> IRouteContext.post(url: String, function: HttpRequestBuilder.() -> Unit = {}): T? {
    return try {
        httpClient.post(url, function).body()
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

suspend inline fun <reified T> IRouteContext.get(url: String, function: HttpRequestBuilder.() -> Unit = {}): T? {
    return try {
        httpClient.get(url, function).body()
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}