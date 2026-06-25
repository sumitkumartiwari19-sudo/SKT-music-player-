package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.di.ServiceLocator
import com.example.player.PlayerManager

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var playerManager: PlayerManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        playerManager = ServiceLocator.providePlayerManager(this)

        // Find or create session activity pending intent to open app when clicking notification
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Build MediaSession linked on playerManager's exoPlayer
        mediaSession = MediaSession.Builder(this, playerManager.exoPlayer)
            .setSessionActivity(openAppPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || player.playWhenReady.not() || player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
