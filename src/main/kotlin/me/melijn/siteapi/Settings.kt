package me.melijn.siteapi

import io.github.cdimascio.dotenv.dotenv
import io.jsonwebtoken.io.Decoders

class Settings(
    val redis: Redis,
    val discordOauth: DiscordOauth,
    val restServer: RestServer,
    val recaptcha: GoogleRecaptcha,
    val melijnApi: MelijnApi,
    val siteIps: List<String>,
    val cfIpRanges: List<String>
) {

    data class GoogleRecaptcha(
        val secret: String
    )

    data class Redis(
        val host: String,
        val password: String,
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
        val port: Int,
        val authorization: String,
        val jwtKey: ByteArray,
        val runningLimit: Int,
        val requestQueueLimit: Int
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

        fun get(path: String): String {
            val fixedPath = path.uppercase().replace(".", "_")
            return dotenv[fixedPath] ?: throw IllegalStateException("missing env value: $fixedPath")
        }

        private fun getLong(path: String): Long = get(path).toLong()
        private fun getInt(path: String): Int = get(path).toInt()
        private fun getBoolean(path: String): Boolean = get(path).toBoolean()

        fun initSettings(): Settings {

            return Settings(
                Redis(
                    get("redis.host"),
                    get("redis.password"),
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
                    getInt("restserver.port"),
                    get("restserver.authorization"),
                    Decoders.BASE64.decode(get("restserver.jwtkey")),
                    getInt("restserver.runninglimit"),
                    getInt("restserver.requestqueuelimit"),
                ),
                GoogleRecaptcha(
                    get("googlerecaptcha.secret")
                ),
                MelijnApi(
                    get("melijnapi.host.pattern"),
                    get("melijnapi.token")
                ),
                get("website.ips").takeIf { it.isNotBlank() }?.split(",") ?: emptyList(),
                get("cf.ips").takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
            )
        }
    }
}
