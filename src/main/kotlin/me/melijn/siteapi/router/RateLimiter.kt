package me.melijn.siteapi.router

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.netty.*
import io.ktor.server.request.header
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingApplicationCall
import io.ktor.util.*
import org.slf4j.LoggerFactory
import org.springframework.security.web.util.matcher.IpAddressMatcher
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RateLimiter(
    private val maxRequests: Long,
    timeSpan: Long,
    timeUnit: TimeUnit = TimeUnit.SECONDS
) {
    private val timeSpanMillis = TimeUnit.MILLISECONDS.convert(timeSpan, timeUnit)

    private val requestMap = ConcurrentHashMap<String, RequestInfo>()
    private val blackList = mutableListOf<String>()
    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)

    suspend fun incrementAndGetIsRatelimited(
        context: IRouteContext,
        shouldLog: Boolean = false,
        shouldBlackList: Boolean = false,
        blackListThreshold: Int = 5
    ): Boolean {
        val requesterIP = getAccurateRequesterIP(context)

        val call = context.call
        val cfConnectingIp = call.request.header("Cf-Connecting-Ip")
        logger.info("┌━New Req--")
        logger.info("┃ Req Headers: " + context.call.request.headers.toMap())
        val headerRequesterIp = if (cfConnectingIp != null && context.settings.siteIps.contains(cfConnectingIp))
        // Check if this is an authentic cf server that is setting the header
            if (isCFIp(context, requesterIP)) {
                getRequestIpFromMelijnHeader(call, context)
            } else {
                logger.warn("┃ $requesterIP IS FAKING melijn-requester-ip AND CF-connecting-IP HEADERS, OR THIS IS A NEW CF SERVER")
                null
            }

        // Not CF, but raw request from site server (useful for local, or no cf setups)
        else if (cfConnectingIp == null && context.settings.siteIps.contains(requesterIP)) {
            getRequestIpFromMelijnHeader(call, context)
        } else call.request.header("Cf-Connecting-Ip")


        val absoluteIp = headerRequesterIp ?: requesterIP
        if (shouldBlackList && blackList.contains(absoluteIp)) {
            logger.warn("$absoluteIp is a blackListed ip and made a request")
            return true
        }

        val default = RequestInfo(0, Collections.synchronizedList(mutableListOf()))
        val reqInfo = requestMap.getOrDefault(absoluteIp, default)

        if (context.now - reqInfo.lastTime < timeSpanMillis) {
            val startX = (context.now - timeSpanMillis)
            val amount = try {
                val filtered = reqInfo.requestMap.filter { it > startX }
                reqInfo.requestMap = Collections.synchronizedList(filtered)
                filtered.size
            } catch (e: ConcurrentModificationException) {
                maxRequests.toInt()
            }

            if (amount >= maxRequests) {
                call.respondText("", status = HttpStatusCode.TooManyRequests)
                if (reqInfo.previousResponse) {
                    reqInfo.previousResponse = false
                    reqInfo.rateLimitHits++
                    if (reqInfo.rateLimitHits >= blackListThreshold) {
                        blackList.add(absoluteIp)
                        reqInfo.requestMap.clear()
                    }
                } else {
                    reqInfo.rateLimitStreak++
                    if (reqInfo.rateLimitStreak >= maxRequests) {
                        blackList.add(absoluteIp)
                        reqInfo.requestMap.clear()
                    }
                }
                reqInfo.requestMap.add(context.now)
                reqInfo.lastTime = context.now
                logger.warn("┃ " + absoluteIp + ": " + call.request.uri + " (Ratelimited)")
                return true
            }
        }

        reqInfo.requestMap.add(context.now)
        reqInfo.lastTime = context.now
        reqInfo.previousResponse = true

        requestMap[absoluteIp] = reqInfo
        if (shouldLog) logger.info("┃ $absoluteIp: ${context.request.httpMethod.value} ${call.request.uri}")
        return false
    }

    private fun getRequestIpFromMelijnHeader(
        call: ApplicationCall,
        context: IRouteContext
    ): String? {
        val phase1 = call.request.header("Melijn-X-Real-Ip")
        return if (phase1 != null && isCFIp(context, phase1)) {
            call.request.header("Melijn-Requester-Ip")
        } else if (phase1 != null) {
            logger.warn("┃ Someone made a direct request to traefik, (no cf). Using his ip")
            call.request.header("Melijn-X-Real-Ip")
        } else {
            logger.error("┃ Request from website but no ip headers were supplied!")
            null
        }
    }

    private fun isCFIp(context: IRouteContext, requesterIP: String) =
        context.settings.cfIpRanges.any { IpAddressMatcher(it).matches(requesterIP) }

    private fun getAccurateRequesterIP(context: IRouteContext) = when (context.call) {
        is NettyApplicationCall -> {
            val call = context.call as NettyApplicationCall
            val requesterIP1: InetSocketAddress = call.context.pipeline().channel().remoteAddress() as InetSocketAddress
            requesterIP1.address.toString().dropWhile { c -> c == '/' }
        }

        is RoutingApplicationCall -> {
            val call = context.call as RoutingApplicationCall
            call.request.host()
        }

        else -> {
            context.call.request.host()
        }
    }
}

