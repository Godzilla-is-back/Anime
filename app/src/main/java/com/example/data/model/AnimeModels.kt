package com.example.data.model

data class Anime(
    val id: Int,
    val title: String,
    val romajiTitle: String = "",
    val coverUrl: String,
    val bannerUrl: String = "",
    val bannerDrawableRes: Int? = null,
    val description: String,
    val score: Int, // e.g. 88 (88%)
    val genres: List<String>,
    val episodesCount: Int,
    val seasonYear: Int = 2026,
    val format: String = "TV Series",
    val status: String = "Airing",
    val studio: String = "MAPPA",
    val rating: String = "PG-13"
)

data class Episode(
    val id: String,
    val animeId: Int,
    val episodeNum: Int,
    val title: String,
    val durationMinutes: Int = 24,
    val thumbnail: String,
    val description: String = "",
    val videoUrl: String,
    val introStartSec: Int = 90,
    val introEndSec: Int = 175
)

enum class StreamServer(
    val serverId: String,
    val displayName: String,
    val shortName: String,
    val tag: String,
    val pingMs: Int,
    val isDefault: Boolean = false
) {
    KOTO("koto", "Koto", "Koto", "Anikoto FastCDN", 24, true),
    NEKO("neko", "Neko", "Neko", "Anikoto 1080p", 31),
    GG("gg", "GG", "GG", "Anikoto Mirror", 45)
}

enum class StreamQuality(val label: String, val resolution: String, val bitrate: String) {
    Q1080P("1080p", "1920x1080", "5.4 Mbps"),
    Q720P("720p", "1280x720", "2.8 Mbps"),
    Q480P("480p", "854x480", "1.2 Mbps"),
    Q360P("360p", "640x360", "750 Kbps"),
    AUTO("Auto", "Adaptive", "Dynamic")
}

data class UserProfile(
    val email: String,
    val displayName: String,
    val photoUrl: String = "",
    val isGoogleLoggedIn: Boolean = false,
    val syncStatus: String = "Local Only",
    val memberSince: String = "2026"
)

enum class AppThemeMode(val id: String, val displayName: String, val subtitle: String) {
    SAKURA_OBSIDIAN("sakura_obsidian", "Sakura & Midnight Obsidian", "Soft Pink + Deep Celestial Violet"),
    CYBER_ROSE("cyber_rose", "Cyber Rose & Neon Cyan", "Vibrant Neon Pink + Electric Cyan"),
    PEACH_SUNSET("peach_sunset", "Peach Sunset & Amethyst", "Warm Pastel Pink + Royal Amethyst"),
    COTTON_CANDY("cotton_candy", "Cotton Candy Cloud", "Blush Pink + Pastel Sky Blue")
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SAKURA_OBSIDIAN,
    val defaultServer: StreamServer = StreamServer.KOTO,
    val defaultQuality: StreamQuality = StreamQuality.Q1080P,
    val autoSkipIntro: Boolean = true,
    val autoPlayNext: Boolean = true,
    val audioLanguage: String = "Japanese (Original)",
    val smoothMotion: Boolean = true,
    val compactCards: Boolean = false,
    val offlineModeActive: Boolean = false
)
