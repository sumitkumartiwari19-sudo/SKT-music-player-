package com.example.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsManager
import com.example.data.local.entity.*
import com.example.data.repository.MusicRepository
import com.example.player.PlayerManager
import com.example.service.MusicService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortType {
    NAME, DATE_ADDED, DURATION, ARTIST, ALBUM
}

enum class LayoutType {
    LIST, GRID
}

class MusicViewModel(
    private val context: Context,
    private val repository: MusicRepository,
    private val settingsManager: SettingsManager,
    private val playerManager: PlayerManager
) : ViewModel() {

    // Filtering & Layout state
    val searchQuery = MutableStateFlow("")
    val sortingOption = MutableStateFlow(SortType.NAME)
    val libraryLayout = MutableStateFlow(LayoutType.LIST)

    // User favorites, items, playlists
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<AlbumEntity>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtists: StateFlow<List<ArtistEntity>> = repository.allArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<SongEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<SongEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = repository.folderPaths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Preferences from DataStore
    val themeMode: StateFlow<String> = settingsManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val accentColor: StateFlow<String> = settingsManager.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")

    val lyricsEnabled: StateFlow<Boolean> = settingsManager.lyricsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentPreset: StateFlow<String> = settingsManager.equalizerPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Normal")

    // Equalizer State linked with PlayerManager
    val eqEnabled = playerManager.eqEnabled
    val eqBands = playerManager.eqBands
    val bassBoost = playerManager.bassBoostLevel
    val virtualizer = playerManager.virtualizerLevel

    // Current player states
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val playbackPosition = playerManager.playbackPosition
    val duration = playerManager.duration
    val repeatMode = playerManager.repeatMode
    val isShuffleEnabled = playerManager.isShuffleEnabled
    val playbackSpeed = playerManager.playbackSpeed
    val playQueue = playerManager.currentQueue
    val sleepTimerRemaining = playerManager.sleepTimeRemaining

    // Multi-select mode states
    val selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val isMultiSelectMode = MutableStateFlow(false)

    init {
        // Run deep Media Store scan
        scanLocalFiles()

        // Sync DataStore playback configurations down to the player init config
        viewModelScope.launch {
            val speed = settingsManager.playbackSpeed.first()
            playerManager.setPlaybackSpeed(speed)

            val preset = settingsManager.equalizerPreset.first()
            applyEqualizerPreset(preset)
        }
    }

    fun scanLocalFiles() {
        viewModelScope.launch {
            repository.scanDeviceFiles(forceDemo = false)
        }
    }

    fun forceDemoSongs() {
        viewModelScope.launch {
            repository.scanDeviceFiles(forceDemo = true)
        }
    }

    // Playback proxies
    fun playSong(song: SongEntity, queue: List<SongEntity>) {
        playerManager.playSong(song, queue)
        startPlaybackService()
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
        if (playerManager.isPlaying.value) {
            startPlaybackService()
        }
    }

    fun next() {
        playerManager.next()
        startPlaybackService()
    }

    fun previous() {
        playerManager.previous()
        startPlaybackService()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun setRepeatMode(mode: Int) {
        playerManager.setRepeatMode(mode)
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playerManager.setPlaybackSpeed(speed)
            settingsManager.setPlaybackSpeed(speed)
        }
    }

    private fun startPlaybackService() {
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY_PAUSE
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // Favorites & Playlists operations
    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return repository.getSongsInPlaylist(playlistId)
    }

    // Settings adjustments
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch {
            settingsManager.setAccentColor(color)
        }
    }

    fun setLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setLyricsEnabled(enabled)
        }
    }

    // Equalizer controls & Custom profiles
    fun setEqEnabled(enabled: Boolean) {
        playerManager.setEqEnabled(enabled)
    }

    fun updateEqBands(bands: IntArray) {
        playerManager.updateEqBands(bands)
        viewModelScope.launch {
            settingsManager.setEqualizerPreset("Custom")
        }
    }

    fun setBassBoost(level: Int) {
        playerManager.setBassBoost(level)
    }

    fun setVirtualizer(level: Int) {
        playerManager.setVirtualizer(level)
    }

    fun applyEqualizerPreset(preset: String) {
        viewModelScope.launch {
            settingsManager.setEqualizerPreset(preset)
            val bands = when (preset) {
                "Normal" -> intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                "Pop" -> intArrayOf(2, 3, 4, 3, -1, -2, -1, 1, 3, 4)
                "Rock" -> intArrayOf(5, 4, -3, -5, -2, 1, 4, 6, 7, 7)
                "Classical" -> intArrayOf(4, 3, 2, 2, -1, -1, -2, -3, -3, -4)
                "Jazz" -> intArrayOf(3, 2, 1, 2, -2, -1, 0, 1, 2, 3)
                "Heavy Metal" -> intArrayOf(5, 4, 3, 0, -1, 2, 3, 0, 5, 5)
                "Dance" -> intArrayOf(4, 6, 5, 0, -1, -3, -4, 0, 3, 4)
                else -> return@launch // Custom keeps original bands
            }
            playerManager.updateEqBands(bands)
        }
    }

    // Sleep Timer controls
    fun startSleepTimer(minutes: Int) {
        playerManager.startSleepTimer(minutes)
    }

    fun stopSleepTimer() {
        playerManager.stopSleepTimer()
    }

    // Tag Editor
    fun editSongTags(songId: String, newTitle: String, newArtist: String, newAlbum: String) {
        viewModelScope.launch {
            val songs = allSongs.value
            val targetSong = songs.find { it.id == songId }
            if (targetSong != null) {
                val updated = targetSong.copy(
                    title = newTitle,
                    artist = newArtist,
                    album = newAlbum
                )
                // We mock writing metadata to file by saving it in our local Room DB
                // This is fully functional and edits the state live!
                com.example.di.ServiceLocator.provideDatabase(context).songDao().updateSong(updated)
            }
        }
    }

    // Embedded Lyrics Updates
    fun updateLyrics(songId: String, lyrics: String?) {
        viewModelScope.launch {
            repository.setSongLyric(songId, lyrics)
        }
    }

    // Multi-select management
    fun toggleSongSelected(songId: String) {
        val current = selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        selectedSongIds.value = current
        if (current.isEmpty()) {
            isMultiSelectMode.value = false
        } else {
            isMultiSelectMode.value = true
        }
    }

    fun clearMultiSelect() {
        selectedSongIds.value = emptySet()
        isMultiSelectMode.value = false
    }

    fun addSelectedSongsToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            selectedSongIds.value.forEach { songId ->
                repository.addSongToPlaylist(playlistId, songId)
            }
            clearMultiSelect()
        }
    }

    fun addSelectedSongsToQueue() {
        val songsToAdd = allSongs.value.filter { selectedSongIds.value.contains(it.id) }
        val updatedQueue = playQueue.value.toMutableList()
        updatedQueue.addAll(songsToAdd)
        playerManager.setQueue(updatedQueue)
        clearMultiSelect()
    }

    // Backup & Restore settings simulation
    fun backupSettings(): String {
        // Serialize settings metadata to a nice string that can be restored!
        val favStr = favorites.value.joinToString(",") { it.id }
        val eqPreset = currentPreset.value
        val bassLevel = bassBoost.value
        val virtLevel = virtualizer.value
        return "FAVORITES:$favStr|PRESET:$eqPreset|BASS:$bassLevel|VIRT:$virtLevel"
    }

    fun restoreSettings(backupStr: String) {
        try {
            if (backupStr.isEmpty()) return
            val parts = backupStr.split("|")
            parts.forEach { part ->
                val eqIndex = part.indexOf(":")
                if (eqIndex != -1) {
                    val key = part.substring(0, eqIndex)
                    val value = part.substring(eqIndex + 1)
                    when (key) {
                        "FAVORITES" -> {
                            val idList = value.split(",").filter { it.isNotEmpty() }
                            viewModelScope.launch {
                                idList.forEach { id ->
                                    if (!repository.favorites.first().any { it.id == id }) {
                                        repository.toggleFavorite(id)
                                    }
                                }
                            }
                        }
                        "PRESET" -> {
                            applyEqualizerPreset(value)
                        }
                        "BASS" -> {
                            val level = value.toIntOrNull() ?: 0
                            setBassBoost(level)
                        }
                        "VIRT" -> {
                            val level = value.toIntOrNull() ?: 0
                            setVirtualizer(level)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // failed to restore
        }
    }

    // Factory Provider
    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = com.example.di.ServiceLocator.provideDatabase(context)
                val repo = com.example.di.ServiceLocator.provideRepository(context)
                val sm = com.example.di.ServiceLocator.provideSettingsManager(context)
                val pm = com.example.di.ServiceLocator.providePlayerManager(context)
                return MusicViewModel(context.applicationContext, repo, sm, pm) as T
            }
        }
    }
}
