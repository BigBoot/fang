package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Types
import de.bigboot.ggtools.fang.api.mistforge.MistforgeApi


interface Serializable

inline fun <reified T: Serializable> fromJson(json: String): T? = MistforgeApi.moshi.adapter(T::class.java).fromJson(json)
inline fun <reified T: Serializable> T?.toJson(): String = MistforgeApi.moshi.adapter(T::class.java).toJson(this)


inline fun <reified T: Serializable, reified I: Iterable<T>> fromJsonArray(json: String): I?
        = MistforgeApi.moshi.adapter<I>(Types.newParameterizedType(I::class.java, T::class.java)).fromJson(json)

inline fun <reified T: Serializable, reified I: Iterable<T>> I?.toJsonArray(): String
        = MistforgeApi.moshi.adapter<I>(Types.newParameterizedType(I::class.java, T::class.java)).toJson(this)