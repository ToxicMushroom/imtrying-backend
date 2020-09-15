package me.melijn.siteapi.models

import com.fasterxml.jackson.annotation.JsonProperty

data class GuildsInfo(
    val guilds: List<GuildInfo>
) {
    data class GuildInfo(
        @JsonProperty("id")
        val guildId: String,

        @JsonProperty("name")
        val name: String,

        @JsonProperty("icon")
        val icon: String
    )
}