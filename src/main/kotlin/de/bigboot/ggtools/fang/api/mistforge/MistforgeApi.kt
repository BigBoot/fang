/*
 * Copyright (C) Marco Kirchner - All Rights Reserved
 * Unauthorized copying of this file, via any medium is prohibited
 * Proprietary and confidential
 * Written by Marco Kirchner <marco@kirchner.pw>, 2017
 */

package de.bigboot.ggtools.fang.api.mistforge

import com.squareup.moshi.Moshi
import de.bigboot.ggtools.fang.api.mistforge.model.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*


interface MistforgeApi {
    @FormUrlEncoded
    @POST("/api/build/get")
    suspend fun getGuide(@Field("buildId") buildId: Int): Guide

    @FormUrlEncoded
    @POST("/api/build/search")
    fun searchGuides(
            @Field(value="heroId") heroId: Int,
            @Field(value="roles") roles: String? = null ,
            @Field(value="limit") limit: Int = 10,
            @Field(value="page") page: Int = 1,
            @Field(value="userId") userId: Int? = null,
            @Field(value="sortBy") sortBy: String = "votes"
    ): List<Guide>

    @GET("/api/build/text")
    suspend fun getHeroes(): Heroes

    @GET("/api/data/roles")
    suspend fun getRoles(): Roles

    @GET("/api/data/creatures")
    suspend fun getCreatures(): Creatures

    @FormUrlEncoded
    @POST("/api/news")
    suspend fun getNews(@Field(value="limit") limit: Int = 10, @Field(value="page") page: Int = 1): List<NormalNews>

    @FormUrlEncoded
    @POST("/api/login")
    suspend fun login(@Field(value="username") username: String, @Field(value="password") password: String): LoginResult

    @FormUrlEncoded
    @POST("/api/registration")
    suspend fun register(@Field(value="username") username: String, @Field(value="email") email: String, @Field(value="password") password: String, @Field(value="googlecaptcha") captchaToken: String): RegisterResult

    @FormUrlEncoded
    @POST("/api/sendmail")
    suspend fun sendmail(@Field(value="username") username: String, @Field(value="email") email: String, @Field(value="language") language: String = "en"): SendmailResult

    @FormUrlEncoded
    @POST("/api/build/create_build")
    suspend fun editbuild(
            @Field(value="buildid") buildid: Int?, // (optional if you give it it updates the build if not it creates a new one)
            @Field(value="userid") userid: String, // (needed)
            @Field(value="heroid") heroid: Int, // (needed default 0 would be aisling)
            @Field(value="skills") skills: String, // (not needed but you should give them at least for published builds)
            @Field(value="talent") talent: Int, // (not needed but you should give them at least for published builds)
            @Field(value="title") title: String, // (not needed but you should give them at least for published builds)
            @Field(value="role") role: String?, // (not needed default is "Tank" other roles are "Support","Assassin","Range DPS" , "Initiator" , "Melee DPS" and "Bruiser")
            @Field(value="language") language: String = "en", // (not needed default is "en")
            @Field(value="publish") publish: Int, // (not needed default is 0 for not published 1 is published)
            @Field(value="attack") attack: Int, // (not needed but default is 0 you simply can give the attack of the hero)
            @Field(value="defense") defense: Int, // (not needed but default is 0 you simply can give the defense of the hero)
            @Field(value="mobility") mobility: Int, // (not needed but default is 0 you simply can give the mobility of the hero)
            @Field(value="utility") utility: Int, // (not needed but default is 0 you simply can give the utility  of the hero)
            @Field(value="difficulty") difficulty: Int, // (not needed but default is 0 you simply can give the difficulty of the hero)
            @Field(value="token") token: String, // (needed to tell if the user is who he sais he is)
            @Field(value="guidetext") guidetext: String? = null, // (not needed)
            @Field(value="description") description: String? = null, // (not needed)
            @Field(value="creatures") creatures: String? = null // (not needed)
    ): EditBuildResult

    @GET("/img/hero_builder/{hero}/{image}")
    suspend fun getHeroImage(@Path("hero") heroId: Int, @Path("image") image: String): ResponseBody


    companion object {
        private val apikey = "Y5wdQuVp28QNSapTqAumf7Xg3XGE6tms"

        val okHttp = OkHttpClient.Builder()
                .addInterceptor(ApiKeyInterceptor(apikey))
                .build()

        val moshi = Moshi.Builder()
                .add(Date::class.java, DateAdapter("yyyy-MM-dd hh:mm:ss"))
                .add(Roles::class.java, RolesAdapter())
                .add(CreaturesAdapter.Factory)
                .build()

        val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .client(okHttp)
                .baseUrl("https://mistforge.net")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        fun create() = retrofit.create<MistforgeApi>()

        inline fun <reified T> toJson(obj: T): String = moshi.adapter(T::class.java).toJson(obj)
        inline fun <reified T> fromJson(json: String): T = moshi.adapter(T::class.java).fromJson(json)!!
    }
}

class ApiKeyInterceptor(val apiKey: String): Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()

        val url = request.url().newBuilder()
                .addQueryParameter("apikey", apiKey)
                .build()

        val requestBuilder = chain.request().newBuilder().url(url)

        return chain.proceed(requestBuilder.build())
    }
}