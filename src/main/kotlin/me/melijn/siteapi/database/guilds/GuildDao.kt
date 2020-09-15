package me.melijn.siteapi.database.guilds

import me.melijn.siteapi.database.CacheDao
import me.melijn.siteapi.database.DriverManager

class GuildDao(driverManager: DriverManager) : CacheDao(driverManager) {
    override val cacheName: String ="guild"
}