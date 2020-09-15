package me.melijn.siteapi.models

import io.jsonwebtoken.JwtParser
import me.melijn.siteapi.Settings
import me.melijn.siteapi.database.DaoManager

data class ContextContainer(
    val jwtParser: JwtParser,
    val settings: Settings,
    val daoManager: DaoManager
)