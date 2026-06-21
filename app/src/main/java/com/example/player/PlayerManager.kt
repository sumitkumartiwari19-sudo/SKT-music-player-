package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.entity.SongEntity
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class PlayerManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ExoPlayer Instance
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // Manage audio focus automatically
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    // Observable states
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<SongEntity>>(emptyList())
    val currentQueue: StateFlow<List<SongEntity>> = _currentQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Screen progress updates
    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Audio Effects
    private var androidEqualizer: Equalizer? = null
    private var androidBassBoost: BassBoost? = null
    private var androidVirtualizer: Virtualizer? = null

    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    // 10 bands simulated or physically mapped
    private val _eqBands = MutableStateFlow(IntArray(10) { 0 }) // values from -15 to +15 dB
    val eqBands: StateFlow<IntArray> = _eqBands.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow(0) // 0 to 1000
    val bassBoostLevel: StateFlow<Int> = _bassBoostLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0) // 0 to 1000
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()

    // Sleep Timer
    private val _sleepTimeRemaining = MutableStateFlow(0L) // milliseconds left
    val sleepTimeRemaining: StateFlow<Long> = _sleepTimeRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Crossfade parameter (simulated via rapid volume sweeps)
    private var isCrossfading = false

    init {
        setupPlayerListener()
        startProgressUpdater()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val itemIndex = exoPlayer.currentMediaItemIndex
                if (itemIndex >= 0 && itemIndex < _currentQueue.value.size) {
                    _currentIndex.value = itemIndex
                    val song = _currentQueue.value[itemIndex]
                    _currentSong.value = song
                    _duration.value = song.duration

                    // Record standard history tracking in background
                    scope.launch {
                        repository.addToRecentlyPlayed(song.id)
                    }

                    // Re-initialize physical Equalizer effects on the new audio session
                    initializeAudioEffects()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    _playbackPosition.value = 0L
                }
            }

            override fun onRepeatModeChanged(mode: Int) {
                super.onRepeatModeChanged(mode)
                _repeatMode.value = mode
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                _isShuffleEnabled.value = shuffleModeEnabled
            }
        })
    }

    private fun startProgressUpdater() {
        scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _playbackPosition.value = exoPlayer.currentPosition
                }
                delay(400) // update progressive seek bar 2.5 times a second
            }
        }
    }

    private fun initializeAudioEffects() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                // Releases old effects
                releaseAudioEffects()

                androidEqualizer = Equalizer(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                    // Configure hardware equalization based on software presets
                    applySoftwareBandsToHardware(this)
                }

                androidBassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                    if (strengthSupported) {
                        setStrength(_bassBoostLevel.value.toShort())
                    }
                }

                androidVirtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                    if (strengthSupported) {
                        setStrength(_virtualizerLevel.value.toShort())
                    }
                }
                Log.d("PlayerManager", "Audio effects successfully bound to session: $audioSessionId")
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Failed to initialize physical audio effects", e)
        }
    }

    private fun applySoftwareBandsToHardware(eq: Equalizer) {
        try {
            val numHardwareBands = eq.numberOfBands.toInt()
            val softwareBands = _eqBands.value
            // Map our 10 bands gracefully to physical bands (usually 5 bands)
            for (i in 0 until numHardwareBands) {
                val ratio = i.toFloat() / (numHardwareBands - 1).coerceAtLeast(1)
                val softIndex = (ratio * (softwareBands.size - 1)).toInt()
                val softwareGain = softwareBands[softIndex] // -15 to +15

                // Hardware level is inside milliBel (1 dB = 100 mB)
                val minLevel = eq.bandLevelRange[0] // e.g. -1500
                val maxLevel = eq.bandLevelRange[1] // e.g. 1500
                val milliBelVal = (softwareGain * 100).toShort()
                val clampedVal = milliBelVal.coerceIn(minLevel, maxLevel)

                eq.setBandLevel(i.toShort(), clampedVal)
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Failed to set hardware band levels", e)
        }
    }

    private fun releaseAudioEffects() {
        androidEqualizer?.release()
        androidEqualizer = null
        androidBassBoost?.release()
        androidBassBoost = null
        androidVirtualizer?.release()
        androidVirtualizer = null
    }

    // Playback Interfaces
    fun playSong(song: SongEntity, queue: List<SongEntity>) {
        scope.launch {
            _currentQueue.value = queue
            val index = queue.indexOfFirst { it.id == song.id }
            _currentIndex.value = if (index != -1) index else 0

            // Fill ExoPlayer list
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            queue.forEach { qSong ->
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(qSong.filePath))
                    .setMediaId(qSong.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(qSong.title)
                            .setArtist(qSong.artist)
                            .setAlbumTitle(qSong.album)
                            .setArtworkUri(qSong.albumArtUri?.let { Uri.parse(it) })
                            .build()
                    )
                    .build()
                exoPlayer.addMediaItem(mediaItem)
            }

            if (_currentIndex.value in queue.indices) {
                exoPlayer.seekTo(_currentIndex.value, 0)
                _currentSong.value = queue[_currentIndex.value]
            }

            exoPlayer.prepare()
            exoPlayer.play()
            _isPlaying.value = true

            // Trigger audio effect bindings on ready session
            initializeAudioEffects()
        }
    }

    fun playPlaylist(songs: List<SongEntity>) {
        if (songs.isNotEmpty()) {
            playSong(songs[0], songs)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNext()
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && _currentQueue.value.isNotEmpty()) {
            exoPlayer.seekTo(0, 0)
        }
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPrevious()
        } else {
            exoPlayer.seekTo(0, 0)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode
        exoPlayer.repeatMode = mode
    }

    fun toggleShuffle() {
        val enabled = !_isShuffleEnabled.value
        _isShuffleEnabled.value = enabled
        exoPlayer.shuffleModeEnabled = enabled
    }

    // Queue Control
    fun setQueue(queue: List<SongEntity>) {
        _currentQueue.value = queue
        // Rebuild ExoPlayer cache index if needed
    }

    fun clearQueue() {
        _currentQueue.value = emptyList()
        _currentIndex.value = -1
        _currentSong.value = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    // Audio effects operations
    fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        androidEqualizer?.enabled = enabled
        androidBassBoost?.enabled = enabled
        androidVirtualizer?.enabled = enabled
    }

    fun updateEqBands(bands: IntArray) {
        _eqBands.value = bands
        androidEqualizer?.let { applySoftwareBandsToHardware(it) }
    }

    fun setBassBoost(level: Int) {
        _bassBoostLevel.value = level
        try {
            androidBassBoost?.setStrength(level.toShort())
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error updating bassboost level", e)
        }
    }

    fun setVirtualizer(level: Int) {
        _virtualizerLevel.value = level
        try {
            androidVirtualizer?.setStrength(level.toShort())
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error updating virtualizer level", e)
        }
    }

    // Sleep Timer Controls
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimeRemaining.value = 0
            return
        }

        val durationMs = minutes * 60 * 1000L
        _sleepTimeRemaining.value = durationMs

        sleepTimerJob = scope.launch {
            var timeLeft = durationMs
            while (timeLeft > 0) {
                delay(1000)
                timeLeft -= 1000
                _sleepTimeRemaining.value = timeLeft
            }
            // Trigger sleep pause!
            pauseWithFadeOut()
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = 0
    }

    private fun pauseWithFadeOut() {
        scope.launch {
            isCrossfading = true
            var volume = 1.0f
            while (volume > 0.1f) {
                volume -= 0.15f
                exoPlayer.volume = volume.coerceIn(0f, 1f)
                delay(100)
            }
            exoPlayer.pause()
            _isPlaying.value = false
            exoPlayer.volume = 1.0f // reset volume
            isCrossfading = false
            _sleepTimeRemaining.value = 0
        }
    }

    fun release() {
        scope.cancel()
        releaseAudioEffects()
        exoPlayer.release()
    }
}
