package me.melijn.siteapi.router

data class RequestInfo(
    var lastTime: Long,
    var requestMap: MutableList<Long>,
    var previousResponse: Boolean = true, // True: passed, False: Ratelimited
    var rateLimitHits: Int = 0,
    var rateLimitStreak: Int = 0
)