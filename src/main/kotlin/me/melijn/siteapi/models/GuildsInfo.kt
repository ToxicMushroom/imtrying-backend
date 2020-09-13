package me.melijn.siteapi.models

data class GuildsInfo(
    val guilds: List<GuildInfo>
) {
    data class GuildInfo(
        val guildId: Long,
        val name: String,
        val avatar: String
    )
}