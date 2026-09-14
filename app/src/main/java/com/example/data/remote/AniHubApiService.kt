package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SubtitleTrackDto(
    @Json(name = "lang") val lang: String = "English",
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class StreamSourceDto(
    @Json(name = "server") val server: String,
    @Json(name = "url") val url: String,
    @Json(name = "quality") val quality: String = "1080p",
    @Json(name = "isHls") val isHls: Boolean = true
)

@JsonClass(generateAdapter = true)
data class StreamResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "server") val server: String = "Anikoto",
    @Json(name = "streamUrl") val streamUrl: String = "",
    @Json(name = "backupUrl") val backupUrl: String? = null,
    @Json(name = "quality") val quality: String = "1080p",
    @Json(name = "latencyMs") val latencyMs: Int = 38,
    @Json(name = "serverNode") val serverNode: String = "ANIKOTO-NODE-01",
    @Json(name = "subtitles") val subtitles: List<SubtitleTrackDto> = emptyList(),
    @Json(name = "audioTrack") val audioTrack: String = "Sub"
)

@JsonClass(generateAdapter = true)
data class EpisodeStreamDto(
    @Json(name = "episodeNumber") val episodeNumber: Int,
    @Json(name = "title") val title: String = "",
    @Json(name = "sources") val sources: List<StreamSourceDto> = emptyList()
)

/**
 * Retrofit service interface defining dynamic episode streaming requests
 * from Anikoto, AniDB, and auxiliary anime streaming servers.
 */
interface AniHubApiService {

    @GET("api/v1/anikoto/stream")
    suspend fun getAnikotoStream(
        @Query("animeId") animeId: Int,
        @Query("episode") episodeNumber: Int,
        @Query("quality") quality: String? = null
    ): Response<StreamResponseDto>

    @GET("api/v1/anidb/stream")
    suspend fun getAniDbStream(
        @Query("animeId") animeId: Int,
        @Query("episode") episodeNumber: Int,
        @Query("quality") quality: String? = null
    ): Response<StreamResponseDto>

    @GET("api/v1/stream/{server}")
    suspend fun getStreamByServer(
        @Path("server") server: String,
        @Query("animeId") animeId: Int,
        @Query("episode") episodeNumber: Int,
        @Query("quality") quality: String? = null
    ): Response<StreamResponseDto>

    @GET("api/v1/anime/{animeId}/episodes")
    suspend fun getAnimeEpisodes(
        @Path("animeId") animeId: Int
    ): Response<List<EpisodeStreamDto>>

    companion object {
        private const val BASE_URL = "https://api.anihub.stream/"

        fun create(): AniHubApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "AniHub/2.0 (Android; DynamicStreamEngine)")
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(AniHubApiService::class.java)
        }
    }
}
