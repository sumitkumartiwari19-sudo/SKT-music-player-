package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.data.local.entity.SongEntity
import com.example.player.PlayerManager

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val playerManager = PlayerManager.getInstance(context)
        val currentSong = playerManager.currentSong.value
        val isPlaying = playerManager.isPlaying.value

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentSong, isPlaying)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val playerManager = PlayerManager.getInstance(context)

        when (intent.action) {
            ACTION_PREV -> {
                playerManager.skipToPrevious()
                triggerUpdate(context)
            }
            ACTION_PLAY -> {
                playerManager.togglePlayPause()
                triggerUpdate(context)
            }
            ACTION_NEXT -> {
                playerManager.skipToNext()
                triggerUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"
        const val ACTION_PLAY = "com.example.widget.ACTION_PLAY"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            song: SongEntity?,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, com.example.R.layout.music_widget)

            if (song != null) {
                views.setTextViewText(com.example.R.id.widget_song_title, song.title)
                views.setTextViewText(com.example.R.id.widget_song_artist, song.artist)
            } else {
                views.setTextViewText(com.example.R.id.widget_song_title, "Melody Player")
                views.setTextViewText(com.example.R.id.widget_song_artist, "No Song Playing")
            }

            val playIcon = if (isPlaying) {
                android.media.session.PlaybackState.STATE_PAUSED // pause icon equivalent
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }
            views.setImageViewResource(com.example.R.id.widget_btn_play, playIcon)

            // Setup PendingIntents for controls
            views.setOnClickPendingIntent(com.example.R.id.widget_btn_prev, getPendingSelfIntent(context, ACTION_PREV))
            views.setOnClickPendingIntent(com.example.R.id.widget_btn_play, getPendingSelfIntent(context, ACTION_PLAY))
            views.setOnClickPendingIntent(com.example.R.id.widget_btn_next, getPendingSelfIntent(context, ACTION_NEXT))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                this.action = action
            }
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, 0, intent, flags)
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MusicWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            
            val playerManager = PlayerManager.getInstance(context)
            val currentSong = playerManager.currentSong.value
            val isPlaying = playerManager.isPlaying.value

            for (widgetId in allWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId, currentSong, isPlaying)
            }
        }
    }
}
