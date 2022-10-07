package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResult(val result: ResultType, val error: String, val token: String, val username: String, val userid: String)

@JsonClass(generateAdapter = true)
data class RegisterResult(val result: ResultType, val error: String)

@JsonClass(generateAdapter = true)
data class SendmailResult(val result: ResultType, val error: String)

@JsonClass(generateAdapter = true)
data class EditBuildResult(val result: ResultType, val error: String, val buildId: Int? = null)

enum class ResultType { @Json(name="success") Success, @Json(name="error") Error}