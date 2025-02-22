package me.melijn.siteapi.database.guilds

import me.melijn.siteapi.models.GuildsInfo
import me.melijn.siteapi.objectMapper

class GuildWrapper(private val guildDao: GuildDao) {

    suspend fun getGuildInfo(jwt: String, guildId: Long): GuildsInfo.GuildInfo? {
        return guildDao.getCacheEntry("$jwt:$guildId")?.let {
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

    fun setGuildInfo(jwt: String, guildId: Long, guildInfo: GuildsInfo.GuildInfo) {
        setGuildInfo(jwt, guildId.toString(), guildInfo)
    }
}