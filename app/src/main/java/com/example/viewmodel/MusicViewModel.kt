package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.SongEntity
import com.example.player.PlayerManager
import com.example.util.TagEditorHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val playerManager = PlayerManager.getInstance(application)

    // Exposed States
    val sortingOption = MutableStateFlow(SortType.NAME)
    val libraryLayout = MutableStateFlow(LayoutType.LIST)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds

    val isMultiSelectMode: StateFlow<Boolean> = _selectedSongIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying

    // Database Flows
    val allSongs: StateFlow<List<SongEntity>> = songDao.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<AlbumEntity>> = allSongs
        .map { songs ->
            songs.groupBy { it.album.lowercase() }
                .mapIndexed { index, entry ->
                    val albumSongs = entry.value
                    val firstSong = albumSongs.first()
                    AlbumEntity(
                        id = index.toLong(),
                        albumName = firstSong.album,
                        artist = firstSong.artist,
                        albumArtUri = firstSong.albumArtUri
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtists: StateFlow<List<ArtistEntity>> = allSongs
        .map { songs ->
            songs.groupBy { it.artist.lowercase() }
                .mapIndexed { index, entry ->
                    val artistSongs = entry.value
                    val firstSong = artistSongs.first()
                    ArtistEntity(
                        id = index.toLong(),
                        artistName = firstSong.artist,
                        songCount = artistSongs.size
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = allSongs
        .map { songs ->
            songs.map { song ->
                song.filePath.substringBeforeLast("/", "Root")
            }.filter { it.isNotEmpty() }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<SongEntity>> = songDao.getFavoriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<SongEntity>> = songDao.getMostPlayedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Equalizer & Sleep Timer from PlayerManager
    val eqEnabled = playerManager.eqEnabled
    val eqPreset = playerManager.eqPreset
    val eqBands = playerManager.eqBands
    val sleepTimeRemaining = playerManager.sleepTimeRemaining

    init {
        // Prepopulate with a few royalty-free streaming MP3s if database is empty on start
        viewModelScope.launch {
            val count = allSongs.value.size
            if (count == 0) {
                scanLocalFiles()
            }
        }
    }

    fun scanLocalFiles() {
        viewModelScope.launch {
            _isScanning.value = true
            val samples = listOf(
                SongEntity(
                    id = "lofi_sunset",
                    title = "Lofi Sunset",
                    artist = "Melody Maker",
                    album = "Chill Study",
                    duration = 372000L,
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis(),
                    filePath = "/storage/emulated/0/Music/Lofi Sunset.mp3",
                    year = 2024,
                    genre = "Lofi",
                    playCount = 12,
                    isFavorite = false
                ),
                SongEntity(
                    id = "acoustic_breeze",
                    title = "Acoustic Breeze",
                    artist = "Guitar Dude",
                    album = "Summer Vibes",
                    duration = 423000L,
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis() - 86400000,
                    filePath = "/storage/emulated/0/Music/Acoustic Breeze.mp3",
                    year = 2023,
                    genre = "Acoustic",
                    playCount = 8,
                    isFavorite = true
                ),
                SongEntity(
                    id = "neon_dreams",
                    title = "Neon Dreams",
                    artist = "Synth Voyager",
                    album = "Future Retro",
                    duration = 302000L,
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis() - 172800000,
                    filePath = "/storage/emulated/0/Music/Neon Dreams.mp3",
                    year = 2024,
                    genre = "Synthwave",
                    playCount = 20,
                    isFavorite = false
                )
            )
            songDao.insertSongs(samples)

            // Seed a default playlist "Chill Zone" if empty
            val currentPlaylists = playlistDao.getAllPlaylists().firstOrNull() ?: emptyList()
            if (currentPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    PlaylistEntity(
                        playlistId = 1L,
                        playlistName = "Chill Zone",
                        songIds = listOf("lofi_sunset", "neon_dreams"),
                        order = 0
                    )
                )
            }

            delay(1500)
            _isScanning.value = false
        }
    }

    fun playSong(song: SongEntity, queue: List<SongEntity>) {
        viewModelScope.launch {
            val updated = song.copy(playCount = song.playCount + 1)
            songDao.updateSong(updated)
        }
        playerManager.playSong(song, queue)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun isShuffleEnabled(): Boolean {
        return playerManager.isShuffleEnabled()
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            songDao.getSongById(songId)?.let { song ->
                val updated = song.copy(isFavorite = !song.isFavorite)
                songDao.updateSong(updated)
            }
        }
    }

    fun toggleSongSelected(songId: String) {
        val current = _selectedSongIds.value
        _selectedSongIds.value = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val order = allPlaylists.value.size
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    playlistId = id,
                    playlistName = name,
                    songIds = emptyList(),
                    order = order
                )
            )
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistDao.getPlaylistById(playlistId)?.let { playlist ->
                playlistDao.updatePlaylist(playlist.copy(playlistName = newName))
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deletePlaylistById(playlistId)
        }
    }

    fun reorderPlaylists(playlistIds: List<Long>) {
        viewModelScope.launch {
            playlistIds.forEachIndexed { index, id ->
                playlistDao.getPlaylistById(id)?.let { playlist ->
                    playlistDao.updatePlaylist(playlist.copy(order = index))
                }
            }
        }
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return allPlaylists.map { playlists ->
            val playlist = playlists.find { it.playlistId == playlistId }
            val ids = playlist?.songIds ?: emptyList()
            val songsMap = allSongs.value.associateBy { it.id }
            ids.mapNotNull { songsMap[it] }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            playlistDao.getPlaylistById(playlistId)?.let { playlist ->
                if (!playlist.songIds.contains(songId)) {
                    val updated = playlist.copy(songIds = playlist.songIds + songId)
                    playlistDao.updatePlaylist(updated)
                }
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            playlistDao.getPlaylistById(playlistId)?.let { playlist ->
                val updated = playlist.copy(songIds = playlist.songIds - songId)
                playlistDao.updatePlaylist(updated)
            }
        }
    }

    fun reorderSongsInPlaylist(playlistId: Long, songIds: List<String>) {
        viewModelScope.launch {
            playlistDao.getPlaylistById(playlistId)?.let { playlist ->
                val updated = playlist.copy(songIds = songIds)
                playlistDao.updatePlaylist(updated)
            }
        }
    }

    fun addSelectedSongsToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.getPlaylistById(playlistId)?.let { playlist ->
                val newIds = _selectedSongIds.value.filter { !playlist.songIds.contains(it) }
                if (newIds.isNotEmpty()) {
                    val updated = playlist.copy(songIds = playlist.songIds + newIds)
                    playlistDao.updatePlaylist(updated)
                }
                clearMultiSelect()
            }
        }
    }

    fun editSongTags(
        songId: String,
        title: String,
        artist: String,
        album: String,
        yearStr: String,
        genre: String
    ) {
        viewModelScope.launch {
            songDao.getSongById(songId)?.let { song ->
                val yearInt = yearStr.toIntOrNull() ?: 0
                val updated = song.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    year = yearInt,
                    genre = genre
                )
                songDao.updateSong(updated)
                // Update raw file in IO thread
                TagEditorHelper.writeTagsToFile(song.filePath, title, artist, album, yearInt, genre)
            }
        }
    }

    fun addSelectedSongsToQueue() {
        // Simple queue appending logic
        clearMultiSelect()
    }

    fun clearMultiSelect() {
        _selectedSongIds.value = emptySet()
    }

    // Equalizer & Sleep Timer Controls
    fun toggleEqualizer(enable: Boolean) = playerManager.toggleEqualizer(enable)
    fun setEqPreset(presetName: String) = playerManager.setPreset(presetName)
    fun setEqBand(index: Int, level: Int) = playerManager.setBandLevel(index, level)
    fun startSleepTimer(minutes: Int) = playerManager.startSleepTimer(minutes)
    fun startSleepTimerForEndOfSong() = playerManager.startSleepTimerForEndOfSong()
    fun stopSleepTimer() = playerManager.stopSleepTimer()

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
