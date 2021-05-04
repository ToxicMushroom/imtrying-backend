package me.melijn.siteapi.models

data class Shard(
    val id: Int,
    val guildCount: Int,
    val userCount: Int,
    val ping: Int,
    val queuedMessages: Int,
    val responses: Long,
    val connectedVoiceChannels: Int,
    val status: String,
    val listeningVoiceChannels: Int,
    val musicPlayers: Int,
    val queuedTracks: Int,
    val unavailable: Int
)