package me.melijn.siteapi.routes

import me.melijn.siteapi.router.AbstractRoute
import me.melijn.siteapi.router.IRouteContext

class RootRoute : AbstractRoute("/") {

    override suspend fun execute(context: IRouteContext) {
        context.reply(
            "welcome to melijn's backend, most of our cool routes are behind authorization :>\n" +
                "If you find any security vulerabilities feel free to report them in my dms or at admin 'ata' melijn 'dotto' com\n" +
                "I'm just trying to block bots with the weird format 🤫"
        )
    }
}