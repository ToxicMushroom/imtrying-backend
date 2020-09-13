package me.melijn.siteapi.models

import io.jsonwebtoken.JwtParser
import me.melijn.siteapi.Settings

data class ContextContainer(
    val jwtParser: JwtParser,
    val settings: Settings
)