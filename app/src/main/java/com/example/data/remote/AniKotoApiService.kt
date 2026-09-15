package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Data classes for AniKoto API responses
@JsonClass(generateAdapter = true)
data class AniKotoSearchResponse(
    @Json(name = "results")
    val results: List<AniKotoAnime>? = null
)

@JsonClass(generateAdapter = true)
data class AniKotoAnime(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "anilistId")
    val anilistId: Int? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "image")
    val image: String? = null,
    @Json(name = "cover")
    val cover: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "rating")
    val rating: Float? = null,
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "releaseDate")
    val releaseDate: Int? = null,
    @Json(name = "status")
    val status: String? = null,
    @Json(name = "genres")
    val genres: List<String>? = null,
    @Json(name = "totalEpisodes")
    val totalEpisodes: Int? = null
)

@JsonClass(generateAdapter = true)
data class AniKotoEpisode(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "number")
    val number: Int? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "image")
    val image: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "airDate")
    val airDate: String? = null
)

@JsonClass(generateAdapter = true)
data class AniKotoStreamingSource(
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "quality")
    val quality: String? = null,
    @Json(name = "headers")
    val headers: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class AniKotoStreamResponse(
    @Json(name = "sources")
    val sources: List<AniKotoStreamingSource>? = null,
    @Json(name = "subtitles")
    val subtitles: List<SubtitleSource>? = null
)

@JsonClass(generateAdapter = true)
data class SubtitleSource(
    @Json(name = "lang")
    val lang: String? = null,
    @Json(name = "url")
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class AniKotoAnimeInfo(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "anilistId")
    val anilistId: Int? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "image")
    val image: String? = null,
    @Json(name = "cover")
    val cover: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "rating")
    val rating: Float? = null,
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "releaseDate")
    val releaseDate: Int? = null,
    @Json(name = "status")
    val status: String? = null,
    @Json(name = "genres")
    val genres: List<String>? = null,
    @Json(name = "totalEpisodes")
    val totalEpisodes: Int? = null,
    @Json(name = "episodes")
    val episodes: List<AniKotoEpisode>? = null
)

/**
 * AniKoto API Service - Real-time anime streaming data from anikototv.to
 * Base URL: https://anikototvapi.vercel.app
 */
interface AniKotoApiService {
    
    @GET("search")
    fun searchAnime(
        @Query("q") query: String
    ): Call<AniKotoSearchResponse>
    
    @GET("anime/{id}/episodes")
    fun getEpisodes(
        @Path("id") animeId: String
    ): Call<List<AniKotoEpisode>>
    
    @GET("anime/{id}/{episode}/sub")
    fun getSubbedStream(
        @Path("id") animeId: String,
        @Path("episode") episodeNumber: Int
    ): Call<AniKotoStreamResponse>
    
    @GET("anime/{id}/{episode}/dub")
    fun getDubbedStream(
        @Path("id") animeId: String,
        @Path("episode") episodeNumber: Int
    ): Call<AniKotoStreamResponse>
    
    @GET("info")
    fun getAnimeInfo(
        @Query("name") slug: String
    ): Call<AniKotoAnimeInfo>
    
    @GET("schedule")
    fun getSchedule(
        @Query("time") date: String
    ): Call<List<AniKotoAnime>>
}
