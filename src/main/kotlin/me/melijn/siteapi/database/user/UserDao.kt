package me.melijn.siteapi.database.user

import me.melijn.siteapi.database.CacheDao
import me.melijn.siteapi.database.DriverManager

class UserDao(driverManager: DriverManager) : CacheDao(driverManager) {

    override val cacheName: String = "user"

}