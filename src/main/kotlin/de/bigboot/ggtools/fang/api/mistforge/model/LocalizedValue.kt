package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class LocalizedValue (val de: String, val en: String, val fr: String): Serializable {
    operator fun invoke(lang: LangCode = LangCode.En) = when (lang) {
        LangCode.En -> en
        LangCode.De -> de
        LangCode.Fr -> fr
    }.trim()
}