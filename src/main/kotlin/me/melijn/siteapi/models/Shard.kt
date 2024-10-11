package me.melijn.siteapi.models

import kotlinx.serialization.Serializable

@Serializable
data class Shard(
    val id: Int,
    val guildCount: Int,
    val userCount: Int,
    val ping: Int,
    val responses: Long,
    val status: String,
    val unavailable: Int
)