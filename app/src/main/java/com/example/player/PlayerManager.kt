package com.example.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.entity.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerManager private constructor(private val context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    // Active playback states
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue

    private var currentIndex = -1

    // Equalizer
    private var equalizer: Equalizer? = null
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled

    private val _eqPreset = MutableStateFlow("Normal")
    val eqPreset: StateFlow<String> = _eqPreset

    // 5 bands gains in dB (-15 to 15)
    private val _eqBands = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val eqBands: StateFlow<List<Int>> = _eqBands

    // Sleep Timer
    private val handler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    private val _sleepTimeRemaining = MutableStateFlow<Long?>(null) // milliseconds remaining
    val sleepTimeRemaining: StateFlow<Long?> = _sleepTimeRemaining

    var pauseOnSongEnd = false
        private set

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                com.example.widget.MusicWidgetProvider.triggerUpdate(context)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || 
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    
                    if (pauseOnSongEnd && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        player.pause()
                        pauseOnSongEnd = false
                        Toast.makeText(context, "Sleep Timer: Paused at end of track", Toast.LENGTH_SHORT).show()
                    }

                    val index = player.currentMediaItemIndex
                    if (index in _queue.value.indices) {
                        currentIndex = index
                        _currentSong.value = _queue.value[index]
                    }
                    com.example.widget.MusicWidgetProvider.triggerUpdate(context)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (pauseOnSongEnd) {
                        player.pause()
                        pauseOnSongEnd = false
                        Toast.makeText(context, "Sleep Timer: Paused at end of track", Toast.LENGTH_SHORT).show()
                    }
                }
                // Initialize equalizer on audio session active
                if (playbackState == Player.STATE_READY) {
                    initEqualizer()
                }
            }
        })
    }

    private fun initEqualizer() {
        if (equalizer == null && player.audioSessionId != 0) {
            try {
                equalizer = Equalizer(0, player.audioSessionId).apply {
                    enabled = _eqEnabled.value
                    applyPresetValues()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleEqualizer(enable: Boolean) {
        _eqEnabled.value = enable
        equalizer?.enabled = enable
    }

    fun setPreset(presetName: String) {
        _eqPreset.value = presetName
        val bands = when (presetName) {
            "Bass Boost" -> listOf(12, 6, 0, 0, 0)
            "Pop" -> listOf(-2, 2, 5, 1, -2)
            "Rock" -> listOf(6, 3, -1, 4, 7)
            "Jazz" -> listOf(4, 2, -2, 2, 5)
            "Classical" -> listOf(5, 3, -1, -4, 4)
            else -> listOf(0, 0, 0, 0, 0) // Normal
        }
        _eqBands.value = bands
        applyPresetValues()
    }

    fun setBandLevel(bandIndex: Int, level: Int) {
        if (bandIndex in _eqBands.value.indices) {
            val newList = _eqBands.value.toMutableList()
            newList[bandIndex] = level.coerceIn(-15, 15)
            _eqBands.value = newList
            _eqPreset.value = "Custom"
            applyBandValue(bandIndex, level)
        }
    }

    private fun applyPresetValues() {
        val bands = _eqBands.value
        for (i in bands.indices) {
            applyBandValue(i, bands[i])
        }
    }

    private fun applyBandValue(bandIndex: Int, level: Int) {
        equalizer?.let { eq ->
            try {
                if (bandIndex < eq.numberOfBands) {
                    val minLevel = eq.bandLevelRange[0]
                    val maxLevel = eq.bandLevelRange[1]
                    // Convert gain (-15..15 dB) to Equalizer units (usually millibels)
                    val factor = (maxLevel - minLevel) / 30
                    val millibels = level * factor
                    eq.setBandLevel(bandIndex.toShort(), millibels.toShort())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSong(song: SongEntity, newQueue: List<SongEntity>) {
        _queue.value = newQueue
        currentIndex = newQueue.indexOfFirst { it.id == song.id }
        if (currentIndex == -1) {
            _queue.value = newQueue + song
            currentIndex = newQueue.size
        }

        player.clearMediaItems()
        _queue.value.forEach { qSong ->
            player.addMediaItem(MediaItem.Builder().setMediaId(qSong.id).setUri(qSong.filePath).build())
        }

        player.seekTo(currentIndex, 0)
        player.prepare()
        player.play()
        _currentSong.value = song
        initEqualizer()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun isShuffleEnabled(): Boolean {
        return player.shuffleModeEnabled
    }

    fun getCurrentPosition(): Long {
        return player.currentPosition
    }

    fun getDuration(): Long {
        return player.duration
    }

    // Sleep Timer management
    fun startSleepTimer(minutes: Int) {
        stopSleepTimer()
        pauseOnSongEnd = false

        if (minutes <= 0) return

        val durationMs = minutes * 60 * 1000L
        _sleepTimeRemaining.value = durationMs

        sleepTimerRunnable = object : Runnable {
            override fun run() {
                val remaining = _sleepTimeRemaining.value ?: 0
                if (remaining <= 1000L) {
                    player.pause()
                    _sleepTimeRemaining.value = null
                    Toast.makeText(context, "Sleep Timer: Playback Paused", Toast.LENGTH_SHORT).show()
                } else {
                    val nextRemaining = remaining - 1000L
                    _sleepTimeRemaining.value = nextRemaining
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(sleepTimerRunnable!!, 1000)
        Toast.makeText(context, "Sleep Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
    }

    fun startSleepTimerForEndOfSong() {
        stopSleepTimer()
        pauseOnSongEnd = true
        Toast.makeText(context, "Sleep Timer: Will pause at the end of current song", Toast.LENGTH_SHORT).show()
    }

    fun stopSleepTimer() {
        sleepTimerRunnable?.let { handler.removeCallbacks(it) }
        sleepTimerRunnable = null
        _sleepTimeRemaining.value = null
        pauseOnSongEnd = false
    }

    fun release() {
        stopSleepTimer()
        player.release()
        equalizer?.release()
        equalizer = null
    }

    companion object {
        @Volatile
        private var INSTANCE: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
