package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val songs by viewModel.allSongs.collectAsState()
    val albums by viewModel.allAlbums.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val sortOption by viewModel.sortingOption.collectAsState()
    val layoutType by viewModel.libraryLayout.collectAsState()

    var activeCategoryIndex by remember { mutableStateOf(0) } // 0: Songs, 1: Albums, 2: Artists, 3: Folders, 4: Playlists
    val categories = listOf("Songs", "Albums", "Artists", "Folders", "Playlists")

    // Filter selectors for detail view overlays
    var selectedAlbum by remember { mutableStateOf<AlbumEntity?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    // Dropdown Dialog state
    var showSortMenu by remember { mutableStateOf(false) }
    var itemToEditTag by remember { mutableStateOf<SongEntity?>(null) }
    var playlistToAddTo by remember { mutableStateOf<SongEntity?>(null) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }

    // Multi-select status
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedIds by viewModel.selectedSongIds.collectAsState()

    // 1. Sort Logic
    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortType.NAME -> songs.sortedBy { it.title.lowercase() }
            SortType.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SortType.DURATION -> songs.sortedBy { it.duration }
            SortType.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortType.ALBUM -> songs.sortedBy { it.album.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Library",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                    }
                    IconButton(onClick = {
                        viewModel.libraryLayout.value =
                            if (layoutType == LayoutType.LIST) LayoutType.GRID else LayoutType.LIST
                    }) {
                        Icon(
                            imageVector = if (layoutType == LayoutType.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle Grid Layout"
                        )
                    }

                    // Sort menu configuration
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Categories selector
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
                        onClick = {
                            activeCategoryIndex = index
                            // Reset detail sheets
                            selectedAlbum = null
                            selectedArtist = null
                            selectedFolder = null
                            selectedPlaylist = null
                        },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Select Floating Options Bar
            if (isMultiSelect) {
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
                            IconButton(onClick = { showAddPlaylistDialog = true }) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add selected to Playlist")
                            }
                            IconButton(onClick = { viewModel.clearMultiSelect() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                    }
                }
            }

            // Shuffle Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (sortedSongs.isNotEmpty()) {
                            viewModel.toggleShuffle()
                            val shuffled = sortedSongs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled)
                        }
                    },
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

                Text(
                    text = "${sortedSongs.size} Tracks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // RENDER SECTION ACCORDING TO CATEGORIES
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 100.dp) // space for miniplayer
            ) {
                when (activeCategoryIndex) {
                    0 -> { // 2. SONGS
                        if (layoutType == LayoutType.LIST) {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(sortedSongs, key = { it.id }) { song ->
                                    val isSelected = selectedIds.contains(song.id)
                                    SongItemRow(
                                        song = song,
                                        isSelected = isSelected,
                                        isMultiSelectMode = isMultiSelect,
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
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 130.dp),
                                contentPadding = PaddingValues(16.dp),
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

                    1 -> { // 3. ALBUMS
                        if (selectedAlbum == null) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                                if (album.albumArtUri != null && album.albumArtUri.startsWith("http")) {
                                                    AsyncImage(
                                                        model = album.albumArtUri,
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
                        } else {
                            // Filter nested view for selected album
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
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(albumSongs) { song ->
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isMultiSelectMode = false,
                                            onSelectToggle = {},
                                            onClick = { viewModel.playSong(song, albumSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // 4. ARTISTS
                        if (selectedArtist == null) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(artistSongs) { song ->
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isMultiSelectMode = false,
                                            onSelectToggle = {},
                                            onClick = { viewModel.playSong(song, artistSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> { // 5. FOLDERS
                        if (selectedFolder == null) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(folders) { path ->
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
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(folderSongs) { song ->
                                        SongItemRow(
                                            song = song,
                                            isSelected = false,
                                            isMultiSelectMode = false,
                                            onSelectToggle = {},
                                            onClick = { viewModel.playSong(song, folderSongs) },
                                            onMoreClick = { playlistToAddTo = song }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> { // 6. PLAYLISTS
                        if (selectedPlaylist == null) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(playlists) { playlist ->
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
                                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(playlist.playlistName, fontWeight = FontWeight.Bold)
                                            }
                                            IconButton(onClick = { viewModel.deletePlaylist(playlist.playlistId) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val listId = selectedPlaylist?.playlistId ?: 0L
                            val playlistSongsState = viewModel.getSongsInPlaylist(listId).collectAsState(emptyList())
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
                                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(48.dp))
                                            Text("Playlist is empty", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(playlistSongs) { song ->
                                            SongItemRow(
                                                song = song,
                                                isSelected = false,
                                                isMultiSelectMode = false,
                                                onSelectToggle = {},
                                                onClick = { viewModel.playSong(song, playlistSongs) },
                                                onMoreClick = {
                                                    // option to delete from playlist
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
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.editSongTags(song.id, editTitle, editArtist, editAlbum)
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItemRow(
    song: SongEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (song.albumArtUri != null && song.albumArtUri.startsWith("http")) {
                    AsyncImage(
                        model = song.albumArtUri,
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

            Spacer(modifier = Modifier.width(16.dp))

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onMoreClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Track options menu")
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
                if (song.albumArtUri != null && song.albumArtUri.startsWith("http")) {
                    AsyncImage(
                        model = song.albumArtUri,
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
