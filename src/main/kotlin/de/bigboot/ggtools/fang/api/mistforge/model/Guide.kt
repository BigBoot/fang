package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class Guide (
        val userId: Int,
        val username: String,
        val heroId: Int,
        val skills: String,
        val talent: Int,
        val creatures: String,
        val title: String,
        val role: String,
        val guide: String,
        val postDate: Date,
        val lastUpdate: Date?,
        val upvotes: Int,
        val views: Int,
        val langCode: LangCode?,
        val published: Boolean,
        val attackStat: Int?,
        val defenseStat: Int?,
        val mobilityStat: Int?,
        val utilityStat: Int?,
        val difficultiyStat: Int?
): Serializable