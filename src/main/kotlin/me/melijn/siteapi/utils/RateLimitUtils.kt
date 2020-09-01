package me.melijn.siteapi.utils

import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.siteapi.models.RequestContext
import org.slf4j.LoggerFactory
import java.util.*

object RateLimitUtils {

    private val logger = LoggerFactory.getLogger(RateLimitUtils::class.java)

    suspend fun getValidatedRouteRateLimitNMessage(
        context: RequestContext,
        requestMap: MutableMap<String, RequestInfo>,
        rateInf: RateLimitInfo,
        blackList: MutableList<String>? = null,
        blackListThreshold: Int = 5
    ): Boolean? {
        val call = context.call
        val cfIp = call.request.header("cf-connecting-ip")
        val absoluteIp = cfIp ?: call.request.origin.remoteHost
        if (blackList?.contains(absoluteIp) == true) {
            logger.warn("$absoluteIp is a blackListed ip and made a request")
            return null
        }

        val reqInfo = requestMap.getOrDefault(absoluteIp, RequestInfo(0, Collections.synchronizedList(mutableListOf())))

        if (context.now - reqInfo.lastTime < rateInf.timeSpan) {
            val startX = (context.now - rateInf.timeSpan)
            val amount = try {
                val filtered = reqInfo.requestMap.filter { it > startX }
                reqInfo.requestMap = Collections.synchronizedList(filtered)
                filtered.size
            } catch (e: ConcurrentModificationException) {
                rateInf.maxRequests.toInt()
            }
            if (amount >= rateInf.maxRequests) {
                call.respondText("", status = HttpStatusCode.TooManyRequests)
                if (reqInfo.previousResponse) {
                    reqInfo.previousResponse = false
                    reqInfo.rateLimitHits++
                    if (reqInfo.rateLimitHits >= blackListThreshold) {
                        blackList?.add(absoluteIp)
                        reqInfo.requestMap.clear()
                    }
                } else {
                    reqInfo.rateLimitStreak++
                    if (reqInfo.rateLimitStreak >= rateInf.maxRequests) {
                        blackList?.add(absoluteIp)
                        reqInfo.requestMap.clear()
                    }
                }
                reqInfo.requestMap.add(context.now)
                reqInfo.lastTime = context.now
                logger.warn(absoluteIp + ": " + call.request.uri + " (Ratelimited)")
                return null
            }
        }

        reqInfo.requestMap.add(context.now)
        reqInfo.lastTime = context.now
        reqInfo.previousResponse = true

        requestMap[absoluteIp] = reqInfo
        logger.info(absoluteIp + ": " + call.request.uri)
        return true
    }

    data class RequestInfo(
        var lastTime: Long,
        var requestMap: MutableList<Long>,
        var previousResponse: Boolean = true, // True: passed, False: Ratelimited
        var rateLimitHits: Int = 0,
        var rateLimitStreak: Int = 0
    )

    data class RateLimitInfo(
        val maxRequests: Long,
        val timeSpan: Long
    )
}