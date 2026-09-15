package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

// Backend Scraper API responses
@JsonClass(generateAdapter = true)
data class ScrapedAnimeResponse(
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "episodes")
    val episodes: List<ScrapedEpisode>? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "coverImage")
    val coverImage: String? = null,
    @Json(name = "rating")
    val rating: Float? = null,
    @Json(name = "genres")
    val genres: List<String>? = null,
    @Json(name = "status")
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class ScrapedEpisode(
    @Json(name = "number")
    val number: Int? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "thumbnail")
    val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class ScraperRequest(
    @Json(name = "url")
    val url: String,
    @Json(name = "selector")
    val selector: String? = null,
    @Json(name = "type")
    val type: String = "jsoup" // jsoup or puppeteer
)

@JsonClass(generateAdapter = true)
data class ScraperResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "data")
    val data: Map<String, Any>? = null,
    @Json(name = "error")
    val error: String? = null
)

/**
 * Scraper Backend API Service - For complex sites
 * Base URL: Your own backend server (Node.js with puppeteer)
 */
interface ScraperApiService {
    
    @POST("scrape/jsoup")
    fun scrapeWithJsoup(
        @Body request: ScraperRequest
    ): Call<ScraperResponse>
    
    @POST("scrape/puppeteer")
    fun scrapeWithPuppeteer(
        @Body request: ScraperRequest
    ): Call<ScraperResponse>
    
    @GET("scrape/anime/info")
    fun getAnimeInfo(
        @Query("url") url: String
    ): Call<ScrapedAnimeResponse>
    
    @GET("health")
    fun healthCheck(): Call<Map<String, String>>
}
