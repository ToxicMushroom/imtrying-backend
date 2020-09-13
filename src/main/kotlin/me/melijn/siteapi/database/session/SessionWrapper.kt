package me.melijn.siteapi.database.session

import me.melijn.siteapi.models.SessionInfo
import me.melijn.siteapi.objectMapper

class SessionWrapper(private val sessionDao: SessionDao) {

    suspend fun getSessionInfo(jwt: String): SessionInfo? {
        return sessionDao.getCacheEntry(jwt)?.let {
            objectMapper.readValue(it, SessionInfo::class.java)
        }
    }

    fun setSessionInfo(jwt: String, sessionInfo: SessionInfo) {
        sessionDao.setCacheEntry(
            jwt,
            objectMapper.writeValueAsString(sessionInfo),
            ((sessionInfo.expireTime - System.currentTimeMillis()) / 60_000).toInt()
        )
    }
}
