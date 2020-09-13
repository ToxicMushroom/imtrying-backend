package me.melijn.siteapi.models

data class UserInfo(
    val userName: String,
    val discriminator: String,
    val avatar: String
)