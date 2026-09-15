package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ScraperRepository {
    private val scraperApiService = NetworkClient.getScraperApiService()
    private val TAG = "ScraperRepository"
    
    // ===================== JSOUP LOCAL SCRAPING =====================
    
    suspend fun scrapeWithJsoup(url: String, selector: String? = null): Result<Document> = 
        withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Scraping with Jsoup: $url")
            
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .followRedirects(true)
                .get()
            
            Log.d(TAG, "Successfully scraped: $url")
            Result.success(doc)
        } catch (e: Exception) {
            Log.e(TAG, "Jsoup scraping failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun extractAnimeFromHtml(
        doc: Document,
        titleSelector: String = "h1.title",
        episodeSelector: String = "div.episode"
    ): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Extracting anime data from HTML")
            
            val title = doc.selectFirst(titleSelector)?.text() ?: "Unknown"
            val episodes = doc.select(episodeSelector).map { element ->
                mapOf(
                    "number" to (element.attr("data-ep") ?: element.text()),
                    "url" to element.attr("href"),
                    "title" to element.selectFirst(".ep-title")?.text(),
                    "thumbnail" to element.selectFirst("img")?.attr("src")
                )
            }
            
            val data = mapOf(
                "title" to title,
                "episodes" to episodes,
                "totalEpisodes" to episodes.size
            )
            
            Log.d(TAG, "Extracted ${episodes.size} episodes")
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "HTML extraction failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ===================== BACKEND PUPPETEER SCRAPING =====================
    
    suspend fun scrapeWithBackend(
        url: String,
        type: String = "jsoup"
    ): Result<ScraperResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Scraping via backend ($type): $url")
            
            // Check backend health first
            val healthCheck = scraperApiService.healthCheck().execute()
            if (!healthCheck.isSuccessful) {
                Log.w(TAG, "Backend not available, falling back to local Jsoup")
                return@withContext Result.failure(Exception("Backend unavailable"))
            }
            
            val request = ScraperRequest(
                url = url,
                type = type
            )
            
            val response = when (type) {
                "puppeteer" -> scraperApiService.scrapeWithPuppeteer(request).execute()
                else -> scraperApiService.scrapeWithJsoup(request).execute()
            }
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Log.d(TAG, "Backend scraping successful")
                    Result.success(body)
                } else {
                    Log.e(TAG, "Backend error: ${body?.error}")
                    Result.failure(Exception(body?.error ?: "Unknown error"))
                }
            } else {
                Log.e(TAG, "Backend request failed: ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend scraping error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAnimeInfoFromBackend(url: String): Result<ScrapedAnimeResponse> = 
        withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching anime info from backend: $url")
            
            val response = scraperApiService.getAnimeInfo(url).execute()
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Anime info retrieved: ${body.title}")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Log.e(TAG, "Failed to get anime info: ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Anime info fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ===================== HYBRID APPROACH =====================
    
    /**
     * Smart scraping: Try local Jsoup first, fallback to backend Puppeteer
     */
    suspend fun scrapeSmart(
        url: String,
        titleSelector: String = "h1.title",
        episodeSelector: String = "div.episode"
    ): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting smart scraping for: $url")
        
        // Try local Jsoup first (faster, no server dependency)
        val localResult = scrapeWithJsoup(url)
        
        if (localResult.isSuccess) {
            // Extract data from HTML
            val extractResult = extractAnimeFromHtml(
                localResult.getOrNull()!!,
                titleSelector,
                episodeSelector
            )
            
            if (extractResult.isSuccess) {
                Log.d(TAG, "Smart scraping succeeded with Jsoup")
                return@withContext extractResult
            }
        }
        
        // Fallback to backend Puppeteer (slower but handles JS)
        Log.d(TAG, "Jsoup failed, trying backend Puppeteer")
        
        val backendResult = scrapeWithBackend(url, "puppeteer")
        
        if (backendResult.isSuccess) {
            val response = backendResult.getOrNull()!!
            if (response.data != null) {
                Log.d(TAG, "Smart scraping succeeded with Puppeteer")
                return@withContext Result.success(response.data)
            }
        }
        
        Log.e(TAG, "Smart scraping failed on both methods")
        return@withContext Result.failure(
            Exception("Both Jsoup and Puppeteer scraping failed")
        )
    }
    
    // ===================== BATCH OPERATIONS =====================
    
    suspend fun scrapeMultipleUrls(
        urls: List<String>,
        usePuppeteer: Boolean = false
    ): Result<List<ScraperResponse>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Scraping ${urls.size} URLs")
            
            val results = urls.map { url ->
                val request = ScraperRequest(
                    url = url,
                    type = if (usePuppeteer) "puppeteer" else "jsoup"
                )
                
                val response = if (usePuppeteer) {
                    scraperApiService.scrapeWithPuppeteer(request).execute()
                } else {
                    scraperApiService.scrapeWithJsoup(request).execute()
                }
                
                response.body() ?: ScraperResponse(
                    success = false,
                    error = "No response body"
                )
            }
            
            Log.d(TAG, "Batch scraping completed: ${results.size} results")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Batch scraping error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ===================== UTILITY FUNCTIONS =====================
    
    suspend fun isBackendAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = scraperApiService.healthCheck().execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
