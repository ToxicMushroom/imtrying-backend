package me.melijn.siteapi.database

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.delay
import me.melijn.siteapi.Settings
import me.melijn.siteapi.threading.TaskManager
import org.slf4j.LoggerFactory

class DriverManager(redisSettings: Settings.Redis) {

    private val logger = LoggerFactory.getLogger(DriverManager::class.java)

    var redisClient: RedisClient? = null
    var redisConnection: StatefulRedisConnection<String, String?>? = null

    init {
        if (redisSettings.enabled) {
            logger.info("Connecting to redis..")
            connectRedis(redisSettings.host, redisSettings.port, redisSettings.password)
        }
    }


    private fun connectRedis(host: String, port: Int, password: String) {
        val uriBuilder = RedisURI.builder()
            .withHost(host)
            .withPort(port)

        val uri = if (password.isNotBlank()){
            uriBuilder.withPassword(password.toCharArray())
        } else {
            uriBuilder
        }.build()

        val redisClient = RedisClient
            .create(uri)

        this.redisClient = redisClient

        try {
            redisConnection = redisClient.connect()
            logger.info("Connected to redis")

        } catch (e: Throwable) {
            TaskManager.async {
                logger.warn("Retrying to connect to redis..")
                recursiveConnectRedis(host, port)
                logger.warn("Retrying to connect to redis has succeeded!")
            }
        }
    }

    private suspend fun recursiveConnectRedis(host: String, port: Int) {
        try {
            redisConnection = redisClient?.connect()
        } catch (e: Throwable) {
            delay(5_000)
            recursiveConnectRedis(host, port)
        }
    }
}