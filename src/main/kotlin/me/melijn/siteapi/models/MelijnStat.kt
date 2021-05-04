package me.melijn.siteapi.models

data class MelijnStat(
    var bot: BotStat,
    var server: ServerStat,
    var shards: List<Shard>
) {
    data class BotStat(
        var uptime: Long,
        var melijnThreads: Int,
        var ramUsage: Long,
        var ramTotal: Long,
        var jvmThreads: Int,
        var cpuUsage: Double
    )

    data class ServerStat(
        var uptime: Long,
        var ramUsage: Long,
        var ramTotal: Long
    )
}