package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Json

enum class LangCode: Serializable {
    @Json(name="eng") En,
    @Json(name="ger") De,
    @Json(name="fra") Fr
}