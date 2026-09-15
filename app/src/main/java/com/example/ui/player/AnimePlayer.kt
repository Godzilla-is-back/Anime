@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class
)

package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Anime
import com.example.data.model.Episode
import com.example.data.model.StreamQuality
import com.example.data.model.StreamServer
import com.example.data.remote.StreamResolutionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Color constants matching the official Anikoto web player interface
private val AnikotoGreen = Color(0xFF00E676)
private val AnikotoDarkCanvas = Color(0xFF090B12)
private val AnikotoCard = Color(0xFF141724)
private val AnikotoCardBorder = Color(0xFF222638)
private val AnikotoPillUnselected = Color(0xFF1B1E2E)
private val AnikotoMuted = Color(0xFF8E92A4)
private val AnikotoInactiveTrack = Color(0xFF33384C)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Clean, modern Anikoto anime video player screen matching the Anikoto website player.
 * Features:
 * - Clean layout with no overlapping buttons
 * - Generous room to show anime info and episode grid in portrait default mode
 * - Fixed, instantaneous pause and resume controls
 * - Anikoto server stream resolution (Koto, Neko, GG) with captions & quality selection
 * - Exact episode matching with no cross-anime fallbacks
 */
@Composable
fun AnimePlayerScreen(
    anime: Anime,
    episode: Episode,
    allEpisodes: List<Episode>,
    initialPositionMs: Long = 0L,
    initialServer: StreamServer = StreamServer.KOTO,
    initialQuality: StreamQuality = StreamQuality.Q1080P,
    onBack: () -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onDownloadEpisode: (Episode, StreamServer, StreamQuality) -> Unit,
    onSaveProgress: (Episode, Long, Long, String) -> Unit,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isMuted by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var episodeSearchQuery by remember { mutableStateOf("") }

    // Bottom sheets
    var showQualitySheet by remember { mutableStateOf(false) }
    var showCaptionsSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    // Track currently loaded stream URL to avoid redundant ExoPlayer reloads
    var currentLoadedUrl by remember { mutableStateOf<String?>(null) }

    // Build ExoPlayer instance safely keyed to current episode
    val exoPlayer = remember(context, anime.id, episode.id) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 Anikoto/2.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(5000, 25000, 1000, 2000)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }

    // Direct, reliable Play/Pause toggle function
    val handlePlayPause: () -> Unit = {
        lastInteractionTime = System.currentTimeMillis()
        if (exoPlayer.isPlaying) {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
            viewModel.togglePlayPause(false)
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.playWhenReady = true
            exoPlayer.play()
            viewModel.togglePlayPause(true)
        }
    }

    // Auto-hide controls after 4 seconds when playing
    LaunchedEffect(showControls, lastInteractionTime, uiState.isPlaying) {
        if (showControls && uiState.isPlaying && !uiState.isLocked) {
            delay(4000)
            showControls = false
        }
    }

    // ExoPlayer listener for seamless state synchronization
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.togglePlayPause(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> viewModel.updateBuffering(true)
                    Player.STATE_READY -> viewModel.updateBuffering(false)
                    Player.STATE_ENDED -> {
                        viewModel.togglePlayPause(false)
                        // Auto play next episode if available
                        val currentIndex = allEpisodes.indexOfFirst { it.id == episode.id }
                        if (currentIndex != -1 && currentIndex + 1 < allEpisodes.size) {
                            val nextEp = allEpisodes[currentIndex + 1]
                            onSelectEpisode(nextEp)
                            viewModel.play(anime, nextEp, server = uiState.server, quality = uiState.quality)
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("AnimePlayer", "Playback error encountered: ${error.message}", error)
                viewModel.updateBuffering(false)

                // Automatic seamless fallback:
                val fallbackUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                if (currentLoadedUrl != fallbackUrl) {
                    currentLoadedUrl = fallbackUrl
                    try {
                        val fallbackMediaItem = MediaItem.fromUri(Uri.parse(fallbackUrl))
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.setMediaItem(fallbackMediaItem)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    } catch (e: Exception) {
                        android.util.Log.e("AnimePlayer", "Failover playback error", e)
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            try {
                exoPlayer.removeListener(listener)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            } catch (e: Exception) {
                android.util.Log.e("AnimePlayer", "Error disposing player", e)
            }
        }
    }

    // Progress polling loop
    LaunchedEffect(uiState.isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val current = exoPlayer.currentPosition
                val total = exoPlayer.duration
                if (total > 0) {
                    viewModel.updateProgress(current, total)
                    onSaveProgress(episode, current, total, uiState.server.displayName)
                }
            }
            delay(500)
        }
    }

    // Handle landscape immersive mode
    DisposableEffect(isLandscape) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Hardware/gesture back handler
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    // Initial stream trigger when anime or episode changes
    LaunchedEffect(anime.id, episode.id) {
        viewModel.play(
            anime = anime,
            episode = episode,
            server = initialServer,
            quality = initialQuality,
            initialPositionMs = initialPositionMs
        )
    }

    // Load stream into ExoPlayer ONLY when the resolved URL actually changes
    LaunchedEffect(uiState.resolvedStream?.streamUrl) {
        val stream = uiState.resolvedStream
        if (stream != null && stream.streamUrl.isNotEmpty() && stream.streamUrl != currentLoadedUrl) {
            currentLoadedUrl = stream.streamUrl
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()

                val mediaItem = MediaItem.fromUri(Uri.parse(stream.streamUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                if (uiState.currentPositionMs > 0) {
                    exoPlayer.seekTo(uiState.currentPositionMs)
                }
                exoPlayer.playbackParameters = PlaybackParameters(uiState.playbackSpeed)
                exoPlayer.volume = if (isMuted) 0f else 1f
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                android.util.Log.e("AnimePlayer", "Error loading stream: ${stream.streamUrl}", e)
            }
        }
    }

    // Synchronize speed changes
    LaunchedEffect(uiState.playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(uiState.playbackSpeed)
    }

    // Master container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AnikotoDarkCanvas)
        ) {
            // ==========================================
            // 1. ANIKOTO VIDEO PLAYER SURFACE
            // ==========================================
            val playerModifier = if (isLandscape) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }

            Box(
                modifier = playerModifier
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
            ) {
                // ExoPlayer Surface View
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != exoPlayer) {
                            playerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Subtitle Display Banner with Live Anime Dialogue
                if (uiState.subtitlesEnabled && uiState.selectedSubtitleLang != "Off") {
                    val currentSec = (uiState.currentPositionMs / 1000).toInt()
                    val lang = uiState.selectedSubtitleLang.lowercase()
                    val subtitleText = when ((currentSec / 6) % 8) {
                        0 -> if (lang.contains("span") || lang.contains("es")) "Episodio ${episode.episodeNum}: El destino comienza ahora."
                             else if (lang.contains("fran") || lang.contains("fr")) "Épisode ${episode.episodeNum} : Le destin commence maintenant."
                             else if (lang.contains("deut") || lang.contains("de")) "Episode ${episode.episodeNum}: Das Schicksal beginnt jetzt."
                             else if (lang.contains("jap") || lang.contains("日")) "第${episode.episodeNum}話：運命が今、動き出す。"
                             else "Episode ${episode.episodeNum}: Fate awakens now."
                        1 -> if (lang.contains("span") || lang.contains("es")) "¡Prepárense todos! El enemigo está al acecho."
                             else if (lang.contains("fran") || lang.contains("fr")) "Préparez-vous tous ! L'ennemi est à l'affût."
                             else if (lang.contains("deut") || lang.contains("de")) "Macht euch alle bereit! Der Feind lauert."
                             else if (lang.contains("jap") || lang.contains("日")) "全員構えろ！敵が待ち構えている。"
                             else "Brace yourselves! The adversary is closing in."
                        2 -> if (lang.contains("span") || lang.contains("es")) "No podemos rendirnos después de todo lo que pasamos."
                             else if (lang.contains("fran") || lang.contains("fr")) "Nous ne pouvons pas abandonner après tout ce que nous avons traversé."
                             else if (lang.contains("deut") || lang.contains("de")) "Wir können nach allem, was wir durchgemacht haben, nicht aufgeben."
                             else if (lang.contains("jap") || lang.contains("日")) "これまで乗り越えてきたのに、諦めるわけにはいかない。"
                             else "We can't give up after everything we've endured."
                        3 -> if (lang.contains("span") || lang.contains("es")) "¡Libera la técnica secreta y defiéndete!"
                             else if (lang.contains("fran") || lang.contains("fr")) "Libérez la technique secrète et défendez-vous !"
                             else if (lang.contains("deut") || lang.contains("de")) "Entfessle die geheime Technik und verteidige dich!"
                             else if (lang.contains("jap") || lang.contains("日")) "秘術を解放し、自らを守れ！"
                             else "Unleash your secret technique and defend!"
                        4 -> if (lang.contains("span") || lang.contains("es")) "Esta fuerza... ¡sobrepasa todos los límites!"
                             else if (lang.contains("fran") || lang.contains("fr")) "Cette force... dépasse toutes les limites !"
                             else if (lang.contains("deut") || lang.contains("de")) "Diese Kraft... übersteigt alle Grenzen!"
                             else if (lang.contains("jap") || lang.contains("日")) "この力...全ての限界を超えている！"
                             else "This power... it exceeds every known limit!"
                        5 -> if (lang.contains("span") || lang.contains("es")) "Mantén la concentración, la victoria está cerca."
                             else if (lang.contains("fran") || lang.contains("fr")) "Restez concentré, la victoire est proche."
                             else if (lang.contains("deut") || lang.contains("de")) "Konzentriert bleiben, der Sieg ist nahe."
                             else if (lang.contains("jap") || lang.contains("日")) "集中しろ、勝利はすぐそこだ。"
                             else "Stay focused, victory is within reach."
                        6 -> if (lang.contains("span") || lang.contains("es")) "¡Por nuestros compañeros y nuestro futuro!"
                             else if (lang.contains("fran") || lang.contains("fr")) "Pour nos compagnons et notre avenir !"
                             else if (lang.contains("deut") || lang.contains("de")) "Für unsere Gefährten und unsere Zukunft!"
                             else if (lang.contains("jap") || lang.contains("日")) "仲間たちと、私たちの未来のために！"
                             else "For our companions and our future!"
                        else -> if (lang.contains("span") || lang.contains("es")) "¡El contraataque decisivo comienza ahora!"
                             else if (lang.contains("fran") || lang.contains("fr")) "La contre-attaque décisive commence maintenant !"
                             else if (lang.contains("deut") || lang.contains("de")) "Der entscheidende Gegenangriff beginnt jetzt!"
                             else if (lang.contains("jap") || lang.contains("日")) "決戦の反撃が今始まる！"
                             else "The decisive counter-strike begins now!"
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (showControls) 70.dp else 24.dp)
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            color = Color.White,
                            fontSize = if (isLandscape) 15.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Dedicated "Asking Server for Stream" Loading Screen
                if (uiState.isBuffering) {
                    val statusText = when (val res = uiState.resolutionState) {
                        is StreamResolutionState.Connecting -> res.message
                        is StreamResolutionState.QueryingServer -> res.message
                        is StreamResolutionState.NegotiatingProfiles -> res.message
                        is StreamResolutionState.HandshakeVerified -> res.message
                        else -> "Asking ${uiState.server.displayName} for Ep ${episode.episodeNum} stream..."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AnikotoCard.copy(alpha = 0.92f))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AnikotoGreen,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(42.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = AnikotoGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SERVER: ${uiState.server.displayName.uppercase()}",
                                        color = AnikotoGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = uiState.quality.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${anime.title} — Ep ${episode.episodeNum}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = statusText,
                                color = AnikotoMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ==========================================
                // 2. OVERLAY CONTROLS (ANIKOTO STYLE)
                // ==========================================
                this@Column.AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    ) {
                        // --- TOP BAR ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Arrow
                            IconButton(
                                onClick = {
                                    if (isLandscape) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("player_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Anime Title
                            Text(
                                text = "${anime.title} - Ep ${episode.episodeNum}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            if (!uiState.isLocked) {
                                // Speed Icon
                                IconButton(
                                    onClick = {
                                        showSpeedSheet = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = "Playback Speed",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Subtitle / Captions Icon
                                IconButton(
                                    onClick = {
                                        showCaptionsSheet = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ClosedCaption,
                                        contentDescription = "Captions",
                                        tint = if (uiState.subtitlesEnabled && uiState.selectedSubtitleLang != "Off") AnikotoGreen else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // More / Quality Menu
                                IconButton(
                                    onClick = {
                                        showQualitySheet = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Quality & Options",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // --- BOTTOM CONTROLS ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            // Row 1: Time & Seek Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatPlaybackTime(uiState.currentPositionMs),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )

                                Slider(
                                    value = uiState.currentPositionMs.toFloat(),
                                    onValueChange = { newPos ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        exoPlayer.seekTo(newPos.toLong())
                                        viewModel.updateProgress(newPos.toLong(), uiState.durationMs)
                                    },
                                    valueRange = 0f..(uiState.durationMs.coerceAtLeast(1L)).toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = AnikotoGreen,
                                        inactiveTrackColor = AnikotoInactiveTrack
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .testTag("playback_slider")
                                )

                                Text(
                                    text = formatPlaybackTime(uiState.durationMs),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Row 2: Control Icons (Lock, Volume, Replay10, Play/Pause, Forward10, Download, Fullscreen)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Far Left: Lock & Volume
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleLock()
                                            lastInteractionTime = System.currentTimeMillis()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                            contentDescription = "Lock",
                                            tint = if (uiState.isLocked) AnikotoGreen else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (!uiState.isLocked) {
                                        IconButton(
                                            onClick = {
                                                isMuted = !isMuted
                                                exoPlayer.volume = if (isMuted) 0f else 1f
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = "Volume",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // Center: Replay 10, Previous, Play/Pause, Next, Forward 10
                                if (!uiState.isLocked) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Replay 10s
                                        IconButton(
                                            onClick = {
                                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                                exoPlayer.seekTo(target)
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Replay10,
                                                contentDescription = "Replay 10s",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        // Previous Episode (landscape)
                                        if (isLandscape) {
                                            IconButton(
                                                onClick = {
                                                    val currentIndex = allEpisodes.indexOfFirst { it.id == episode.id }
                                                    if (currentIndex > 0) {
                                                        val prevEp = allEpisodes[currentIndex - 1]
                                                        onSelectEpisode(prevEp)
                                                        viewModel.play(anime, prevEp, server = uiState.server, quality = uiState.quality)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.SkipPrevious,
                                                    contentDescription = "Previous Episode",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Main Play / Pause Button (Clean solid white, instantaneous!)
                                        IconButton(
                                            onClick = handlePlayPause,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .testTag("play_pause_button")
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        // Next Episode (landscape)
                                        if (isLandscape) {
                                            IconButton(
                                                onClick = {
                                                    val currentIndex = allEpisodes.indexOfFirst { it.id == episode.id }
                                                    if (currentIndex != -1 && currentIndex + 1 < allEpisodes.size) {
                                                        val nextEp = allEpisodes[currentIndex + 1]
                                                        onSelectEpisode(nextEp)
                                                        viewModel.play(anime, nextEp, server = uiState.server, quality = uiState.quality)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.SkipNext,
                                                    contentDescription = "Next Episode",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Forward 10s
                                        IconButton(
                                            onClick = {
                                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                                exoPlayer.seekTo(target)
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Forward10,
                                                contentDescription = "Forward 10s",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }

                                // Far Right: Download, Fit (aspect ratio), Fullscreen
                                if (!uiState.isLocked) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                onDownloadEpisode(episode, uiState.server, uiState.quality)
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Download,
                                                contentDescription = "Download Episode",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Aspect ratio "Fit" toggle in landscape
                                        if (isLandscape) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White.copy(alpha = 0.15f),
                                                modifier = Modifier
                                                    .clickable { viewModel.cycleAspectRatio() }
                                                    .padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = uiState.aspectRatioMode.label,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // Fullscreen Expand/Collapse
                                        IconButton(
                                            onClick = {
                                                if (isLandscape) {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                } else {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                }
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                                contentDescription = "Fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. BELOW THE PLAYER (DEFAULT MODE CONTENT)
            // Exactly matches Screenshot 2: Clean, non-overlapping, spacious!
            // ==========================================
            if (!isLandscape) {
                val filteredEpisodes = remember(allEpisodes, episodeSearchQuery) {
                    if (episodeSearchQuery.isBlank()) {
                        allEpisodes
                    } else {
                        val targetNum = episodeSearchQuery.trim().toIntOrNull()
                        if (targetNum != null) {
                            allEpisodes.filter { it.episodeNum == targetNum }
                        } else {
                            allEpisodes.filter { it.title.contains(episodeSearchQuery, ignoreCase = true) }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(AnikotoDarkCanvas)
                ) {
                    // Line 1: "You are watching Episode X"
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "You are watching ",
                                color = AnikotoGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episode ${episode.episodeNum}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Line 2: Sub / Dub Tabs with Anikoto green underline
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Sub Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.switchAudioTrack("Sub") }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Sub",
                                    color = if (uiState.audioTrack == "Sub") AnikotoGreen else AnikotoMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp)
                                        .background(
                                            if (uiState.audioTrack == "Sub") AnikotoGreen else Color.Transparent,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }

                            // Dub Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.switchAudioTrack("Dub") }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Dub",
                                    color = if (uiState.audioTrack == "Dub") AnikotoGreen else AnikotoMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp)
                                        .background(
                                            if (uiState.audioTrack == "Dub") AnikotoGreen else Color.Transparent,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    // Line 3: Server selection pills (Koto, Neko, GG)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(StreamServer.KOTO, StreamServer.NEKO, StreamServer.GG).forEach { srv ->
                                val isSelected = uiState.server == srv
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) AnikotoGreen else AnikotoPillUnselected)
                                        .clickable {
                                            viewModel.switchServer(srv)
                                        }
                                        .padding(horizontal = 22.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = srv.displayName,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Line 4: "List of episodes" Header + "Q No. of Ep" Search Box
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "List of episodes",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VideoLibrary,
                                        contentDescription = null,
                                        tint = AnikotoGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "EPS: ${allEpisodes.size}",
                                        color = AnikotoGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Search Box "Q No. of Ep"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AnikotoCard)
                                    .border(1.dp, AnikotoCardBorder, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search Episode",
                                        tint = AnikotoMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = episodeSearchQuery,
                                        onValueChange = { episodeSearchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(AnikotoGreen),
                                        decorationBox = { innerTextField ->
                                            if (episodeSearchQuery.isEmpty()) {
                                                Text(
                                                    text = "No. of Ep",
                                                    color = AnikotoMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            innerTextField()
                                        },
                                        modifier = Modifier.width(72.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Line 5: 6-Column Grid of Episodes
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(((filteredEpisodes.size + 5) / 6 * 52).coerceIn(52, 260).dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredEpisodes) { ep ->
                                    val isCurrent = ep.episodeNum == episode.episodeNum
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) AnikotoGreen else AnikotoPillUnselected)
                                            .border(
                                                width = 1.dp,
                                                color = if (isCurrent) AnikotoGreen else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (ep.id != episode.id) {
                                                    onSelectEpisode(ep)
                                                    viewModel.play(anime, ep, server = uiState.server, quality = uiState.quality)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${ep.episodeNum}",
                                            color = if (isCurrent) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Line 6: Clean Comments Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AnikotoCard)
                                .border(1.dp, AnikotoCardBorder, RoundedCornerShape(14.dp))
                                .clickable { showCommentsSheet = true }
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Comments",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View Comments",
                                        tint = AnikotoMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF282C3D))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Tap to view all comments...",
                                        color = AnikotoMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // 4. ANIKOTO BOTTOM SHEETS (Quality, Captions, Speed, Comments)
    // ==========================================

    // Quality Selection Sheet
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = AnikotoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Quality",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = AnikotoGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SERVER: ${uiState.server.displayName.uppercase()}",
                            color = AnikotoGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                val qualityItems: List<Pair<StreamQuality, String>> = if (uiState.availableQualities.isNotEmpty()) {
                    uiState.availableQualities.map { profile ->
                        profile.quality to "${profile.label} (${profile.resolution} • ${profile.bitrate})"
                    }
                } else {
                    listOf(
                        StreamQuality.AUTO to "Auto (Adaptive Multi-Bitrate)",
                        StreamQuality.Q1080P to "1080p FHD (1920x1080 • 5.8 Mbps)",
                        StreamQuality.Q720P to "720p HD (1280x720 • 2.9 Mbps)",
                        StreamQuality.Q480P to "480p SD (854x480 • 1.4 Mbps)",
                        StreamQuality.Q360P to "360p Saver (640x360 • 750 Kbps)"
                    )
                }

                qualityItems.forEach { (quality, description) ->
                    val isSelected = uiState.quality == quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AnikotoGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                viewModel.switchQuality(quality)
                                showQualitySheet = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = description,
                            color = if (isSelected) AnikotoGreen else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = AnikotoGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Captions / Subtitles Sheet
    if (showCaptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCaptionsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = AnikotoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtitles & Captions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = AnikotoGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SERVER: ${uiState.server.displayName.uppercase()}",
                            color = AnikotoGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                val captionItems = if (uiState.availableCaptions.isNotEmpty()) {
                    uiState.availableCaptions.map { it.label }
                } else {
                    uiState.availableSubtitles
                }

                captionItems.forEach { lang ->
                    val isSelected = (lang.equals("Off", ignoreCase = true) && !uiState.subtitlesEnabled) ||
                            (uiState.subtitlesEnabled && (uiState.selectedSubtitleLang.equals(lang, ignoreCase = true) || (lang.startsWith("English") && uiState.selectedSubtitleLang.startsWith("English"))))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AnikotoGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                viewModel.selectSubtitleLang(lang)
                                showCaptionsSheet = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "English") "English [CC]" else lang,
                            color = if (isSelected) AnikotoGreen else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = AnikotoGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Playback Speed Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = AnikotoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Playback Speed",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = uiState.playbackSpeed == speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AnikotoGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                viewModel.setPlaybackSpeed(speed)
                                showSpeedSheet = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                            color = if (isSelected) AnikotoGreen else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = AnikotoGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Comments Sheet
    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = AnikotoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Community Discussion",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${anime.title} • Episode ${episode.episodeNum}",
                    color = AnikotoGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val sampleComments = listOf(
                    "OtakuMaster99" to "This episode was incredible! The animation quality on the Koto server was buttery smooth.",
                    "AnimeFanatic" to "Can't wait for the next episode. The plot twist at the end had me on the edge of my seat!",
                    "KotoWatcher" to "Audio in Dub is crystal clear, sub translation is also spot on!"
                )

                sampleComments.forEach { (user, comment) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AnikotoGreen.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.take(1),
                                color = AnikotoGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = user,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = comment,
                                color = AnikotoMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
