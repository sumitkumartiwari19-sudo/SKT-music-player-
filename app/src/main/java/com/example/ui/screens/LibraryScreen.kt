package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.SongEntity
import com.example.viewmodel.LayoutType
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.SortType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    isPlayerOpen: Boolean = false,
    initialPlaylistId: Long? = null,
    onClearInitialPlaylistId: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val songs by viewModel.allSongs.collectAsState()
    val albums by viewModel.allAlbums.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val sortOption by viewModel.sortingOption.collectAsState()
    val layoutType by viewModel.libraryLayout.collectAsState()

    var activeCategoryIndex by remember { mutableStateOf(0) } // 0: Songs, 1: Albums, 2: Artists, 3: Folders, 4: Playlists, 5: Most Played
    val categories = listOf("Songs", "Albums", "Artists", "Folders", "Playlists", "Most Played")

    // Filter selectors for detail view overlays
    var selectedAlbum by remember { mutableStateOf<AlbumEntity?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }

    LaunchedEffect(initialPlaylistId, playlists) {
        if (initialPlaylistId != null && playlists.isNotEmpty()) {
            val found = playlists.find { it.playlistId == initialPlaylistId }
            if (found != null) {
                activeCategoryIndex = 4
                selectedPlaylist = found
            }
            onClearInitialPlaylistId()
        }
    }

    // Dropdown Dialog state
    var showSortMenu by remember { mutableStateOf(false) }
    var itemToEditTag by remember { mutableStateOf<SongEntity?>(null) }
    var playlistToAddTo by remember { mutableStateOf<SongEntity?>(null) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }

    // Multi-select status
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedIds by viewModel.selectedSongIds.collectAsState()

    // Handle system back press within the Library Screen to drop any detailed category views but only when player is not open
    val isAnyDetailSelected = (selectedAlbum != null || selectedArtist != null || selectedFolder != null || selectedPlaylist != null) && !isPlayerOpen
    BackHandler(enabled = isAnyDetailSelected) {
        selectedAlbum = null
        selectedArtist = null
        selectedFolder = null
        selectedPlaylist = null
    }

    // Sort Logic
    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortType.NAME -> songs.sortedBy { it.title.lowercase() }
            SortType.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SortType.DURATION -> songs.sortedBy { it.duration }
            SortType.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortType.ALBUM -> songs.sortedBy { it.album.lowercase() }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                if (isAnyDetailSelected) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when {
                                    selectedAlbum != null -> selectedAlbum?.albumName ?: "Album Details"
                                    selectedArtist != null -> selectedArtist?.artistName ?: "Artist Details"
                                    selectedFolder != null -> selectedFolder?.substringAfterLast("/") ?: "Folder Details"
                                    selectedPlaylist != null -> selectedPlaylist?.playlistName ?: "Playlist Details"
                                    else -> "Details"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                selectedAlbum = null
                                selectedArtist = null
                                selectedFolder = null
                                selectedPlaylist = null
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isAnyDetailSelected) innerPadding.calculateTopPadding() else 0.dp)
            ) {
                // Dropdown Sort Menu
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = { viewModel.sortingOption.value = SortType.NAME; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added") },
                        onClick = { viewModel.sortingOption.value = SortType.DATE_ADDED; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Duration") },
                        onClick = { viewModel.sortingOption.value = SortType.DURATION; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Artist") },
                        onClick = { viewModel.sortingOption.value = SortType.ARTIST; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Album") },
                        onClick = { viewModel.sortingOption.value = SortType.ALBUM; showSortMenu = false }
                    )
                }

                when (activeCategoryIndex) {
                    0 -> { // Songs Tab
                        val listState = rememberLazyListState()
                        if (layoutType == LayoutType.LIST) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        LibraryHeaderWithTabs(
                                            title = "My Library",
                                            activeCategoryIndex = activeCategoryIndex,
                                            categories = categories,
                                            onCategorySelect = { index ->
                                                activeCategoryIndex = index
                                                selectedAlbum = null
                                                selectedArtist = null
                                                selectedFolder = null
                                                selectedPlaylist = null
                                            },
                                            onSortClick = { showSortMenu = true },
                                            onLayoutToggleClick = {
                                                viewModel.libraryLayout.value =
                                                    if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                            },
                                            isListLayout = layoutType == LayoutType.LIST,
                                            onRefreshClick = { viewModel.scanLocalFiles() },
                                            isRefreshing = isScanning
                                        )
                                    }

                                    if (isMultiSelect) {
                                        item {
                                            MultiSelectBar(viewModel, selectedIds, onAddPlaylist = { showAddPlaylistDialog = true })
                                        }
                                    }

                                    stickyHeader {
                                        StickyPlayAndLocateHeader(
                                            songsCount = sortedSongs.size,
                                            onShufflePlay = {
                                                if (sortedSongs.isNotEmpty()) {
                                                    viewModel.toggleShuffle()
                                                    val shuffled = sortedSongs.shuffled()
                                                    viewModel.playSong(shuffled[0], shuffled)
                                                }
                                            },
                                            onLocateCurrent = {
                                                val currentSongVal = viewModel.currentSong.value
                                                if (currentSongVal != null) {
                                                    val index = sortedSongs.indexOfFirst { it.id == currentSongVal.id }
                                                    if (index >= 0) {
                                                        scope.launch {
                                                            // Offset calculation: header is 0, optional multiselect is 1, sticky play is 2
                                                            val scrollTarget = index + if (isMultiSelect) 3 else 2
                                                            // Convert dp to pixels to account for the sticky header (56.dp) plus padding (16.dp)
                                                            val offset = with(density) { (56.dp + 16.dp).toPx().toInt() }
                                                            listState.animateScrollToItem(
                                                                index = scrollTarget,
                                                                scrollOffset = -offset
                                                            )
                                                            Toast.makeText(context, "Locating: ${currentSongVal.title}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Current song not in list", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "No tracks are currently playing", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }

                                    items(sortedSongs, key = { it.id }) { song ->
                                        val isSelected = selectedIds.contains(song.id)
                                        val isCurrentlyPlaying = currentSong?.id == song.id
                                        val isPlaying = isPlaying
                                        SongItemRow(
                                            song = song,
                                            isSelected = isSelected,
                                            isCurrentlyPlaying = isCurrentlyPlaying,
                                            isPlaying = isPlaying,
                                            isMultiSelectMode = isMultiSelect,
                                            isFavorite = favorites.any { it.id == song.id },
                                            onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                            onSelectToggle = { viewModel.toggleSongSelected(song.id) },
                                            onClick = {
                                                if (isMultiSelect) {
                                                    viewModel.toggleSongSelected(song.id)
                                                } else {
                                                    viewModel.playSong(song, sortedSongs)
                                                }
                                            },
                                            onMoreClick = {
                                                playlistToAddTo = song
                                            }
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(180.dp))
                                    }
                                }

                                // Custom Scrollbar Slider / Fast Scroller Overlay
                                if (sortedSongs.size > 8) {
                                    val density = LocalDensity.current
                                    var containerHeight by remember { mutableStateOf(1f) }
                                    var dragY by remember { mutableStateOf(0f) }
                                    var isDragging by remember { mutableStateOf(false) }
                                    var currentScrollLetter by remember { mutableStateOf("") }

                                    // Auto fade control states
                                    var isScrollbarVisible by remember { mutableStateOf(false) }

                                    val isScrollInProgress = listState.isScrollInProgress
                                    val firstVisibleIndex = listState.firstVisibleItemIndex
                                    val firstVisibleOffset = listState.firstVisibleItemScrollOffset

                                    LaunchedEffect(isScrollInProgress, firstVisibleIndex, firstVisibleOffset, isDragging) {
                                        if (isScrollInProgress || isDragging) {
                                            isScrollbarVisible = true
                                        } else {
                                            delay(1200)
                                            isScrollbarVisible = false
                                        }
                                    }

                                    val scrollbarAlpha by animateFloatAsState(
                                        targetValue = if (isScrollbarVisible) 1f else 0f,
                                        animationSpec = tween(durationMillis = if (isScrollbarVisible) 150 else 600),
                                        label = "scrollbarAlpha"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(28.dp)
                                            .padding(vertical = 120.dp) // Leave safety padding for the bottom bar & top tab row
                                            .graphicsLayer { alpha = scrollbarAlpha }
                                            .onGloballyPositioned { coordinates ->
                                                containerHeight = coordinates.size.height.toFloat()
                                            }
                                            .pointerInput(containerHeight, sortedSongs) {
                                                detectVerticalDragGestures(
                                                    onDragStart = { offset ->
                                                        isDragging = true
                                                        dragY = offset.y.coerceIn(0f, containerHeight)
                                                        val fraction = dragY / containerHeight
                                                        val targetIndex = (fraction * sortedSongs.size).toInt().coerceIn(0, sortedSongs.lastIndex)
                                                        currentScrollLetter = sortedSongs[targetIndex].title.firstOrNull()?.toString()?.uppercase() ?: ""
                                                        scope.launch {
                                                            listState.scrollToItem(targetIndex + if (isMultiSelect) 3 else 2)
                                                        }
                                                    },
                                                    onDragEnd = { isDragging = false },
                                                    onDragCancel = { isDragging = false }
                                                ) { change, dragAmount ->
                                                    change.consume()
                                                    dragY = (dragY + dragAmount).coerceIn(0f, containerHeight)
                                                    val fraction = dragY / containerHeight
                                                    val targetIndex = (fraction * sortedSongs.size).toInt().coerceIn(0, sortedSongs.lastIndex)
                                                    currentScrollLetter = sortedSongs[targetIndex].title.firstOrNull()?.toString()?.uppercase() ?: ""
                                                    scope.launch {
                                                        listState.scrollToItem(targetIndex + if (isMultiSelect) 3 else 2)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Vertical Track
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                        )

                                        // Movable Thumb
                                        val thumbProgress = remember {
                                            if (isDragging) {
                                                dragY / containerHeight
                                            } else {
                                                val visibleIndex = 0
                                                val totalCount = sortedSongs.size
                                                if (totalCount > 0) {
                                                    val offsetIndex = (visibleIndex - (if (isMultiSelect) 3 else 2)).coerceAtLeast(0)
                                                    offsetIndex.toFloat() / totalCount.toFloat()
                                                } else {
                                                    0f
                                                }
                                            }
                                        }

                                        val thumbHeightPx = with(density) { 16.dp.toPx() }
                                         val maxThumbTravelPx = (containerHeight - thumbHeightPx).coerceAtLeast(1f)

                                         val thumbY by remember(sortedSongs, isDragging, containerHeight, maxThumbTravelPx) {
                                             derivedStateOf {
                                                 val progressFraction = if (isDragging) {
                                                     dragY / containerHeight
                                                 } else {
                                                     val visibleItemIdx = listState.firstVisibleItemIndex
                                                     val totalCount = sortedSongs.size
                                                     if (totalCount > 0) {
                                                         val offsetIndex = (visibleItemIdx - (if (isMultiSelect) 3 else 2)).coerceAtLeast(0)
                                                         offsetIndex.toFloat() / totalCount.toFloat()
                                                     } else {
                                                         0f
                                                     }
                                                 }
                                                 (progressFraction * maxThumbTravelPx).coerceIn(0f, maxThumbTravelPx)
                                             }
                                         }
                                        

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .graphicsLayer { translationY = thumbY }
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }

                                    // Floating letter preview bubble
                                    if (isDragging && currentScrollLetter.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 40.dp)
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(28.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .shadow(6.dp, RoundedCornerShape(28.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currentScrollLetter,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 24.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LibraryHeaderWithTabs(
                                    title = "My Library",
                                    activeCategoryIndex = activeCategoryIndex,
                                    categories = categories,
                                    onCategorySelect = { index ->
                                        activeCategoryIndex = index
                                        selectedAlbum = null
                                        selectedArtist = null
                                        selectedFolder = null
                                        selectedPlaylist = null
                                    },
                                    onSortClick = { showSortMenu = true },
                                    onLayoutToggleClick = {
                                        viewModel.libraryLayout.value =
                                            if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                    },
                                    isListLayout = layoutType == LayoutType.LIST,
                                    onRefreshClick = { viewModel.scanLocalFiles() },
                                    isRefreshing = isScanning
                                )

                                StickyPlayAndLocateHeader(
                                    songsCount = sortedSongs.size,
                                    onShufflePlay = {
                                        if (sortedSongs.isNotEmpty()) {
                                            viewModel.toggleShuffle()
                                            val shuffled = sortedSongs.shuffled()
                                            viewModel.playSong(shuffled[0], shuffled)
                                        }
                                    },
                                    onLocateCurrent = {
                                        Toast.makeText(context, "Switch to List layout to find position", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 130.dp),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 180.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(sortedSongs, key = { it.id }) { song ->
                                        SongGridCard(
                                            song = song,
                                            onClick = { viewModel.playSong(song, sortedSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Albums Tab
                        if (selectedAlbum == null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LibraryHeaderWithTabs(
                                    title = "My Library",
                                    activeCategoryIndex = activeCategoryIndex,
                                    categories = categories,
                                    onCategorySelect = { index ->
                                        activeCategoryIndex = index
                                        selectedAlbum = null
                                        selectedArtist = null
                                        selectedFolder = null
                                        selectedPlaylist = null
                                    },
                                    onSortClick = { showSortMenu = true },
                                    onLayoutToggleClick = {
                                        viewModel.libraryLayout.value =
                                            if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                    },
                                    isListLayout = layoutType == LayoutType.LIST
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(albums, key = { it.id }) { album ->
                                        Card(
                                            onClick = { selectedAlbum = album },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                                ) {
                                                    if (!album.albumArtUri.isNullOrEmpty()) {
                                                        AsyncImage(
                                                            model = let {
                                                                val context = androidx.compose.ui.platform.LocalContext.current
                                                                remember(album.albumArtUri) {
                                                                    coil.request.ImageRequest.Builder(context)
                                                                        .data(album.albumArtUri)
                                                                        .crossfade(true)
                                                                        .build()
                                                                }
                                                            },
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
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(album.albumName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(album.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val albumSongs = songs.filter { it.album.equals(selectedAlbum?.albumName, true) }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedAlbum = null }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Album: ${selectedAlbum?.albumName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 180.dp)
                                ) {
                                    items(albumSongs, key = { it.id }) { song ->
                                        val isCurrentlyPlaying = currentSong?.id == song.id
                                        val isPlaying = isPlaying
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isCurrentlyPlaying = isCurrentlyPlaying,
                                            isPlaying = isPlaying,
                                            isMultiSelectMode = false,
                                            onSelectToggle = {},
                                            isFavorite = favorites.any { it.id == song.id },
                                            onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                            onClick = { viewModel.playSong(song, albumSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // Artists Tab
                        if (selectedArtist == null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LibraryHeaderWithTabs(
                                    title = "My Library",
                                    activeCategoryIndex = activeCategoryIndex,
                                    categories = categories,
                                    onCategorySelect = { index ->
                                        activeCategoryIndex = index
                                        selectedAlbum = null
                                        selectedArtist = null
                                        selectedFolder = null
                                        selectedPlaylist = null
                                    },
                                    onSortClick = { showSortMenu = true },
                                    onLayoutToggleClick = {
                                        viewModel.libraryLayout.value =
                                            if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                    },
                                    isListLayout = layoutType == LayoutType.LIST
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(artists, key = { it.id }) { artist ->
                                        Card(
                                            onClick = { selectedArtist = artist },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(40.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(artist.artistName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${artist.songCount} songs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val artistSongs = songs.filter { it.artist.equals(selectedArtist?.artistName, true) }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedArtist = null }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Artist: ${selectedArtist?.artistName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 180.dp)
                                ) {
                                    items(artistSongs, key = { it.id }) { song ->
                                        val isCurrentlyPlaying = currentSong?.id == song.id
                                        val isPlaying = isPlaying
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isCurrentlyPlaying = isCurrentlyPlaying,
                                            isPlaying = isPlaying,
                                            isMultiSelectMode = false,
                                            onSelectToggle = {},
                                            isFavorite = favorites.any { it.id == song.id },
                                            onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                            onClick = { viewModel.playSong(song, artistSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> { // Folders Tab
                        if (selectedFolder == null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LibraryHeaderWithTabs(
                                    title = "My Library",
                                    activeCategoryIndex = activeCategoryIndex,
                                    categories = categories,
                                    onCategorySelect = { index ->
                                        activeCategoryIndex = index
                                        selectedAlbum = null
                                        selectedArtist = null
                                        selectedFolder = null
                                        selectedPlaylist = null
                                    },
                                    onSortClick = { showSortMenu = true },
                                    onLayoutToggleClick = {
                                        viewModel.libraryLayout.value =
                                            if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                    },
                                    isListLayout = layoutType == LayoutType.LIST
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 180.dp)
                                ) {
                                    items(folders, key = { it }) { path ->
                                        val simplePathName = path.substringAfterLast("/")
                                        Card(
                                            onClick = { selectedFolder = path },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(simplePathName, fontWeight = FontWeight.Bold)
                                                    Text(path, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val folderSongs = songs.filter { it.filePath.startsWith(selectedFolder ?: "") }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedFolder = null }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        selectedFolder?.substringAfterLast("/") ?: "Folder",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 180.dp)
                                ) {
                                    items(folderSongs, key = { it.id }) { song ->
                                        val isCurrentlyPlaying = currentSong?.id == song.id
                                        val isPlaying = isPlaying
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isCurrentlyPlaying = isCurrentlyPlaying,
                                            isPlaying = isPlaying,
                                            isMultiSelectMode = false,
                                            onSelectToggle = {},
                                            isFavorite = favorites.any { it.id == song.id },
                                            onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                            onClick = { viewModel.playSong(song, folderSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> { // Playlists Tab
                        if (selectedPlaylist == null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LibraryHeaderWithTabs(
                                    title = "My Library",
                                    activeCategoryIndex = activeCategoryIndex,
                                    categories = categories,
                                    onCategorySelect = { index ->
                                        activeCategoryIndex = index
                                        selectedAlbum = null
                                        selectedArtist = null
                                        selectedFolder = null
                                        selectedPlaylist = null
                                    },
                                    onSortClick = { showSortMenu = true },
                                    onLayoutToggleClick = {
                                        viewModel.libraryLayout.value =
                                            if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                    },
                                    isListLayout = layoutType == LayoutType.LIST
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 180.dp)
                                ) {
                                    items(playlists, key = { it.playlistId }) { playlist ->
                                        Card(
                                            onClick = { selectedPlaylist = playlist },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(playlist.playlistName, fontWeight = FontWeight.Bold)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            val idx = playlists.indexOf(playlist)
                                                            if (idx > 0) {
                                                                val newList = playlists.toMutableList()
                                                                newList.removeAt(idx)
                                                                newList.add(idx - 1, playlist)
                                                                viewModel.reorderPlaylists(newList.map { it.playlistId })
                                                            }
                                                        },
                                                        enabled = playlists.indexOf(playlist) > 0
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val idx = playlists.indexOf(playlist)
                                                            if (idx < playlists.size - 1) {
                                                                val newList = playlists.toMutableList()
                                                                newList.removeAt(idx)
                                                                newList.add(idx + 1, playlist)
                                                                viewModel.reorderPlaylists(newList.map { it.playlistId })
                                                            }
                                                        },
                                                        enabled = playlists.indexOf(playlist) < playlists.size - 1
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(onClick = { playlistToRename = playlist }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Rename Playlist", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(onClick = { viewModel.deletePlaylist(playlist.playlistId) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val listId = selectedPlaylist?.playlistId ?: 0L
                            val playlistSongsState = remember(listId) { viewModel.getSongsInPlaylist(listId) }.collectAsState(emptyList())
                            val playlistSongs = playlistSongsState.value

                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { selectedPlaylist = null }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            selectedPlaylist?.playlistName ?: "Playlist Details",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }

                                    OutlinedButton(onClick = {
                                        // Auto-add all songs in DB to this playlist as helper
                                        sortedSongs.take(5).forEach { song ->
                                            viewModel.addSongToPlaylist(listId, song.id)
                                        }
                                    }) {
                                        Text("Auto-Add Songs")
                                    }
                                }

                                if (playlistSongs.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("No songs added yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Add audio tracks to start listening offline.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    selectedPlaylist = null
                                                    activeCategoryIndex = 0 // Navigate back to Songs tab
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Songs", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 180.dp)
                                    ) {
                                        items(playlistSongs, key = { it.id }) { song ->
                                            val isCurrentlyPlaying = currentSong?.id == song.id
                                            val isPlaying = isPlaying
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            val idx = playlistSongs.indexOf(song)
                                                            if (idx > 0) {
                                                                val newList = playlistSongs.toMutableList()
                                                                newList.removeAt(idx)
                                                                newList.add(idx - 1, song)
                                                                viewModel.reorderSongsInPlaylist(listId, newList.map { it.id })
                                                            }
                                                        },
                                                        enabled = playlistSongs.indexOf(song) > 0,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Song Up", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val idx = playlistSongs.indexOf(song)
                                                            if (idx < playlistSongs.size - 1) {
                                                                val newList = playlistSongs.toMutableList()
                                                                newList.removeAt(idx)
                                                                newList.add(idx + 1, song)
                                                                viewModel.reorderSongsInPlaylist(listId, newList.map { it.id })
                                                            }
                                                        },
                                                        enabled = playlistSongs.indexOf(song) < playlistSongs.size - 1,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Song Down", modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Box(modifier = Modifier.weight(1f)) {
                                                    SongItemRow(
                                                        song = song,
                                                        isSelected = false,
                                                        isCurrentlyPlaying = isCurrentlyPlaying,
                                                        isPlaying = isPlaying,
                                                        isMultiSelectMode = false,
                                                        onSelectToggle = {},
                                                        isFavorite = favorites.any { it.id == song.id },
                                                        onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                                        onClick = { viewModel.playSong(song, playlistSongs) },
                                                        onMoreClick = {
                                                            scope.launch {
                                                                viewModel.removeSongFromPlaylist(listId, song.id)
                                                                Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    5 -> { // Most Played Tab
                        Column(modifier = Modifier.fillMaxSize()) {
                            LibraryHeaderWithTabs(
                                title = "My Library",
                                activeCategoryIndex = activeCategoryIndex,
                                categories = categories,
                                onCategorySelect = { index ->
                                    activeCategoryIndex = index
                                    selectedAlbum = null
                                    selectedArtist = null
                                    selectedFolder = null
                                    selectedPlaylist = null
                                },
                                onSortClick = { showSortMenu = true },
                                onLayoutToggleClick = {
                                    viewModel.libraryLayout.value =
                                        if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                                },
                                isListLayout = layoutType == LayoutType.LIST
                            )

                            if (mostPlayedSongs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No songs played yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Play counts will update as you listen to music.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 180.dp)
                                ) {
                                    items(mostPlayedSongs, key = { it.id }) { song ->
                                        val isCurrentlyPlaying = currentSong?.id == song.id
                                        val isPlaying = isPlaying
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isCurrentlyPlaying = isCurrentlyPlaying,
                                            isPlaying = isPlaying,
                                            isMultiSelectMode = false,
                                            isFavorite = favorites.any { it.id == song.id },
                                            onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                                            onClick = { viewModel.playSong(song, mostPlayedSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add to Playlist picker dialogue
        if (playlistToAddTo != null) {
            val pendingSong = playlistToAddTo!!
            AlertDialog(
                onDismissRequest = { playlistToAddTo = null },
                title = { Text("Options for: ${pendingSong.title}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.toggleFavorite(pendingSong.id)
                                playlistToAddTo = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Toggle Favorite", modifier = Modifier.fillMaxWidth())
                        }

                        TextButton(
                            onClick = {
                                itemToEditTag = pendingSong
                                playlistToAddTo = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Metadata tags (Tag Editor)", modifier = Modifier.fillMaxWidth())
                        }

                        Divider()

                        Text("Add to Playlist:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                        if (playlists.isEmpty()) {
                            Text("No playlists created yet. Create one on the Home tab.", fontSize = 11.sp)
                        } else {
                            playlists.forEach { playlist ->
                                TextButton(
                                    onClick = {
                                        viewModel.addSongToPlaylist(playlist.playlistId, pendingSong.id)
                                        playlistToAddTo = null
                                        Toast.makeText(context, "Added to playlist!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(playlist.playlistName)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { playlistToAddTo = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // Tag Editor Dialog
        if (itemToEditTag != null) {
            val song = itemToEditTag!!
            var editTitle by remember { mutableStateOf(song.title) }
            var editArtist by remember { mutableStateOf(song.artist) }
            var editAlbum by remember { mutableStateOf(song.album) }
            var editYear by remember { mutableStateOf("") }
            var editGenre by remember { mutableStateOf("") }

            LaunchedEffect(song.id) {
                val metadata = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.util.TagEditorHelper.readTagsFromFile(song.filePath)
                }
                if (metadata != null) {
                    editTitle = metadata.title.ifEmpty { song.title }
                    editArtist = metadata.artist.ifEmpty { song.artist }
                    editAlbum = metadata.album.ifEmpty { song.album }
                    editYear = metadata.year
                    editGenre = metadata.genre
                }
            }

            AlertDialog(
                onDismissRequest = { itemToEditTag = null },
                title = { Text("Offline Tag Editor") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Song Title") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editArtist,
                            onValueChange = { editArtist = it },
                            label = { Text("Artist Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editAlbum,
                            onValueChange = { editAlbum = it },
                            label = { Text("Album Title") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editYear,
                            onValueChange = { editYear = it },
                            label = { Text("Year") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editGenre,
                            onValueChange = { editGenre = it },
                            label = { Text("Genre") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.editSongTags(song.id, editTitle, editArtist, editAlbum, editYear, editGenre)
                        itemToEditTag = null
                        Toast.makeText(context, "Tags successfully saved!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToEditTag = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Multi-select targets playlist Dialog
        if (showAddPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showAddPlaylistDialog = false },
                title = { Text("Add Selected to Playlist") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    viewModel.addSelectedSongsToPlaylist(playlist.playlistId)
                                    showAddPlaylistDialog = false
                                    Toast.makeText(context, "Added Selected Songs!", Toast.LENGTH_SHORT).show()
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
                },
                confirmButton = {
                    TextButton(onClick = { showAddPlaylistDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Playlist Rename Dialog
        if (playlistToRename != null) {
            val playlist = playlistToRename!!
            var renameName by remember(playlist) { mutableStateOf(playlist.playlistName) }
            AlertDialog(
                onDismissRequest = { playlistToRename = null },
                title = { Text("Rename Playlist") },
                text = {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameName.isNotBlank()) {
                            viewModel.renamePlaylist(playlist.playlistId, renameName.trim())
                            playlistToRename = null
                        }
                    }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LibraryHeaderWithTabs(
    title: String,
    activeCategoryIndex: Int,
    categories: List<String>,
    onCategorySelect: (Int) -> Unit,
    onSortClick: () -> Unit,
    onLayoutToggleClick: () -> Unit,
    isListLayout: Boolean,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRefreshClick != null) {
                    val infiniteTransition = rememberInfiniteTransition(label = "Spin")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Rotation"
                    )

                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_songs_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Song List",
                            modifier = if (isRefreshing) Modifier.rotate(angle) else Modifier
                        )
                    }
                }

                IconButton(onClick = onSortClick) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                }
                IconButton(onClick = onLayoutToggleClick) {
                    Icon(
                        imageVector = if (isListLayout) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Toggle Grid Layout"
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = activeCategoryIndex,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeCategoryIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, name ->
                Tab(
                    selected = activeCategoryIndex == index,
                    onClick = { onCategorySelect(index) },
                    text = {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = if (activeCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun StickyPlayAndLocateHeader(
    songsCount: Int,
    onShufflePlay: () -> Unit,
    onLocateCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShufflePlay,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("shuffle_all_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Shuffle Play", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Locate Current Song crosshair button with standard 18dp radius
                IconButton(
                    onClick = onLocateCurrent,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Locate Current Song",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "$songsCount Tracks",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MultiSelectBar(
    viewModel: MusicViewModel,
    selectedIds: Set<String>,
    onAddPlaylist: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${selectedIds.size} songs selected",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { viewModel.addSelectedSongsToQueue() }) {
                    Icon(Icons.Default.Queue, contentDescription = "Add selected to Queue")
                }
                IconButton(onClick = { onAddPlaylist() }) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add selected to Playlist")
                }
                IconButton(onClick = { viewModel.clearMultiSelect() }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
    }
}

@Composable
fun AnimatedEqualizerBars(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val height1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, delayMillis = 50),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier.size(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(height1)
                .background(barColor, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(height2)
                .background(barColor, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(height3)
                .background(barColor, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItemRow(
    song: SongEntity,
    isSelected: Boolean,
    isCurrentlyPlaying: Boolean = false,
    isPlaying: Boolean = false,
    isMultiSelectMode: Boolean = false,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onSelectToggle: () -> Unit = {},
    onClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isCurrentlyPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelectToggle
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multi-select Indicator checkbox
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentlyPlaying && isPlaying) {
                    AnimatedEqualizerBars(modifier = Modifier.align(Alignment.Center))
                } else if (!song.albumArtUri.isNullOrEmpty()) {
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
                        tint = if (isCurrentlyPlaying) highlightColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentlyPlaying) highlightColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = if (isCurrentlyPlaying) highlightColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration text helper
            val minutes = song.duration / 1000 / 60
            val seconds = (song.duration / 1000) % 60
            Text(
                text = String.format("%d:%02d", minutes, seconds),
                fontSize = 12.sp,
                color = if (isCurrentlyPlaying) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (onFavoriteToggle != null) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Track options menu",
                    tint = if (isCurrentlyPlaying) highlightColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SongGridCard(
    song: SongEntity,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    Text(song.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onMoreClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
