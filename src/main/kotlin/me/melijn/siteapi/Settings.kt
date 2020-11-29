package me.melijn.siteapi

import io.github.cdimascio.dotenv.dotenv
import io.jsonwebtoken.io.Decoders

class Settings(
    val redis: Redis,
    val discordOauth: DiscordOauth,
    val restServer: RestServer,
    val recaptcha: GoogleRecaptcha,
    val melijnApi: MelijnApi
) {

    data class GoogleRecaptcha(
        val secret: String
    )

    data class Redis(
        val host: String,
        val port: Int,
        val enabled: Boolean
    )

    data class DiscordOauth(
        val host: String,
        val botId: String,
        val botSecret: String,
        val redirectUrl: String
    )

    data class RestServer(
        val jwtKey: ByteArray
    )

    data class MelijnApi(
        val host: String,
        val token: String
    )


    companion object {
        private val dotenv = dotenv {
            this.filename = System.getenv("ENV_FILE") ?: ".env"
            this.ignoreIfMissing = true
        }

        fun get(path: String): String = dotenv[path.toUpperCase().replace(".", "_")]
            ?: throw IllegalStateException("missing env value: $path")

        private fun getLong(path: String): Long = get(path).toLong()
        private fun getInt(path: String): Int = get(path).toInt()
        private fun getBoolean(path: String): Boolean = get(path).toBoolean()

        fun initSettings(): Settings {

            return Settings(
                Redis(
                    get("redis.host"),
                    getInt("redis.port"),
                    getBoolean("redis.enabled")
                ),
                DiscordOauth(
                    get("discordoauth.host"),
                    get("discordoauth.botid"),
                    get("discordoauth.botsecret"),
                    get("discordoauth.redirecturl")
                ),
                RestServer(
                    Decoders.BASE64.decode(get("restserver.jwtkey"))
                ),
                GoogleRecaptcha(
                    get("googlerecaptcha.secret")
                ),
                MelijnApi(
                    get("melijnapi.host"),
                    get("melijnapi.token")
                )
            )
        }
    }
}