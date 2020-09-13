package me.melijn.siteapi.models


data class SessionInfo(
    val sessionId: String, // Session ID will be Base58 encoded System.nanoTime()
    val expireTime: Long,
    val userId: Long,
    val oauthToken: String,
    val refreshToken: String
)