package me.melijn.siteapi.database

import me.melijn.siteapi.Settings
import me.melijn.siteapi.database.guilds.GuildsDao
import me.melijn.siteapi.database.guilds.GuildsWrapper
import me.melijn.siteapi.database.session.SessionDao
import me.melijn.siteapi.database.session.SessionWrapper
import me.melijn.siteapi.database.user.UserDao
import me.melijn.siteapi.database.user.UserWrapper

class DaoManager(redisSettings: Settings.Redis) {

    var driverManager: DriverManager = DriverManager(redisSettings)

    // Wrappers
    val sessionWrapper: SessionWrapper = SessionWrapper(SessionDao(driverManager))
    val userWrapper: UserWrapper = UserWrapper(UserDao(driverManager))
    val guildsWrapper: GuildsWrapper = GuildsWrapper(GuildsDao(driverManager))

}