package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Heroes(val patch: Int, val isCore: Boolean, val heroes: List<Hero>): Serializable