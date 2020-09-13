package me.melijn.siteapi.database.session

import me.melijn.siteapi.database.CacheDao
import me.melijn.siteapi.database.DriverManager

class SessionDao(driverManager: DriverManager) : CacheDao(driverManager) {
    override val cacheName: String = "jwt"
}