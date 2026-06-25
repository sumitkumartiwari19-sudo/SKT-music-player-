package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.SongEntity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val focusSearchKeyboard by viewModel.focusSearchKeyboard.collectAsState()
    val songs by viewModel.allSongs.collectAsState()
    val albums by viewModel.allAlbums.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()

    // Filtered lists
    val filteredSongs = remember(query, songs) {
        if (query.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    val filteredAlbums = remember(query, albums) {
        if (query.isBlank()) emptyList()
        else albums.filter {
            it.albumName.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
    }

    val filteredArtists = remember(query, artists) {
        if (query.isBlank()) emptyList()
        else artists.filter {
            it.artistName.contains(query, ignoreCase = true)
        }
    }

    val filteredPlaylists = remember(query, playlists) {
        if (query.isBlank()) emptyList()
        else playlists.filter {
            it.playlistName.contains(query, ignoreCase = true)
        }
    }

    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Songs, 2: Albums, 3: Artists
    val tabs = listOf("All", "Songs", "Albums", "Artists")

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        ),
        modifier = modifier.fillMaxSize()
    ) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(focusSearchKeyboard) {
            if (focusSearchKeyboard) {
                focusRequester.requestFocus()
                viewModel.focusSearchKeyboard.value = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        // Search Header
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Text Search input
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Songs, artists, or albums...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag("search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = selectedFilterTab,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedFilterTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilterTab == index,
                        onClick = { selectedFilterTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedFilterTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Results lazy list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RENDER ARTISTS
                if (selectedFilterTab == 0 || selectedFilterTab == 3) {
                    if (filteredArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Artists",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(filteredArtists, key = { "art_${it.id}" }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                valign = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(25.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(artist.artistName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("${artist.songCount} songs • ${artist.albumCount} albums", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // RENDER ALBUMS
                if (selectedFilterTab == 0 || selectedFilterTab == 2) {
                    if (filteredAlbums.isNotEmpty()) {
                        item {
                            Text(
                                "Albums",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(filteredAlbums, key = { "alb_${it.id}" }) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                valign = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    if (!album.albumArtUri.isNullOrEmpty()) {
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        val imageRequest = remember(album.albumArtUri) {
                                            coil.request.ImageRequest.Builder(context)
                                                .data(album.albumArtUri)
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
                                            Icons.Default.Album,
                                            contentDescription = null,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(album.albumName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(album.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // RENDER SONGS
                if (selectedFilterTab == 0 || selectedFilterTab == 1) {
                    if (filteredSongs.isNotEmpty()) {
                        item {
                            Text(
                                "Songs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(filteredSongs, key = { "song_${it.id}" }) { song ->
                            SearchSongRow(song = song, onClick = {
                                viewModel.playSong(song, filteredSongs)
                            })
                        }
                    }
                }

                if (filteredSongs.isEmpty() && filteredAlbums.isEmpty() && filteredArtists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No matching results",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Suggest search context
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Find Your Favorite Tunes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Search offline folders, local playlists, artists and track names instantly with zero lag.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
}

@Composable
fun SearchSongRow(
    song: SongEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Utility extension for helper Row padding
@Composable
private fun Row(modifier: Modifier, valign: Alignment.Vertical, content: @Composable RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = Arrangement.Start, verticalAlignment = valign, content = content)
}
