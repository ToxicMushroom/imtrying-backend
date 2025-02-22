package me.melijn.siteapi.models

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class PodInfo(
    @JsonProperty("podId")
    val podId: Int,
    @JsonProperty("podCount")
    val podCount: Int,
    @JsonProperty("shardCount")
    val shardCount: Int
) {
    val shardsPerPod: Int = shardCount / podCount
    val minShardId: Int = podId * shardsPerPod
    val maxShardId: Int = podId * shardsPerPod + shardsPerPod - 1
    val shardList: List<Int> = List(shardsPerPod) { index -> index + minShardId }
}