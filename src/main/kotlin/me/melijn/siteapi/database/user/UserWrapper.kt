package me.melijn.siteapi.database.user

import me.melijn.siteapi.models.UserInfo
import me.melijn.siteapi.objectMapper

class UserWrapper(private val userDao: UserDao) {

    suspend fun getUserInfo(jwt: String): UserInfo? {
        return userDao.getCacheEntry(jwt)?.let {
            objectMapper.readValue(it, UserInfo::class.java)
        }
    }

    fun setUserInfo(jwt: String, userInfo: UserInfo, lifeTime: Long) {
        userDao.setCacheEntry(
            jwt,
            objectMapper.writeValueAsString(userInfo),
            (lifeTime / 60_000).toInt()
        )
    }
}