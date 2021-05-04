package me.melijn.siteapi.router

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import me.melijn.siteapi.Container
import me.melijn.siteapi.routes.CreateCookieRoute
import me.melijn.siteapi.routes.RootRoute
import me.melijn.siteapi.routes.commands.CommandMapRoute
import me.melijn.siteapi.routes.commands.CommandsRoute
import me.melijn.siteapi.routes.dashboard.GetGuildPageInfoRoute
import me.melijn.siteapi.routes.dashboard.guild.GeneralRoute
import me.melijn.siteapi.routes.dashboard.guild.LoggingRoute
import me.melijn.siteapi.routes.dashboard.guild.StarboardRoute
import me.melijn.siteapi.routes.dashboard.user.UserRoute
import me.melijn.siteapi.routes.general.GetLoginGuildsRoute
import me.melijn.siteapi.routes.general.GetLoginInfoRoute
import me.melijn.siteapi.routes.stats.EventsRoute
import me.melijn.siteapi.routes.stats.GetPublicStatsRoute
import me.melijn.siteapi.routes.verify.GetVerifiableGuildsRoute
import me.melijn.siteapi.routes.verify.VerifyGuildRoute

class RoutingClient(private val container: Container) {

    private val rateLimiter = RateLimiter(100, 60)
    private val routes: List<AbstractRoute> = listOf(
        RootRoute(),
        CommandsRoute(),
        CommandMapRoute(),
        GetPublicStatsRoute(),
        EventsRoute(),

        CreateCookieRoute(),

        VerifyGuildRoute(),
        GetVerifiableGuildsRoute(),

        GetLoginInfoRoute(),
        GetLoginGuildsRoute(),
        GetGuildPageInfoRoute(),

        // User dashboard routes
        UserRoute.Set(),
        UserRoute.Get(),

        // Guild dashboard routes
        GeneralRoute.Set(),
        GeneralRoute.Get(),
        LoggingRoute.Set(),
        LoggingRoute.Get(),
        StarboardRoute.Set(),
        StarboardRoute.Get()
    )

    fun setApplication(application: Application) {
        application.intercept(ApplicationCallPipeline.Call) {
            val ratelimited = rateLimiter.incrementAndGetIsRatelimited(
                RouteContext(this.context, container),
                shouldBlackList = true,
                blackListThreshold = 5,
                shouldLog = true
            )
            if (ratelimited) {
                this.finish()
                return@intercept
            }
            this.proceed()
        }
        application.routing {
            setRouting(this)
        }
    }

    private fun setRouting(routing: Routing) {
        for (route in routes) {
            val body: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
                val context = RouteContext(this.context, container)
                route.run(context)
            }
            when (route.httpMethod) {
                HttpMethod.Get -> routing.get(route.route, body)
                HttpMethod.Post -> routing.post(route.route, body)
                HttpMethod.Put -> routing.put(route.route, body)
                HttpMethod.Patch -> routing.patch(route.route, body)
                HttpMethod.Delete -> routing.delete(route.route, body)
                HttpMethod.Head -> routing.head(route.route, body)
                HttpMethod.Options -> routing.options(route.route, body)
            }
        }
    }

}