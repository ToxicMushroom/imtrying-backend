package me.melijn.siteapi.router

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.melijn.siteapi.Container
import java.util.concurrent.TimeUnit

class RestServer(private val container: Container) {

    val settings = container.settings.restServer
    private val server: NettyApplicationEngine = embeddedServer(Netty, settings.port, configure = {
        this.runningLimit = settings.runningLimit
        this.requestQueueLimit = settings.requestQueueLimit
    }) {
        val builder = RoutingClient(container)
        builder.setApplication(this)
    }

    fun stop() {
        server.stop(0, 2, TimeUnit.SECONDS)
    }

    fun start() {
        server.start(false)
    }
}