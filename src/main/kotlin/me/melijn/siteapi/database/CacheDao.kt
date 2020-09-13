package me.melijn.siteapi.database

import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await

abstract class CacheDao(val driverManager: DriverManager) {

    abstract val cacheName: String

    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) {
        val async = driverManager.redisConnection?.async() ?: return
        if (ttlM == null) async.set("$cacheName:$key", value.toString())
        else async.set("$cacheName:$key", value.toString(), SetArgs().ex(ttlM * 60L))
    }

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) {
        val async = driverManager.redisConnection?.async() ?: return
        if (args == null) async.set("$cacheName:$key", value.toString())
        else async.set("$cacheName:$key", value.toString(), args)
    }

    // ttl: minutes
    suspend fun getCacheEntry(key: Any, newTTL: Int? = null): String? {
        val commands = (driverManager.redisConnection?.async() ?: return null)
        val result = commands
            .get("$cacheName:$key")
            .await()
        if (result != null && newTTL != null) {
            commands.expire("$cacheName:$key", newTTL * 60L)
        }
        return result
    }
}
