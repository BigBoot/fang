package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.api.emu.EmuApi
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class EmuServiceImpl : EmuService, KoinComponent {
    private val client: EmuApi?

    init {
        if (Config.emu.url != null && Config.emu.api_key != null) {
            client = Retrofit.Builder()
                .baseUrl(Config.emu.url)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(
                    OkHttpClient().newBuilder()
                        .addInterceptor {
                            it.proceed(it.request().newBuilder().addHeader("x-api-key", Config.emu.api_key).build())
                        }.build())
                .build()
                .create(EmuApi::class.java)
        } else {
            client = null
        }
    }

    override suspend fun getQueue(): List<String> {
        return try { client?.getQueue()?.players ?: emptyList() } catch (t: Throwable) { emptyList() }
    }
}
