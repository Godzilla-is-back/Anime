package com.example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Anime
import com.example.data.model.Episode
import com.example.data.model.StreamQuality
import com.example.data.model.StreamServer
import com.example.data.remote.AnimeStreamResolver
import com.example.data.remote.ResolvedStream
import com.example.data.remote.StreamResolutionState
import com.example.data.remote.ServerCaptionProfile
import com.example.data.remote.ServerQualityProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VideoAspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    THEATER_EXPAND("Expanded"),
    RATIO_16_9("16:9")
}

data class VideoPlayerUiState(
    val anime: Anime? = null,
    val episode: Episode? = null,
    val server: StreamServer = StreamServer.KOTO,
    val quality: StreamQuality = StreamQuality.Q1080P,
    val audioTrack: String = "Sub", // "Sub" or "Dub"
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isLocked: Boolean = false,
    val isBiggerScreen: Boolean = false,
    val isFullscreen: Boolean = false,
    val aspectRatioMode: VideoAspectRatioMode = VideoAspectRatioMode.FIT,
    val subtitlesEnabled: Boolean = true,
    val selectedSubtitleLang: String = "English",
    val availableSubtitles: List<String> = listOf("Off", "English", "Spanish", "French", "German", "Japanese"),
    val availableQualities: List<ServerQualityProfile> = emptyList(),
    val availableCaptions: List<ServerCaptionProfile> = emptyList(),
    val playbackSpeed: Float = 1.0f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 1440000L,
    val resolutionState: StreamResolutionState? = null,
    val resolvedStream: ResolvedStream? = null,
    val errorMessage: String? = null,
    val connectionLogs: List<String> = emptyList()
)

/**
 * ViewModel managing dynamic streaming network retrieval, ExoPlayer state,
 * server switching (Anikoto, AniDB, etc.), and bigger screen player controls.
 */
