package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

sealed class News: Serializable

@JsonClass(generateAdapter = true)
data class NormalNews(
        @Json(name="id") val newsId: Int,
        val authorId: Int?,
        val authorName: String,
        val title: String,
        val content: String,
        val image: String,
        val date: Date
) : News()

data class PatchnotesNews(val content: String) : News()

data class BuilderNews(val heroId: Int, val heroName: String) : News()

class LoadingNews: News()