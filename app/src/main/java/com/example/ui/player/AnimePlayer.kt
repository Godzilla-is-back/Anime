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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import androidx.media3.common.MediaItem
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
import com.example.ui.components.PressableScale
import com.example.ui.theme.SakuraGlintAqua
import com.example.ui.theme.SakuraPinkGlow
import com.example.ui.theme.StreamGoodGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Modern, smooth, bigger-screen anime video player screen powered by VideoPlayerViewModel.
 */
@Composable
fun AnimePlayerScreen(
    anime: Anime,
    episode: Episode,
    allEpisodes: List<Episode>,
    initialPositionMs: Long = 0L,
    initialServer: StreamServer = StreamServer.ANIKOTO,
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
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) } // "-10s" or "+10s"

    // Bottom Sheets
    var showServerSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }

    // Colors
    val neonAccent = SakuraGlintAqua
    val neonPink = SakuraPinkGlow
    val darkBackground = Color(0xFF07050E)
    val cardBackground = Color(0xFF130F20)

    // Setup ExoPlayer
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AniHub/2.0 (Linux; Android 14; Mobile; DynamicExoPlayer)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15000, 50000, 1500, 3000)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(showControls, lastInteractionTime, uiState.isPlaying) {
        if (showControls && uiState.isPlaying && !uiState.isLocked) {
            delay(3500)
            showControls = false
        }
    }

    // Listen to ExoPlayer playback events
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
                        // Auto-play next episode if available
                        val currentIndex = allEpisodes.indexOfFirst { it.id == episode.id }
                        if (currentIndex != -1 && currentIndex + 1 < allEpisodes.size) {
                            onSelectEpisode(allEpisodes[currentIndex + 1])
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.updateBuffering(false)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll position updates
    LaunchedEffect(uiState.isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val current = exoPlayer.currentPosition
                val total = exoPlayer.duration
                viewModel.updateProgress(current, total)
                uiState.episode?.let { ep ->
                    onSaveProgress(ep, current, total, uiState.server.displayName)
                }
            }
            delay(500)
        }
    }

    // Handle Immersive Mode in Landscape
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

    // Back Handler: return to portrait if in landscape, or pop back
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    // Trigger Play request on ViewModel whenever anime, episode, or initial props change
    LaunchedEffect(anime.id, episode.id) {
        viewModel.play(
            anime = anime,
            episode = episode,
            server = initialServer,
            quality = initialQuality,
            initialPositionMs = initialPositionMs
        )
    }

    // React to ResolvedStream from ViewModel
    LaunchedEffect(uiState.resolvedStream) {
        val stream = uiState.resolvedStream
        if (stream != null) {
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
        }
    }

    // Double tap feedback timer
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(750)
            doubleTapFeedback = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .testTag("anime_player_container")
    ) {
        // Player Surface Layout
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Player Box: Supports Standard 16:9 or Expansive Bigger Screen Mode in portrait!
            val playerModifier = if (isLandscape) {
                Modifier.fillMaxSize()
            } else if (uiState.isBiggerScreen) {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f) // Theater / Bigger Screen height!
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
                                if (!uiState.isLocked) {
                                    showControls = !showControls
                                    lastInteractionTime = System.currentTimeMillis()
                                } else {
                                    // Flash unlock icon if tapped while locked
                                    showControls = true
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            },
                            onDoubleTap = { offset ->
                                if (!uiState.isLocked) {
                                    val width = size.width
                                    if (offset.x < width / 2) {
                                        // Left double tap: Replay 10s
                                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                        exoPlayer.seekTo(newPos)
                                        doubleTapFeedback = "-10s"
                                    } else {
                                        // Right double tap: Forward 10s
                                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                        exoPlayer.seekTo(newPos)
                                        doubleTapFeedback = "+10s"
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            }
                        )
                    }
            ) {
                // ExoPlayer AndroidView
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            resizeMode = when (uiState.aspectRatioMode) {
                                VideoAspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                VideoAspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                VideoAspectRatioMode.THEATER_EXPAND -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                VideoAspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            }
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                        view.resizeMode = when (uiState.aspectRatioMode) {
                            VideoAspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            VideoAspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            VideoAspectRatioMode.THEATER_EXPAND -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            VideoAspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Double Tap Animated Ripple Feedback Indicator
                if (doubleTapFeedback != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, neonAccent),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = doubleTapFeedback!!,
                                    color = TextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Buffering / Loading / Server Handshake Overlay
                if (uiState.isBuffering || uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.errorMessage != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Playback Error",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = uiState.errorMessage!!,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = neonAccent,
                                        modifier = Modifier.clickable { viewModel.retry() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry Stream", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = cardBackground,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable { showServerSheet = true }
                                    ) {
                                        Text(
                                            "Switch Server",
                                            color = TextWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Dynamic Live Server Query HUD
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = neonAccent,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connecting to ${uiState.server.displayName}...",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fetching episode stream from API",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Controls Overlay with animated visibility
                this@Column.AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(220))
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
                        // Top Bar: Back, Anime & Episode Title, Server/Quality Badges, Settings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isLandscape) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier.testTag("player_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextWhite
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.title,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Ep ${episode.episodeNum} • ${episode.title}",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Live Server badge
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = neonAccent.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, neonAccent.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable { showServerSheet = true }
                                    ) {
                                        Text(
                                            text = "${uiState.server.shortName} • ${uiState.resolvedStream?.latencyMs ?: uiState.server.pingMs}ms",
                                            color = neonAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Sub / Dub Audio Toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    val nextTrack = if (uiState.audioTrack == "Sub") "Dub" else "Sub"
                                    viewModel.switchAudioTrack(nextTrack)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) {
                                Text(
                                    text = uiState.audioTrack,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Bigger Screen / Theater Mode Toggle (in portrait)
                            if (!isLandscape) {
                                IconButton(
                                    onClick = {
                                        viewModel.toggleBiggerScreen()
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.testTag("toggle_bigger_screen_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isBiggerScreen) Icons.Filled.FitScreen else Icons.Filled.ZoomOutMap,
                                        contentDescription = "Bigger Screen",
                                        tint = if (uiState.isBiggerScreen) neonAccent else TextWhite
                                    )
                                }
                            }

                            // Aspect Ratio Mode Cycle
                            IconButton(
                                onClick = {
                                    viewModel.cycleAspectRatio()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = TextWhite
                                )
                            }

                            // Lock Screen Toggle
                            IconButton(
                                onClick = {
                                    viewModel.toggleLock()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                    contentDescription = "Lock Controls",
                                    tint = if (uiState.isLocked) neonPink else TextWhite
                                )
                            }
                        }

                        // Center Controls: Skip -10s, Play/Pause, Skip +10s (hidden if locked)
                        if (!uiState.isLocked) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Episode or Replay 10s
                                IconButton(
                                    onClick = {
                                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                        exoPlayer.seekTo(newPos)
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Replay10,
                                        contentDescription = "Replay 10s",
                                        tint = TextWhite,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                // Main Play / Pause Button
                                Surface(
                                    shape = CircleShape,
                                    color = neonAccent,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable {
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                exoPlayer.play()
                                            }
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                        .testTag("play_pause_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                // Forward 10s
                                IconButton(
                                    onClick = {
                                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                        exoPlayer.seekTo(newPos)
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = TextWhite,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Controls Bar: Progress Slider, Duration, Quality, Speed, Fullscreen
                        if (!uiState.isLocked) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                // Progress Slider and Time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTime(uiState.currentPositionMs),
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Slider(
                                        value = uiState.currentPositionMs.toFloat(),
                                        onValueChange = { newPos ->
                                            lastInteractionTime = System.currentTimeMillis()
                                            exoPlayer.seekTo(newPos.toLong())
                                        },
                                        valueRange = 0f..(uiState.durationMs.coerceAtLeast(1L)).toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = neonAccent,
                                            activeTrackColor = neonAccent,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                            .testTag("playback_slider")
                                    )

                                    Text(
                                        text = formatTime(uiState.durationMs),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                // Quick Actions Row: Server, Quality, Speed, Subtitle, Fullscreen
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Server Selector Button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.12f),
                                            modifier = Modifier.clickable {
                                                showServerSheet = true
                                                lastInteractionTime = System.currentTimeMillis()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.SignalCellularAlt, contentDescription = null, tint = StreamGoodGreen, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(uiState.server.displayName, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        // Quality Button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.12f),
                                            modifier = Modifier.clickable {
                                                showQualitySheet = true
                                                lastInteractionTime = System.currentTimeMillis()
                                            }
                                        ) {
                                            Text(
                                                uiState.quality.label,
                                                color = TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Speed Button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.12f),
                                            modifier = Modifier.clickable {
                                                showSpeedSheet = true
                                                lastInteractionTime = System.currentTimeMillis()
                                            }
                                        ) {
                                            Text(
                                                "${uiState.playbackSpeed}x",
                                                color = TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Subtitle Toggle
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleSubtitles()
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.subtitlesEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                                                contentDescription = "Subtitles",
                                                tint = if (uiState.subtitlesEnabled) neonAccent else TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Mute Toggle
                                        IconButton(
                                            onClick = {
                                                isMuted = !isMuted
                                                exoPlayer.volume = if (isMuted) 0f else 1f
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = "Audio Volume",
                                                tint = TextWhite,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Right controls: Next Episode & Fullscreen Landscape
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val currentIndex = allEpisodes.indexOfFirst { it.id == episode.id }
                                        if (currentIndex != -1 && currentIndex + 1 < allEpisodes.size) {
                                            IconButton(
                                                onClick = {
                                                    onSelectEpisode(allEpisodes[currentIndex + 1])
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.SkipNext,
                                                    contentDescription = "Next Episode",
                                                    tint = TextWhite,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                activity?.let { act ->
                                                    if (isLandscape) {
                                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                    } else {
                                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("fullscreen_toggle_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                                contentDescription = "Toggle Fullscreen",
                                                tint = TextWhite,
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

            // In Portrait mode: Show Episode list, Server Selector panel, and Anime Info below video
            if (!isLandscape) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(darkBackground),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title, Status, and Action Buttons
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = anime.title,
                                        color = TextWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Episode ${episode.episodeNum}: ${episode.title}",
                                        color = neonAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = neonAccent.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, neonAccent.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable {
                                        onDownloadEpisode(episode, uiState.server, uiState.quality)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null, tint = neonAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", color = neonAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Live Server Status Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cardBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(StreamGoodGreen, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Server: ${uiState.server.displayName}",
                                                color = TextWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "${uiState.resolvedStream?.latencyMs ?: uiState.server.pingMs}ms Ping",
                                            color = StreamGoodGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Routing Node: ${uiState.resolvedStream?.serverNode ?: "Edge Gateway"}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Server Quick Switch Row
                    item {
                        Column {
                            Text(
                                text = "AVAILABLE STREAMING SERVERS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(StreamServer.values()) { server ->
                                    val isSelected = server == uiState.server
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) neonAccent.copy(alpha = 0.2f) else cardBackground,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) neonAccent else Color.White.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier.clickable { viewModel.switchServer(server) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(if (isSelected) neonAccent else StreamGoodGreen, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = server.displayName,
                                                color = if (isSelected) neonAccent else TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Episodes List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EPISODES (${allEpisodes.size})",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(allEpisodes) { ep ->
                        val isCurrent = ep.id == episode.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) cardBackground else cardBackground.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isCurrent) neonAccent else Color.White.copy(alpha = 0.05f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectEpisode(ep) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) neonAccent else Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ep.episodeNum.toString(),
                                            color = if (isCurrent) Color.Black else TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Episode ${ep.episodeNum}",
                                        color = if (isCurrent) neonAccent else TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = ep.title,
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isCurrent) {
                                    Text(
                                        text = "PLAYING",
                                        color = neonAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Server Selection
    if (showServerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showServerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = cardBackground
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Streaming Server",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Each server routes queries through high-speed edge nodes.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                StreamServer.values().forEach { server ->
                    val selected = server == uiState.server
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) neonAccent.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) neonAccent else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.switchServer(server)
                                showServerSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(server.displayName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(server.tag, color = TextMuted, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${server.pingMs}ms", color = StreamGoodGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (selected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = neonAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Bottom Sheet: Quality Selection
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = cardBackground
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Video Quality",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                StreamQuality.values().forEach { q ->
                    val selected = q == uiState.quality
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) neonAccent.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) neonAccent else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.switchQuality(q)
                                showQualitySheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(q.label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = neonAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Bottom Sheet: Playback Speed
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = cardBackground
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Playback Speed",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val selected = uiState.playbackSpeed == speed
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) neonAccent.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) neonAccent else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setPlaybackSpeed(speed)
                                exoPlayer.playbackParameters = PlaybackParameters(speed)
                                showSpeedSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${speed}x", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = neonAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
