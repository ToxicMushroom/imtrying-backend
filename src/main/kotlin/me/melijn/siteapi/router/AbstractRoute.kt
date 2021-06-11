package me.melijn.siteapi.router

import io.ktor.http.*
import org.slf4j.LoggerFactory


abstract class AbstractRoute(val route: String, val httpMethod: HttpMethod = HttpMethod.Get) {

    var authorization: Boolean = false
    var rateLimiter: RateLimiter? = null
    var requiredParameters = emptyArray<String>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun run(context: IRouteContext) {
        context.init()

        if (rateLimiter?.incrementAndGetIsRatelimited(context) == true) return

        // AuthorizationCheck
        if (authorization && context.headers["Authorization"] != context.settings.restServer.authorization) {
            logger.info("${context.headers["Authorization"]} vs ${context.settings.restServer.authorization}")
            context.replyError(HttpStatusCode.Unauthorized)
            return
        }

        // ParameterCheck
        for (requiredParameter in requiredParameters) {
            if (!context.queryParams.containsKey(requiredParameter)) {
                context.replyError(HttpStatusCode.BadRequest, "Missing url parameter: $requiredParameter")
                return
            }
        }

        execute(context)
    }

    abstract suspend fun execute(context: IRouteContext)
}