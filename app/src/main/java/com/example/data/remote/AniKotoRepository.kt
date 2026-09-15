package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AniKotoRepository {
    private val apiService = NetworkClient.getAniKotoApiService()
    private val TAG = "AniKotoRepository"
    
    suspend fun searchAnime(query: String): Result<List<AniKotoAnime>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Searching for: $query")
            val response = apiService.searchAnime(query).execute()
            
            if (response.isSuccessful) {
                val animes = response.body()?.results ?: emptyList()
                Log.d(TAG, "Found ${animes.size} results")
                Result.success(animes)
            } else {
                Log.e(TAG, "Search failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Search failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getEpisodes(animeId: String): Result<List<AniKotoEpisode>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching episodes for anime: $animeId")
            val response = apiService.getEpisodes(animeId).execute()
            
            if (response.isSuccessful) {
                val episodes = response.body() ?: emptyList()
                Log.d(TAG, "Found ${episodes.size} episodes")
                Result.success(episodes)
            } else {
                Log.e(TAG, "Episodes fetch failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Episodes fetch failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Episodes fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getSubbedStream(
        animeId: String,
        episodeNumber: Int
    ): Result<AniKotoStreamResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching subbed stream for anime: $animeId, episode: $episodeNumber")
            val response = apiService.getSubbedStream(animeId, episodeNumber).execute()
            
            if (response.isSuccessful) {
                val stream = response.body()
                if (stream != null) {
                    Log.d(TAG, "Subbed stream found with ${stream.sources?.size ?: 0} sources")
                    Result.success(stream)
                } else {
                    Result.failure(Exception("Stream response is empty"))
                }
            } else {
                Log.e(TAG, "Stream fetch failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Stream fetch failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getDubbedStream(
        animeId: String,
        episodeNumber: Int
    ): Result<AniKotoStreamResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching dubbed stream for anime: $animeId, episode: $episodeNumber")
            val response = apiService.getDubbedStream(animeId, episodeNumber).execute()
            
            if (response.isSuccessful) {
                val stream = response.body()
                if (stream != null) {
                    Log.d(TAG, "Dubbed stream found with ${stream.sources?.size ?: 0} sources")
                    Result.success(stream)
                } else {
                    Result.failure(Exception("Stream response is empty"))
                }
            } else {
                Log.e(TAG, "Stream fetch failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Stream fetch failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAnimeInfo(slug: String): Result<AniKotoAnimeInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching anime info for: $slug")
            val response = apiService.getAnimeInfo(slug).execute()
            
            if (response.isSuccessful) {
                val info = response.body()
                if (info != null) {
                    Log.d(TAG, "Anime info found: ${info.title}")
                    Result.success(info)
                } else {
                    Result.failure(Exception("Anime info response is empty"))
                }
            } else {
                Log.e(TAG, "Anime info fetch failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Anime info fetch failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Anime info fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getSchedule(date: String): Result<List<AniKotoAnime>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching schedule for date: $date")
            val response = apiService.getSchedule(date).execute()
            
            if (response.isSuccessful) {
                val schedule = response.body() ?: emptyList()
                Log.d(TAG, "Found ${schedule.size} animes on schedule")
                Result.success(schedule)
            } else {
                Log.e(TAG, "Schedule fetch failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Schedule fetch failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Schedule fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
