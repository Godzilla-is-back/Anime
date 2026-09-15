package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.AniKotoAnime
import com.example.data.remote.AniKotoRepository
import kotlinx.coroutines.launch

@Composable
fun AniKotoStreamingScreen(aniKotoRepository: AniKotoRepository) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AniKotoAnime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedAnime by remember { mutableStateOf<AniKotoAnime?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27))
    ) {
        if (selectedAnime == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.foundation.background(
                                    Color(0xFF1A1F3A)
                                ).brush ?: androidx.compose.foundation.brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1A1F3A),
                                        Color(0xFF0A0E27)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AniKoto Streaming",
                            fontSize = 28.sp,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Search Bar
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = aniKotoRepository.searchAnime(searchQuery)
                                        result.onSuccess { animes ->
                                            searchResults = animes
                                            isLoading = false
                                        }
                                        result.onFailure { exception ->
                                            errorMessage = exception.message
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Error Message
                if (errorMessage != null) {
                    item {
                        ErrorBanner(message = errorMessage!!) {
                            errorMessage = null
                        }
                    }
                }
                
                // Loading Indicator
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF6B6B),
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }
                
                // Search Results
                if (searchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Search Results (${searchResults.size})",
                            fontSize = 18.sp,
                            color = Color(0xFFF5EEDC),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    items(searchResults.chunked(2)) { animeRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            animeRow.forEach { anime ->
                                AnimeCard(
                                    anime = anime,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(280.dp),
                                    onClick = { selectedAnime = anime }
                                )
                            }
                            if (animeRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            AnimeDetailScreen(
                anime = selectedAnime!!,
                aniKotoRepository = aniKotoRepository,
                onBackClick = { selectedAnime = null }
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp)),
        placeholder = { Text("Search anime...", color = Color(0xFF9CA3AF)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF9CA3AF)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1F2937),
            unfocusedContainerColor = Color(0xFF1F2937),
            focusedTextColor = Color(0xFFF5EEDC),
            unfocusedTextColor = Color(0xFFF5EEDC),
            focusedIndicatorColor = Color(0xFFFF6B6B),
            unfocusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        singleLine = true
    )
}

@Composable
fun AnimeCard(
    anime: AniKotoAnime,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1F2937))
            .clickable(onClick = onClick)
    ) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF111827))
        ) {
            if (anime.image != null) {
                AsyncImage(
                    model = anime.image,
                    contentDescription = anime.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF374151)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Image", color = Color(0xFF9CA3AF))
                }
            }
            
            // Rating Badge
            if (anime.rating != null && anime.rating!! > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = Color(0xFFFF6B6B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "★ ${String.format("%.1f", anime.rating)}",
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
        
        // Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = anime.title ?: "Unknown",
                fontSize = 14.sp,
                color = Color(0xFFF5EEDC),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                anime.type?.let {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF374151))
                            .padding(4.dp)
                    )
                }
                anime.status?.let {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = Color(0xFF4ADE80),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF374151))
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFFDC2626),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AnimeDetailScreen(
    anime: AniKotoAnime,
    aniKotoRepository: AniKotoRepository,
    onBackClick: () -> Unit
) {
    var episodes by remember { mutableStateOf<List<com.example.data.remote.AniKotoEpisode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(true) }
    var selectedEpisode by remember { mutableStateOf<com.example.data.remote.AniKotoEpisode?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(anime.id) {
        anime.id?.let {
            coroutineScope.launch {
                val result = aniKotoRepository.getEpisodes(it)
                result.onSuccess { eps ->
                    episodes = eps
                    isLoadingEpisodes = false
                }
                result.onFailure {
                    isLoadingEpisodes = false
                }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27))
    ) {
        // Back Button & Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1F3A))
                    .padding(16.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = anime.title ?: "Anime Details",
                    fontSize = 24.sp,
                    color = Color(0xFFF5EEDC)
                )
            }
        }
        
        // Anime Cover
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color(0xFF111827))
            ) {
                if (anime.cover != null) {
                    AsyncImage(
                        model = anime.cover,
                        contentDescription = anime.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (anime.image != null) {
                    AsyncImage(
                        model = anime.image,
                        contentDescription = anime.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        // Info Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(16.dp)
            ) {
                if (anime.rating != null && anime.rating!! > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rating: ★ ${String.format("%.1f", anime.rating)}/10",
                            fontSize = 14.sp,
                            color = Color(0xFFFF6B6B)
                        )
                    }
                }
                
                anime.description?.let {
                    Text(
                        text = "Description",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = Color(0xFFF5EEDC),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    anime.type?.let {
                        InfoChip(label = "Type", value = it)
                    }
                    anime.status?.let {
                        InfoChip(label = "Status", value = it)
                    }
                }
            }
        }
        
        // Episodes Section
        item {
            Text(
                text = "Episodes",
                fontSize = 18.sp,
                color = Color(0xFFF5EEDC),
                modifier = Modifier.padding(16.dp)
            )
        }
        
        if (isLoadingEpisodes) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF6B6B))
                }
            }
        } else if (episodes.isEmpty()) {
            item {
                Text(
                    text = "No episodes available",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(episodes) { episode ->
                EpisodeItem(
                    episode = episode,
                    onClick = { selectedEpisode = episode }
                )
            }
        }
    }
    
    if (selectedEpisode != null) {
        EpisodeStreamModal(
            episode = selectedEpisode!!,
            anime = anime,
            aniKotoRepository = aniKotoRepository,
            onDismiss = { selectedEpisode = null }
        )
    }
}

@Composable
fun EpisodeItem(
    episode: com.example.data.remote.AniKotoEpisode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2937))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111827))
        ) {
            if (episode.image != null) {
                AsyncImage(
                    model = episode.image,
                    contentDescription = "Episode ${episode.number}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF374151)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EP ${episode.number}",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Episode ${episode.number}",
                fontSize = 14.sp,
                color = Color(0xFFF5EEDC)
            )
            episode.title?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.Close.apply { 
                // This is a placeholder, replace with play icon if available
            },
            contentDescription = "Play",
            tint = Color(0xFFFF6B6B),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF9CA3AF)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFFFF6B6B),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF374151))
                .padding(4.dp)
        )
    }
}

@Composable
fun EpisodeStreamModal(
    episode: com.example.data.remote.AniKotoEpisode,
    anime: AniKotoAnime,
    aniKotoRepository: AniKotoRepository,
    onDismiss: () -> Unit
) {
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingStream by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(episode.number) {
        anime.id?.let { id ->
            episode.number?.let { epNum ->
                coroutineScope.launch {
                    val result = aniKotoRepository.getSubbedStream(id, epNum)
                    result.onSuccess { streamResponse ->
                        streamUrl = streamResponse.sources?.firstOrNull()?.url
                        isLoadingStream = false
                    }
                    result.onFailure {
                        isLoadingStream = false
                    }
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Episode ${episode.number}", color = Color(0xFFF5EEDC))
        },
        text = {
            Column {
                if (isLoadingStream) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (streamUrl != null) {
                    Text(
                        text = "Stream ready! Open in player",
                        color = Color(0xFF4ADE80),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Stream not available",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B)
                )
            ) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1F2937),
        tonalElevation = 0.dp
    )
}
