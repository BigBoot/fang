package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Upgrades (
        @Json(name="L") val l: Upgrade,
        @Json(name="LL") val ll: Upgrade,
        @Json(name="LR") val lr: Upgrade,
        @Json(name="R") val r: Upgrade,
        @Json(name="RL") val rl: Upgrade,
        @Json(name="RR") val rr: Upgrade
): Serializable {
    operator fun get(path1: UpgradePath, path2: UpgradePath? = null) = when {
        path2 == null && path1 == UpgradePath.Left -> l
        path2 == null && path1 == UpgradePath.Right -> r
        path2 == UpgradePath.Left && path1 == UpgradePath.Left -> ll
        path2 == UpgradePath.Right && path1 == UpgradePath.Left -> lr
        path2 == UpgradePath.Left && path1 == UpgradePath.Right -> rl
        path2 == UpgradePath.Right && path1 == UpgradePath.Right -> rr
        else -> throw RuntimeException("Invalid upgrade path")
    }
}

enum class UpgradePath: Serializable { Left, Right }