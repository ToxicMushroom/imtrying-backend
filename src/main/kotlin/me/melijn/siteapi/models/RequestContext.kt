package me.melijn.siteapi.models

import io.jsonwebtoken.JwtParser
import io.ktor.application.*

data class RequestContext(
    val jwtParser: JwtParser,
    val call: ApplicationCall
)