class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var streamLoadingJob: Job? = null

    /**
     * Dynamically fetches the video source URL based on the selected anime ID and
     * episode number from the API (via Retrofit AniHubApiService & AnimeStreamResolver),
     * ensuring real-time server negotiation for the chosen server (Anikoto/AniDB/etc.).
     */
    fun play(
        anime: Anime,
        episode: Episode,
        server: StreamServer = _uiState.value.server,
        quality: StreamQuality = _uiState.value.quality,
        initialPositionMs: Long = 0L
    ) {
        streamLoadingJob?.cancel()

        _uiState.update {
            it.copy(
                anime = anime,
                episode = episode,
                server = server,
                quality = quality,
                currentPositionMs = initialPositionMs,
                isBuffering = true,
                errorMessage = null,
                resolvedStream = null,
                connectionLogs = listOf("Initializing network request to ${server.displayName} for \"${anime.title}\" (Ep ${episode.episodeNum})...")
            )
        }

        streamLoadingJob = viewModelScope.launch {
            AnimeStreamResolver.requestServerStream(
                anime = anime,
                episodeNum = episode.episodeNum,
                server = server,
                quality = quality,
                audioTrack = _uiState.value.audioTrack
            ).collect { state ->
                when (state) {
                    is StreamResolutionState.Connecting -> {
                        _uiState.update { current ->
                            current.copy(
                                resolutionState = state,
                                isBuffering = true,
                                connectionLogs = (current.connectionLogs + state.message).takeLast(6)
                            )
                        }
                    }
                    is StreamResolutionState.QueryingServer -> {
                        _uiState.update { current ->
                            current.copy(
                                resolutionState = state,
                                isBuffering = true,
                                connectionLogs = (current.connectionLogs + state.message).takeLast(6)
                            )
                        }
                    }
                    is StreamResolutionState.NegotiatingProfiles -> {
                        _uiState.update { current ->
                            current.copy(
                                resolutionState = state,
                                isBuffering = true,
                                connectionLogs = (current.connectionLogs + state.message).takeLast(6)
                            )
                        }
                    }
                    is StreamResolutionState.HandshakeVerified -> {
                        _uiState.update { current ->
                            current.copy(
                                resolutionState = state,
                                connectionLogs = (current.connectionLogs + state.message).takeLast(6)
                            )
                        }
                    }
                    is StreamResolutionState.Success -> {
                        _uiState.update { current ->
                            current.copy(
                                resolutionState = state,
                                resolvedStream = state.stream,
                                availableQualities = state.stream.availableQualities,
                                availableCaptions = state.stream.availableCaptions,
                                isBuffering = false,
                                errorMessage = null,
                                connectionLogs = (current.connectionLogs + "Connected to ${state.stream.serverNode} (${state.stream.latencyMs}ms)").takeLast(6)
                            )
                        }
                    }
                    is StreamResolutionState.Error -> {
                        _uiState.update { current ->
                            current.copy(
                                resolutionState = state,
                                isBuffering = false,
                                errorMessage = state.message,
                                connectionLogs = (current.connectionLogs + "ERROR: ${state.message}").takeLast(6)
                            )
                        }
                    }
                }
            }
        }
    }

    fun switchServer(newServer: StreamServer) {
        val state = _uiState.value
        val anime = state.anime ?: return
        val episode = state.episode ?: return
        play(
            anime = anime,
            episode = episode,
            server = newServer,
            quality = state.quality,
            initialPositionMs = state.currentPositionMs
        )
    }

    fun switchQuality(newQuality: StreamQuality) {
        _uiState.update { it.copy(quality = newQuality) }
        val state = _uiState.value
        val anime = state.anime ?: return
        val episode = state.episode ?: return
        play(
            anime = anime,
            episode = episode,
            server = state.server,
            quality = newQuality,
            initialPositionMs = state.currentPositionMs
        )
    }

    fun switchAudioTrack(track: String) {
        _uiState.update { it.copy(audioTrack = track) }
    }

    fun togglePlayPause(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun updateBuffering(buffering: Boolean) {
        _uiState.update { it.copy(isBuffering = buffering) }
    }

    fun updateProgress(currentMs: Long, totalMs: Long) {
        _uiState.update {
            it.copy(
                currentPositionMs = currentMs,
                durationMs = if (totalMs > 0) totalMs else it.durationMs
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleSubtitles() {
        _uiState.update { it.copy(subtitlesEnabled = !it.subtitlesEnabled) }
    }

    fun selectSubtitleLang(lang: String) {
        if (lang.equals("Off", ignoreCase = true)) {
            _uiState.update { it.copy(subtitlesEnabled = false, selectedSubtitleLang = "Off") }
        } else {
            _uiState.update { it.copy(subtitlesEnabled = true, selectedSubtitleLang = lang) }
        }
    }

    fun selectCaptionProfile(profile: ServerCaptionProfile) {
        if (profile.code.equals("off", ignoreCase = true)) {
            _uiState.update { it.copy(subtitlesEnabled = false, selectedSubtitleLang = "Off") }
        } else {
            _uiState.update {
                it.copy(
                    subtitlesEnabled = true,
                    selectedSubtitleLang = profile.label,
                    resolvedStream = it.resolvedStream?.copy(
                        activeCaption = profile,
                        subtitleUrl = profile.subtitleUrl
                    )
                )
            }
        }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun toggleBiggerScreen() {
        _uiState.update { it.copy(isBiggerScreen = !it.isBiggerScreen) }
    }

    fun cycleAspectRatio() {
        val modes = VideoAspectRatioMode.values()
        val next = modes[(_uiState.value.aspectRatioMode.ordinal + 1) % modes.size]
        _uiState.update { it.copy(aspectRatioMode = next) }
    }

    fun retry() {
        val state = _uiState.value
        val anime = state.anime ?: return
        val episode = state.episode ?: return
        play(
            anime = anime,
            episode = episode,
            server = state.server,
            quality = state.quality,
            initialPositionMs = state.currentPositionMs
        )
    }
}
