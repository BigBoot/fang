package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Upgrade(val name: LocalizedValue, val description: LocalizedValue): Serializable