package com.example.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val ANIKOTO_API_BASE_URL = "https://anikototvapi.vercel.app/"
    private const val SCRAPER_API_BASE_URL = "http://localhost:3000/" // Change to your backend URL
    private const val TAG = "NetworkClient"
    
    private var aniKotoRetrofit: Retrofit? = null
    private var aniKotoApiService: AniKotoApiService? = null
    
    private var scraperRetrofit: Retrofit? = null
    private var scraperApiService: ScraperApiService? = null
    
    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor())
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url
                
                val newUrl = originalUrl.newBuilder()
                    .build()
                
                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .build()
                
                chain.proceed(newRequest)
            }
            .retryOnConnectionFailure(true)
            .build()
    }
    
    private fun getMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }
    
    // ===================== ANIKOTO API =====================
    
    fun getAniKotoApiService(): AniKotoApiService {
        if (aniKotoApiService == null) {
            if (aniKotoRetrofit == null) {
                aniKotoRetrofit = Retrofit.Builder()
                    .baseUrl(ANIKOTO_API_BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
                    .build()
            }
            aniKotoApiService = aniKotoRetrofit!!.create(AniKotoApiService::class.java)
        }
        return aniKotoApiService!!
    }
    
    fun resetAniKotoService() {
        aniKotoApiService = null
        aniKotoRetrofit = null
    }
    
    // ===================== SCRAPER BACKEND API =====================
    
    /**
     * Get Scraper API Service for backend scraping
     * Make sure your backend is running at SCRAPER_API_BASE_URL
     */
    fun getScraperApiService(): ScraperApiService {
        if (scraperApiService == null) {
            if (scraperRetrofit == null) {
                scraperRetrofit = Retrofit.Builder()
                    .baseUrl(SCRAPER_API_BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
                    .build()
            }
            scraperApiService = scraperRetrofit!!.create(ScraperApiService::class.java)
        }
        return scraperApiService!!
    }
    
    fun resetScraperService() {
        scraperApiService = null
        scraperRetrofit = null
    }
    
    /**
     * Change backend URL at runtime
     */
    fun setScraperBaseUrl(baseUrl: String) {
        Log.d(TAG, "Changing scraper base URL to: $baseUrl")
        scraperApiService = null
        scraperRetrofit = null
        
        scraperRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .build()
        
        scraperApiService = scraperRetrofit!!.create(ScraperApiService::class.java)
    }
}

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        Log.d("OkHttp", "Sending request to: ${request.url}")
        Log.d("OkHttp", "Headers: ${request.headers}")
        
        return try {
            val response = chain.proceed(request)
            val endTime = System.currentTimeMillis()
            
            Log.d("OkHttp", "Response received in ${endTime - startTime}ms")
            Log.d("OkHttp", "Status Code: ${response.code}")
            
            response
        } catch (e: Exception) {
            Log.e("OkHttp", "Network error: ${e.message}", e)
            throw e
        }
    }
}
