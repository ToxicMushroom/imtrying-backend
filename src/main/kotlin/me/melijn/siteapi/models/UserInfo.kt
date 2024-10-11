package me.melijn.siteapi.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val idLong: Long,
    val userName: String,
    val discriminator: String,
    val avatar: String
)