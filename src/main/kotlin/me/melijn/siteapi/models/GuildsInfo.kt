package me.melijn.siteapi.models

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class GuildsInfo(
    val guilds: List<GuildInfo>
) {
    @Serializable
    data class GuildInfo(
        @JsonProperty("id")
        val guildId: String,

        @JsonProperty("name")
        val name: String,

        @JsonProperty("icon")
        val icon: String?
    )
}