package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Skill (val name: LocalizedValue, val description: LocalizedValue, val upgrades: Upgrades): Serializable