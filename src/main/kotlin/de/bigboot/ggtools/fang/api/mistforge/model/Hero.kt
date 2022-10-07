package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Hero (
    val fullName: LocalizedValue,
    val description: LocalizedValue,
    val flavorText: LocalizedValue,
    val lore: LocalizedValue,
    val shortName: String,
    val attackStat: Int,
    val defenseStat: Int,
    val mobilityStat: Int,
    val utilityStat: Int,
    val playDifficulty:Int,
    val playStyle: Int,
    val skills: Skills,
    val talents: Talents
): Serializable {
    val heroId get() = when(shortName) {
        "Aisling" -> 0
        "Oncle Sven" -> 1
        "Vadasi" -> 2
        "Beckett" -> 3
        "Charnok" -> 4
        "Xenobia" -> 5
        "Pakko" -> 6
        "Wu" -> 7
        "Zandora" -> 8
        "HK-206" -> 9
        "Seigneur Knossos" -> 10
        "Griselma" -> 11
        "Imani" -> 12
        "Tripp" -> 13
        "Tyto le vif" -> 14
        "Le Margrave" -> 15
        "Voden" -> 16
        "Mozu" -> 17
        "Ramsay" -> 18
        else -> 0
    }
}