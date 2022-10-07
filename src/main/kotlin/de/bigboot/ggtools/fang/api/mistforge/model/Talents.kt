package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.math.max
import kotlin.math.min


@JsonClass(generateAdapter = true)
data class Talents(@Json(name="5") val _internal: List<Talent>): Serializable {
    val talent1 get() = _internal[0]
    val talent2 get() = _internal[1]
    val talent3 get() = _internal[2]

    operator fun get(index: Int) = _internal[max(0, min(2, index))]
}