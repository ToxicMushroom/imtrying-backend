package me.melijn.siteapi.router

import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
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
        val call = context.call
        val cfIp = call.request.header("cf-connecting-ip")
        logger.info("req headers: " + call.request.headers.toMap())
        val absoluteIp = cfIp ?: call.request.origin.remoteHost
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
                logger.warn(absoluteIp + ": " + call.request.uri + " (Ratelimited)")
                return true
            }
        }

        reqInfo.requestMap.add(context.now)
        reqInfo.lastTime = context.now
        reqInfo.previousResponse = true

        requestMap[absoluteIp] = reqInfo
        if (shouldLog) logger.info(absoluteIp + ": " + call.request.uri)
        return false
    }
}

