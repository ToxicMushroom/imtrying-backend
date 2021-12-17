package me.melijn.siteapi.router

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.*
import me.melijn.siteapi.Container
import me.melijn.siteapi.Settings
import me.melijn.siteapi.database.DaoManager
import me.melijn.siteapi.models.PodInfo

class RouteContext(
    override val call: ApplicationCall,
    override val container: Container
) : IRouteContext {

    override val settings: Settings = container.settings
    override val podInfo: PodInfo = container.podInfo
    override val daoManager: DaoManager = container.daoManager
    override val response = call.response
    override val request = call.request
    override var body: String = ""
    override val path: String = request.path()
    override val headers: Map<String, String> = request.headers.toMap().map { it.key.lowercase() to it.value.first() }.toMap()
    override val queryParams: Map<String, String> = request.queryParameters.toMap().map { it.key to it.value.first() }.toMap()
    override val contentType: ContentType = request.contentType()

    override val now = System.currentTimeMillis()

    override suspend fun init() {
        body = call.receiveText()
    }
}