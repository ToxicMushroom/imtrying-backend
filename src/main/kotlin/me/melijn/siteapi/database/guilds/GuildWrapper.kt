package me.melijn.siteapi.database.guilds

import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.objectMapper

class GuildWrapper(private val guildDao: GuildDao) {

    suspend fun getGuildInfo(jwt: String): GuildsInfo.GuildInfo? {
        return guildDao.getCacheEntry(jwt)?.let {
            objectMapper.readValue(it, GuildsInfo.GuildInfo::class.java)
        }
    }

    fun setGuildInfo(jwt: String, guildId: String, guildInfo: GuildsInfo.GuildInfo) {
        guildDao.setCacheEntry(
            "$jwt:$guildId",
            objectMapper.writeValueAsString(guildInfo),
            5
        )
    }
}