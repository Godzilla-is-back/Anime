package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Anime
import com.example.ui.components.PressableScale
import com.example.ui.theme.SakuraGlintAqua
import com.example.ui.theme.SakuraPinkGlow
import com.example.ui.theme.StreamGoodGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Calendar

data class ScheduleEntry(
    val anime: Anime,
    val dayOfWeek: String,
    val airTimeJst: String,
    val upcomingEpisode: Int,
    val countdownText: String,
    val isLiveToday: Boolean = false
)

@Composable
fun ScheduleScreen(
    animeList: List<Anime>,
    onSelectAnime: (Anime) -> Unit,
    onWatchAnime: (Anime) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    // Determine current day of week
    val currentDayIndex = remember {
        val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Calendar.MONDAY is 2, SUNDAY is 1
        when (calDay) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    var selectedDayIndex by remember { mutableStateOf(currentDayIndex) }
    val remindersMap = remember { mutableStateMapOf<Int, Boolean>() }

    // Map anime titles across the schedule
    val scheduleEntries = remember(animeList) {
        val list = mutableListOf<ScheduleEntry>()
        val safeList = if (animeList.isNotEmpty()) animeList else emptyList()

        safeList.forEachIndexed { index, anime ->
            val dayIdx = (anime.id + index) % 7
            val hour = 18 + (index % 6)
            val minute = (index * 15) % 60
            val timeStr = String.format("%02d:%02d JST", hour, minute)
            val isToday = dayIdx == currentDayIndex

            list.add(
                ScheduleEntry(
                    anime = anime,
                    dayOfWeek = days[dayIdx],
                    airTimeJst = timeStr,
                    upcomingEpisode = (anime.episodesCount.coerceAtLeast(1) - (index % 4)).coerceAtLeast(1),
                    countdownText = if (isToday) "Airing in ${((index * 3) % 7) + 1}h" else "Air Time $timeStr",
                    isLiveToday = isToday
                )
            )
        }
        list
    }

    val selectedDayShort = days[selectedDayIndex]
    val selectedDayFull = fullDays[selectedDayIndex]
    val entriesForSelectedDay = remember(scheduleEntries, selectedDayShort) {
        scheduleEntries.filter { it.dayOfWeek == selectedDayShort }
    }

    val darkCanvas = MaterialTheme.colorScheme.background
    val cardSurface = MaterialTheme.colorScheme.surfaceVariant
    val accentPink = SakuraPinkGlow
    val accentAqua = SakuraGlintAqua

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(darkCanvas)
            .testTag("schedule_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = accentPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Airing Schedule",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextWhite
                            )
                        }
                        Text(
                            text = "Weekly Simulcast Broadcasts • Live Updates",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Today Indicator
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1F00E676),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StreamGoodGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StreamGoodGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today: ${days[currentDayIndex]}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreamGoodGreen
                            )
                        }
                    }
                }
            }
        }

        // Days Selector Pills Row
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days.indices.toList()) { idx ->
                    val isSelected = idx == selectedDayIndex
                    val isToday = idx == currentDayIndex

                    PressableScale(
                        onClick = { selectedDayIndex = idx }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isSelected -> accentPink
                                isToday -> Color(0x22FF4081)
                                else -> cardSurface
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> accentPink
                                    isToday -> accentPink.copy(alpha = 0.6f)
                                    else -> Color(0x22FFFFFF)
                                }
                            ),
                            modifier = Modifier.testTag("schedule_day_${days[idx]}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = days[idx],
                                    color = if (isSelected) Color.White else TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                )
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else accentPink)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Day Section Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$selectedDayFull Lineup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Text(
                    text = "${entriesForSelectedDay.size} Shows Scheduled",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        // Schedule Cards
        if (entriesForSelectedDay.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📅", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No broadcasts scheduled for $selectedDayFull",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(entriesForSelectedDay, key = { "${it.anime.id}_${it.dayOfWeek}" }) { entry ->
                val anime = entry.anime
                val hasReminder = remindersMap[anime.id] ?: false

                PressableScale(
                    onClick = { onSelectAnime(anime) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (entry.isLiveToday) accentPink.copy(alpha = 0.5f) else Color(0x22FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover Thumbnail with Episode Badge
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(108.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = anime.coverUrl,
                                    contentDescription = anime.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                                    color = Color(0xCC000000),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "Ep ${entry.upcomingEpisode}",
                                        color = accentAqua,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Anime Metadata
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = anime.title,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${anime.studio} • ${anime.genres.take(2).joinToString(", ")}",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Airing Time Chip
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (entry.isLiveToday) Color(0x22FF4081) else Color(0x22FFFFFF)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = if (entry.isLiveToday) accentPink else TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = entry.airTimeJst,
                                            color = if (entry.isLiveToday) accentPink else TextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = entry.countdownText,
                                    color = if (entry.isLiveToday) StreamGoodGreen else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action: Quick Watch / Reminder
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = {
                                        remindersMap[anime.id] = !hasReminder
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (hasReminder) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "Toggle reminder",
                                        tint = if (hasReminder) accentPink else TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = accentPink,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable { onWatchAnime(anime) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Watch",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
