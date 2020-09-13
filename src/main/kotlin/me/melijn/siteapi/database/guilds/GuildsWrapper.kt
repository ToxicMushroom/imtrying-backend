package me.melijn.siteapi.database.guilds

import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.objectMapper

class GuildsWrapper(private val guildsDao: GuildsDao) {

    suspend fun getGuildsInfo(jwt: String): GuildsInfo? {
        return guildsDao.getCacheEntry(jwt)?.let {
            objectMapper.readValue(it, GuildsInfo::class.java)
        }
    }

    fun setGuildsInfo(jwt: String, guildsInfo: GuildsInfo) {
        guildsDao.setCacheEntry(
            jwt,
            objectMapper.writeValueAsString(guildsInfo),
            1
        )
    }
}