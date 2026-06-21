package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.SongEntity
import com.example.viewmodel.MusicViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onCloseClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    val favs by viewModel.favorites.collectAsState()
    val isFav = currentSong?.let { song -> favs.any { it.id == song.id } } ?: false

    var currentTabState by remember { mutableStateOf(0) } // 0: Player Controls, 1: Embedded Lyrics
    var showQueueDrawer by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }

    // Sliding seek assist (prevents skipping while user holds slider)
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var sliderDraggedValue by remember { mutableStateOf(0f) }

    if (currentSong == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select a track from Library", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val song = currentSong!!

    // Parse Lyrics Phase 1 (Time synchronized rendering based on LRT structure)
    val parsedLyricsList = remember(song.lyricText) {
        parseLrcLyrics(song.lyricText ?: "No local lyrics loaded in metadata.")
    }

    // Active synchronized lyric line
    val activeLyricIndex = remember(progress, parsedLyricsList) {
        var activeIdx = -1
        for (i in parsedLyricsList.indices) {
            if (progress >= parsedLyricsList[i].timestampMs) {
                activeIdx = i
            } else {
                break
            }
        }
        activeIdx
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header actions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize full player")
                }

                // Player vs Lyrics category toggle pill
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = currentTabState == 0,
                        onClick = { currentTabState = 0 },
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ) {
                        Text("Player", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = currentTabState == 1,
                        onClick = { currentTabState = 1 },
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ) {
                        Text("Lyrics", fontSize = 12.sp)
                    }
                }

                IconButton(onClick = { showSongInfoDialog = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = "Track metadata info")
                }
            }

            // Crossfade tab content
            AnimatedContent(
                targetState = currentTabState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { tabIndex ->
                if (tabIndex == 0) {
                    // TAB 0: PLAYER CONTROLS
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big Album artwork card
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                                if (song.albumArtUri != null && song.albumArtUri.startsWith("http")) {
                                    AsyncImage(
                                        model = song.albumArtUri,
                                        contentDescription = "Cover Album Art",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(96.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }

                        // Title, Artist, & Favorite Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleFavorite(song.id) },
                                modifier = Modifier.testTag("favorite_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite Toggle",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // Progress Seek Bar
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                            val activeVal = if (isUserDraggingSlider) sliderDraggedValue else progress.toFloat()
                            val totalVal = if (duration > 0) duration.toFloat() else 1f

                            Slider(
                                value = activeVal,
                                onValueChange = {
                                    isUserDraggingSlider = true
                                    sliderDraggedValue = it
                                },
                                onValueChangeFinished = {
                                    isUserDraggingSlider = false
                                    viewModel.seekTo(sliderDraggedValue.toLong())
                                },
                                valueRange = 0f..totalVal,
                                modifier = Modifier.testTag("track_seeker")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTimeToMinutesSeconds(activeVal.toLong()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatTimeToMinutesSeconds(duration),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Core Media Action Buttons (Previous, Play/Pause, Next)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Repeat modes
                            IconButton(onClick = {
                                val nextMode = when (repeatMode) {
                                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                }
                                viewModel.setRepeatMode(nextMode)
                            }) {
                                val icon = when (repeatMode) {
                                    androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    androidx.media3.common.Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                    else -> Icons.Default.Repeat
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.previous() },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Skip Previous", modifier = Modifier.size(36.dp))
                            }

                            FloatingActionButton(
                                onClick = { viewModel.togglePlayPause() },
                                shape = RoundedCornerShape(28.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("play_pause_fab")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play or Pause",
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.next() },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip Next", modifier = Modifier.size(36.dp))
                            }

                            // Shuffle mode
                            IconButton(onClick = { viewModel.toggleShuffle() }) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Playback Speed Slider
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Playback Speed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = String.format("%.1fx", playbackSpeed),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = playbackSpeed,
                                    onValueChange = { viewModel.setPlaybackSpeed(it) },
                                    valueRange = 0.5f..2.0f,
                                    steps = 5,
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }

                        // Bottom utility buttons (Equalizer, Queue, Sleep Timer)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateToEqualizer,
                                modifier = Modifier.testTag("eq_btn")
                            ) {
                                Icon(Icons.Default.Equalizer, contentDescription = "10-Band Equalizer")
                            }

                            IconButton(onClick = onOpenSleepTimer) {
                                Icon(Icons.Outlined.Timer, contentDescription = "Sleep Timer")
                            }

                            IconButton(onClick = { showQueueDrawer = true }) {
                                Icon(Icons.Default.QueueMusic, contentDescription = "Track Queue list")
                            }
                        }
                    }
                } else {
                    // TAB 1: EMBEDDED METADATA LYRICS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "Embedded Metadata Lyrics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (parsedLyricsList.isEmpty() || (parsedLyricsList.size == 1 && parsedLyricsList[0].text.startsWith("No local"))) {
                            // Empty lyrics placeholder or text editor
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Lyrics,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No Embedded Lyrics Found",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Add LRC formatted or plain text lyrics offline to show synched lyric lines.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                var newLyrics by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = newLyrics,
                                    onValueChange = { newLyrics = it },
                                    label = { Text("Offline Lyric Tags") },
                                    placeholder = { Text("[00:05.00] Line 1\n[00:10.00] Line 2") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .padding(top = 12.dp)
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateLyrics(song.id, newLyrics)
                                    },
                                    modifier = Modifier.padding(top = 10.dp)
                                ) {
                                    Text("Add Lyrics")
                                }
                            }
                        } else {
                            // Synced Lyrics Lazy list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 70.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(parsedLyricsList.size) { index ->
                                    val line = parsedLyricsList[index]
                                    val isHighlighted = index == activeLyricIndex
                                    Text(
                                        text = line.text,
                                        fontSize = if (isHighlighted) 22.sp else 16.sp,
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.seekTo(line.timestampMs) }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Song Information Dialog
        if (showSongInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSongInfoDialog = false },
                title = { Text("Track Metadata Info") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Title: ${song.title}", fontWeight = FontWeight.Bold)
                        Text("Artist: ${song.artist}")
                        Text("Album: ${song.album}")
                        Text("Duration: ${formatTimeToMinutesSeconds(song.duration)}")
                        Text("Track Number: ${song.trackNumber}")
                        Divider()
                        Text("File Engine details (Offline):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Path: ${song.filePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Format: High-Definition Audio (MP3/FLAC)", fontSize = 11.sp)
                        Text("Source: Android Local MediaStore / Offline Assets", fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { showSongInfoDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Play Queue Dialog
        if (showQueueDrawer) {
            val playQueue by viewModel.playQueue.collectAsState()
            AlertDialog(
                onDismissRequest = { showQueueDrawer = false },
                title = { Text("Active Playlist Queue") },
                text = {
                    Column(modifier = Modifier.height(300.dp)) {
                        if (playQueue.isEmpty()) {
                            Text("Queue is empty")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(playQueue) { qSong ->
                                    val isActive = qSong.id == song.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.playSong(qSong, playQueue) }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qSong.title,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(qSong.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isActive) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showQueueDrawer = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

// Phase 1 LRC Parser
data class LyricLine(val timestampMs: Long, val text: String)

fun parseLrcLyrics(lrcText: String): List<LyricLine> {
    val list = mutableListOf<LyricLine>()
    val lines = lrcText.split("\n")
    val regex = "\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)".toRegex()

    for (line in lines) {
        val match = regex.find(line.trim())
        if (match != null) {
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong() * 10 // e.g. .50 is 500ms
            val text = match.groupValues[4].trim()

            val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
            list.add(LyricLine(totalMs, text))
        } else if (line.isNotBlank()) {
            // Backup parsing of plain lines (without sync markers)
            list.add(LyricLine(0L, line.trim()))
        }
    }
    return list.sortedBy { it.timestampMs }
}

fun formatTimeToMinutesSeconds(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
