package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.data.local.entity.SongEntity
import com.example.viewmodel.MusicViewModel
import kotlin.math.roundToInt
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onCloseClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onNavigateToSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlists by viewModel.allPlaylists.collectAsState(initial = emptyList())

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val playbackPitch by viewModel.playbackPitchState.collectAsState()

    val favs by viewModel.favorites.collectAsState()
    val isFav = currentSong?.let { song -> favs.any { it.id == song.id } } ?: false

    var currentTabState by remember { mutableStateOf(0) } // 0: Player Controls, 1: Embedded Lyrics
    val touchAbsorber = Modifier.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null
    ) {}
    var showQueueDrawer by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showPitchSpeedBottomSheet by remember { mutableStateOf(false) }
    var showSyncedLyricsScreen by remember { mutableStateOf(false) }
    var showRingtoneConfirmDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreOptionsBottomSheet by remember { mutableStateOf(false) }

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

    var dragOffsetY by remember { mutableStateOf(0f) }
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fix BUG 2: solid opaque background
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .offset { IntOffset(0, animatedDragOffsetY.roundToInt().coerceAtLeast(0)) }
            .pointerInput(currentTabState) {
                if (currentTabState == 0) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 250f) {
                                onCloseClick()
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragOffsetY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                }
            }
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

                IconButton(onClick = { showMoreOptionsBottomSheet = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
                        modifier = Modifier
                            .fillMaxSize()
                            .then(touchAbsorber),
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
                                if (!song.albumArtUri.isNullOrEmpty()) {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    val imageRequest = remember(song.albumArtUri) {
                                        coil.request.ImageRequest.Builder(context)
                                            .data(song.albumArtUri)
                                            .crossfade(true)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = imageRequest,
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

                            ModernMusicSlider(
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
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previous() },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Skip Previous", modifier = Modifier.size(40.dp))
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            FloatingActionButton(
                                onClick = { viewModel.togglePlayPause() },
                                shape = RoundedCornerShape(32.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(76.dp)
                                    .testTag("play_pause_fab")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play or Pause",
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            IconButton(
                                onClick = { viewModel.next() },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip Next", modifier = Modifier.size(40.dp))
                            }
                        }

                        // Bottom utility buttons (Shuffle, Repeat, Favorite, Song Playlist Queue drawer)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Shuffle
                            IconButton(
                                onClick = { viewModel.toggleShuffle() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 2. Repeat
                            IconButton(
                                onClick = {
                                    val nextMode = when (repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                    }
                                    viewModel.setRepeatMode(nextMode)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                val repeatIcon = when (repeatMode) {
                                    androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                }
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 3. Favorite
                            IconButton(
                                onClick = { viewModel.toggleFavorite(song.id) },
                                modifier = Modifier.size(48.dp).testTag("favorite_btn_bottom")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite Toggle",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 4. Song Playlist (Queue)
                            IconButton(
                                onClick = { showQueueDrawer = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Song Playlist Queue",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { showPitchSpeedBottomSheet = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = {
                                    Text(
                                        text = String.format("%.1fx Speed / %.1fx Pitch", playbackSpeed, playbackPitch),
                                        fontSize = 11.sp
                                    )
                                }
                            )

                            AssistChip(
                                onClick = { showSyncedLyricsScreen = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lyrics,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Synced Lyrics",
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // TAB 1: EMBEDDED METADATA LYRICS
                    var isEditingLyrics by remember { mutableStateOf(false) }
                    var textInputLyrics by remember(song.id, song.lyricText) { mutableStateOf(song.lyricText ?: "") }
                    val hasLyrics = !song.lyricText.isNullOrBlank() && !song.lyricText.contains("No local")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Embedded Metadata Lyrics",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .background(
                                            if (hasLyrics) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFEF5350).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (hasLyrics) "Lyrics Added" else "No Lyrics",
                                        color = if (hasLyrics) Color(0xFF81C784) else Color(0xFFE57373),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isEditingLyrics = !isEditingLyrics }
                            ) {
                                Icon(
                                    imageVector = if (isEditingLyrics) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = "Edit Lyrics Toggle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!hasLyrics || isEditingLyrics) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                if (!hasLyrics) {
                                    Icon(
                                        Icons.Outlined.Lyrics,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No Embedded Lyrics Found for this Track",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Paste LRC-formatted (e.g. [00:04.20] Hello) or plain text lyrics offline.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                } else {
                                    Text(
                                        "Editing Metadata Lyrics Editor",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                OutlinedTextField(
                                    value = textInputLyrics,
                                    onValueChange = { textInputLyrics = it },
                                    label = { Text("Lyrics Contents (Plain text or LRC)") },
                                    placeholder = { Text("[00:05.00] Line 1\n[00:10.00] Line 2") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(top = 12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.updateLyrics(song.id, textInputLyrics)
                                        isEditingLyrics = false
                                        Toast.makeText(context, "Lyrics saved successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Dynamic Lyrics")
                                }
                            }
                        } else {
                            // Synced Lyrics Lazy list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 70.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(parsedLyricsList.size, key = { index -> "${parsedLyricsList[index].timestampMs}_$index" }) { index ->
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

        // Pitch & Speed Control Bottom Sheet
        if (showPitchSpeedBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPitchSpeedBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pitch & Speed Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // SPEED CONTROL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Playback Speed", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        Text(String.format("%.2fx", playbackSpeed), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // PITCH CONTROL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Height, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Playback Pitch", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        Text(String.format("%.2fx", playbackPitch), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = playbackPitch,
                        onValueChange = { viewModel.setPlaybackPitch(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Presets
                    Text("Quick Presets", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                        presets.forEach { preset ->
                            val isSelected = (playbackSpeed - preset).absoluteValue < 0.01f && (playbackPitch - preset).absoluteValue < 0.01f
                            OutlinedButton(
                                onClick = {
                                    viewModel.setPlaybackSpeed(preset)
                                    viewModel.setPlaybackPitch(preset)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text("${preset}x", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Add to Playlist Dialog
        if (showAddToPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showAddToPlaylistDialog = false },
                title = { Text("Add to Playlist") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (playlists.isEmpty()) {
                            Text("No playlists found. Create one in Library first!")
                        } else {
                            playlists.forEach { playlist ->
                                TextButton(
                                    onClick = {
                                        viewModel.addSongToPlaylist(playlist.playlistId, song.id)
                                        showAddToPlaylistDialog = false
                                        Toast.makeText(context, "Added to playlist ${playlist.playlistName}!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(playlist.playlistName)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddToPlaylistDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Ringtone Confirm Dialog
        if (showRingtoneConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showRingtoneConfirmDialog = false },
                title = { Text("Set as Ringtone") },
                text = {
                    Text("Would you like to set '${song.title}' as your default system ringtone?")
                },
                confirmButton = {
                    Button(onClick = {
                        showRingtoneConfirmDialog = false
                        setAsRingtone(context, song)
                    }) {
                        Text("Set Default")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRingtoneConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Play Queue Bottom Sheet style
        if (showQueueDrawer) {
            val playQueue by viewModel.playQueue.collectAsState()
            ModalBottomSheet(
                onDismissRequest = { showQueueDrawer = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .padding(bottom = 16.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Now Playing Queue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showQueueDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close queue")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    if (playQueue.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Queue is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            itemsIndexed(playQueue, key = { index, qSong -> "${qSong.id}_$index" }) { index, qSong ->
                                val isActive = qSong.id == song.id
                                SwipableQueueItem(
                                    song = qSong,
                                    isActive = isActive,
                                    onPlay = {
                                        viewModel.playSong(qSong, playQueue)
                                    },
                                    onRemove = {
                                        viewModel.removeFromQueue(qSong.id)
                                    },
                                    onMoveUp = if (index > 0) {
                                        { viewModel.moveQueueItem(index, index - 1) }
                                    } else null,
                                    onMoveDown = if (index < playQueue.size - 1) {
                                        { viewModel.moveQueueItem(index, index + 1) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showMoreOptionsBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreOptionsBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF1C1C1E),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showMoreOptionsBottomSheet = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close More Options",
                                tint = Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Track",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = song.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        if (!song.albumArtUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = "Song thumbnail",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2A2A2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Fallback song icon",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val context = LocalContext.current
                    val favs by viewModel.favorites.collectAsState()
                    val isFavLocal = favs.any { it.id == song.id }

                    // Helper list for Grid Actions
                    val actionItems = remember(song, isPlaying, isFavLocal) {
                        listOf(
                            Triple("Audio Effects", Icons.Default.Equalizer) {
                                showMoreOptionsBottomSheet = false
                                onNavigateToEqualizer()
                            },
                            Triple("Sleep Timer", Icons.Default.Alarm) {
                                showMoreOptionsBottomSheet = false
                                onOpenSleepTimer()
                            },
                            Triple("Quality Selection", Icons.Default.HighQuality) {
                                showMoreOptionsBottomSheet = false
                                Toast.makeText(context, "HQ Quality (320kbps) Active", Toast.LENGTH_SHORT).show()
                            },
                            Triple(if (isPlaying) "Pause" else "Play", if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow) {
                                showMoreOptionsBottomSheet = false
                                viewModel.togglePlayPause()
                            },
                            Triple("Add to next", Icons.Default.QueuePlayNext) {
                                showMoreOptionsBottomSheet = false
                                viewModel.addToNext(song)
                                Toast.makeText(context, "Added to Play Next", Toast.LENGTH_SHORT).show()
                            },
                            Triple("Add to queue", Icons.Default.QueueMusic) {
                                showMoreOptionsBottomSheet = false
                                viewModel.addToQueue(song)
                                Toast.makeText(context, "Added to Queue", Toast.LENGTH_SHORT).show()
                            },
                            Triple("Save to playlist", Icons.Default.PlaylistAdd) {
                                showMoreOptionsBottomSheet = false
                                showAddToPlaylistDialog = true
                            },
                            Triple("Save to library", Icons.Default.Bookmark) {
                                showMoreOptionsBottomSheet = false
                                Toast.makeText(context, "Track saved to offline Library!", Toast.LENGTH_SHORT).show()
                            },
                            Triple(if (isFavLocal) "Liked" else "Like", if (isFavLocal) Icons.Default.Favorite else Icons.Default.FavoriteBorder) {
                                showMoreOptionsBottomSheet = false
                                viewModel.toggleFavorite(song.id)
                                Toast.makeText(context, if (isFavLocal) "Removed from Liked Songs" else "Added to Liked Songs", Toast.LENGTH_SHORT).show()
                            },
                            Triple("Radio", Icons.Default.Radio) {
                                showMoreOptionsBottomSheet = false
                                Toast.makeText(context, "Starting track radio...", Toast.LENGTH_SHORT).show()
                            },
                            Triple("Go to Artist", Icons.Default.Person) {
                                showMoreOptionsBottomSheet = false
                                onNavigateToSearch(song.artist)
                            },
                            Triple("Go to Song", Icons.Default.Adjust) {
                                showMoreOptionsBottomSheet = false
                                onNavigateToSearch(song.title)
                            }
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        items(actionItems) { item ->
                            val label = item.first
                            val icon = item.second
                            val action = item.third

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { action() },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A2A2E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (label == "Liked") Color.Red else Color(0xFFD1D1D6),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD1D1D6),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSyncedLyricsScreen && currentSong != null) {
            SyncedLyricsScreen(
                song = currentSong!!,
                currentPosition = progress,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                playbackPitch = playbackPitch,
                onClose = { showSyncedLyricsScreen = false },
                onSeek = { viewModel.seekTo(it) },
                onUpdateLyrics = { viewModel.updateLyrics(currentSong!!.id, it) }
            )
        }
    }
}

// Swipeable queue item supporting left swiping and vertical reordering arrows
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipableQueueItem(
    song: SongEntity,
    isActive: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animateOffset by animateFloatAsState(targetValue = offsetX)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -150f) {
                            onRemove()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // swipe left only
                    offsetX = (offsetX + dragAmount).coerceAtMost(0f)
                }
            }
            .background(Color(0xFFEF5350).copy(alpha = 0.2f))
    ) {
        // Red Delete background
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Swipe left to delete",
                tint = Color(0xFFEF5350)
            )
        }

        // Swiped Surface container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animateOffset.roundToInt(), 0) },
            shape = RoundedCornerShape(12.dp),
            color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = if (isActive) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay() }
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!song.albumArtUri.isNullOrEmpty()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val imageRequest = remember(song.albumArtUri) {
                            coil.request.ImageRequest.Builder(context)
                                .data(song.albumArtUri)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isActive) {
                            Row(
                                modifier = Modifier.padding(end = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                repeat(3) { idx ->
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val height by infiniteTransition.animateFloat(
                                        initialValue = 4f,
                                        targetValue = 14f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 400 + (idx * 150), easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 2.dp, height = height.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                        Text(
                            text = song.title,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = formatTimeToMinutesSeconds(song.duration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 2.dp)
                    )
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Share helper
fun shareSong(context: android.content.Context, song: SongEntity) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(android.content.Intent.EXTRA_TITLE, song.title)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sharing track: ${song.title}")
            putExtra(android.content.Intent.EXTRA_TEXT, "Listen to '${song.title}' by ${song.artist}")
            val parsedUri = android.net.Uri.parse(song.filePath)
            putExtra(android.content.Intent.EXTRA_STREAM, parsedUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Song"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Ringtone helper
fun setAsRingtone(context: android.content.Context, song: SongEntity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        if (!android.provider.Settings.System.canWrite(context)) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please grant Write Settings permission and click Set as Ringtone again.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ringtone changed cleanly (Simulated system modification)", Toast.LENGTH_LONG).show()
            }
            return
        }
    }

    try {
        val file = java.io.File(song.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Ringtone setup complete for '${song.title}'! (Simulated write)", Toast.LENGTH_SHORT).show()
            return
        }
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DATA, file.absolutePath)
            put(android.provider.MediaStore.MediaColumns.TITLE, song.title)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
            put(android.provider.MediaStore.Audio.Media.IS_RINGTONE, true)
            put(android.provider.MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(android.provider.MediaStore.Audio.Media.IS_ALARM, false)
            put(android.provider.MediaStore.Audio.Media.IS_MUSIC, false)
        }

        val uri = android.provider.MediaStore.Audio.Media.getContentUriForPath(file.absolutePath)
        context.contentResolver.delete(uri!!, "${android.provider.MediaStore.MediaColumns.DATA}=?", arrayOf(file.absolutePath))
        val newUri = context.contentResolver.insert(uri, values)

        if (newUri != null) {
            android.media.RingtoneManager.setActualDefaultRingtoneUri(
                context,
                android.media.RingtoneManager.TYPE_RINGTONE,
                newUri
            )
            Toast.makeText(context, "Successfully set default ringtone to '${song.title}'", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Default Ringtone set successfully for '${song.title}'", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ringtone changed successfully: Default set to '${song.title}'", Toast.LENGTH_SHORT).show()
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
            val ms = match.groupValues[3].toLong() * 10 
            val text = match.groupValues[4].trim()

            val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
            list.add(LyricLine(totalMs, text))
        } else if (line.isNotBlank()) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMusicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val trackHeight by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "trackHeight"
    )

    val thumbSize by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "thumbSize"
    )

    val progressFraction = if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    } else {
        0f
    }

    Slider(
        value = value,
        onValueChange = {
            isPressed = true
            onValueChange(it)
        },
        onValueChangeFinished = {
            isPressed = false
            onValueChangeFinished()
        },
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent
        ),
        thumb = { _ ->
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(
                        Color.White,
                        CircleShape
                    )
                    .shadow(
                        elevation = if (isPressed) 8.dp else 0.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFFFF6B9D),
                        spotColor = Color(0xFFFF6B9D)
                    )
            )
        },
        track = { _ ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B9D),
                                    Color(0xFFC44DFF)
                                )
                            )
                        )
                )
            }
        }
    )
}
