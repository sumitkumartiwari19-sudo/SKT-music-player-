package com.example.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.di.ServiceLocator
import com.example.player.PlayerManager
import kotlinx.coroutines.*

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "skt_music_channel"
        const val NOTIFICATION_ID = 301

        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }

    private lateinit var playerManager: PlayerManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> playerManager.togglePlayPause()
                ACTION_PREV -> playerManager.previous()
                ACTION_NEXT -> playerManager.next()
                ACTION_STOP -> stopForegroundAndService()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = ServiceLocator.providePlayerManager(this)
        createNotificationChannel()

        // Register action receiver safely
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_PREV)
            addAction(ACTION_NEXT)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }
        isReceiverRegistered = true

        // Observe player changes to keep notification updated
        serviceScope.launch {
            playerManager.currentSong.collect { _ ->
                updateNotification()
            }
        }

        serviceScope.launch {
            playerManager.isPlaying.collect { _ ->
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Direct handling if start-intent specifies any actions
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> playerManager.togglePlayPause()
                ACTION_PREV -> playerManager.previous()
                ACTION_NEXT -> playerManager.next()
                ACTION_STOP -> stopForegroundAndService()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SKT Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media controller in notification panel"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = playerManager.currentSong.value ?: return
        val isPlaying = playerManager.isPlaying.value

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, run { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 0 else 0 }, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val playPausePending = PendingIntent.getBroadcast(
            this, 1, Intent(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val prevPending = PendingIntent.getBroadcast(
            this, 2, Intent(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val nextPending = PendingIntent.getBroadcast(
            this, 3, Intent(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val stopPending = PendingIntent.getBroadcast(
            this, 4, Intent(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // standard backup icon
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPending
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                "Play/Pause",
                playPausePending
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPending
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close",
                stopPending
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundAndService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
