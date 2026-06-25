package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.entity.SongEntity
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncedLyricsScreen(
    song: SongEntity,
    currentPosition: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    playbackPitch: Float,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit,
    onUpdateLyrics: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lyricText = song.lyricText ?: ""
    val lyricLines = remember(lyricText) { parseLrcLyrics(lyricText) }
    
    // Find active lyric line index
    val activeLineIndex = remember(lyricLines, currentPosition) {
        val parsedLines = lyricLines.filter { it.timestampMs > 0 }
        if (parsedLines.isEmpty()) -1
        else {
            var foundIndex = -1
            for (i in parsedLines.indices) {
                if (currentPosition >= parsedLines[i].timestampMs) {
                    foundIndex = i
                } else {
                    break
                }
            }
            // Map index back to original lyricLines index
            if (foundIndex != -1) {
                val activeParsedLine = parsedLines[foundIndex]
                lyricLines.indexOf(activeParsedLine)
            } else {
                -1
            }
        }
    }

    val listState = rememberLazyListState()
    var showEditorDialog by remember { mutableStateOf(false) }

    // Smoothly scroll to active item
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex != -1 && lyricLines.isNotEmpty()) {
            val targetScrollIndex = (activeLineIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScrollIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Immersive blurred background
        if (!song.albumArtUri.isNullOrBlank()) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.35f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                Color.Black
                            )
                        )
                    )
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Synced Lyrics",
                        tint = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(onClick = { showEditorDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit LRC Lyrics",
                        tint = Color.White
                    )
                }
            }

            // Sync/Playback indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isPlaying) Color.Green else Color.LightGray,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Text(
                            text = if (lyricLines.any { it.timestampMs > 0 }) "Synced LRC Active" else "Static Plain Text",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics List
            if (lyricLines.isEmpty() || lyricText.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Synced LRC Lyrics Found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You can add synced LRC lyrics with timestamps (e.g., [00:12.45] lyric text) by tapping the edit button above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showEditorDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add LRC Lyrics")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    itemsIndexed(lyricLines) { index, line ->
                        val isActive = index == activeLineIndex
                        val textColor = if (isActive) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.4f)
                        }
                        val textScale = if (isActive) 1.15f else 1.0f
                        val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (line.timestampMs > 0) {
                                        onSeek(line.timestampMs)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = (22 * textScale).sp,
                                    lineHeight = (30 * textScale).sp
                                ),
                                fontWeight = fontWeight,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (line.timestampMs > 0) {
                                Text(
                                    text = formatTimeToMinutesSeconds(line.timestampMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive LRC Lyrics Editor Dialog
    if (showEditorDialog) {
        var editedText by remember { mutableStateOf(lyricText) }
        Dialog(
            onDismissRequest = { showEditorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LRC Lyrics Editor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showEditorDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Editor")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paste standard LRC formatted lyrics with timestamps below. Timestamps should be in [mm:ss.xx] format.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            editedText = """
[00:02.00] Welcome to SKT Music Player
[00:06.00] Enjoy beautiful high fidelity sound
[00:10.00] Feel the deep bass boost and perfect sound
[00:15.00] Enjoy the scrolling synchronized LRC lyrics
                            """.trimIndent()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insert Sample LRC Template")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("e.g. [00:04.50] My Song Lyrics Line 1...") },
                        label = { Text("LRC Timed Lyrics") },
                        maxLines = Int.MAX_VALUE
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditorDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onUpdateLyrics(editedText)
                                showEditorDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Lyrics")
                        }
                    }
                }
            }
        }
    }
}
