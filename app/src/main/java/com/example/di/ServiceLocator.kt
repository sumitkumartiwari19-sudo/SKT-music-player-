package com.example.di

import android.content.Context
import com.example.data.local.SettingsManager
import com.example.data.local.database.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.player.PlayerManager

object ServiceLocator {
    @Volatile
    private var database: MusicDatabase? = null

    @Volatile
    private var settingsManager: SettingsManager? = null

    @Volatile
    private var repository: MusicRepository? = null

    @Volatile
    private var playerManager: PlayerManager? = null

    fun provideDatabase(context: Context): MusicDatabase {
        return database ?: synchronized(this) {
            val db = MusicDatabase.getDatabase(context)
            database = db
            db
        }
    }

    fun provideSettingsManager(context: Context): SettingsManager {
        return settingsManager ?: synchronized(this) {
            val sm = SettingsManager(context.applicationContext)
            settingsManager = sm
            sm
        }
    }

    fun provideRepository(context: Context): MusicRepository {
        return repository ?: synchronized(this) {
            val db = provideDatabase(context)
            val repo = MusicRepository(
                context = context.applicationContext,
                songDao = db.songDao(),
                playlistDao = db.playlistDao(),
                playbackDao = db.playbackDao()
            )
            repository = repo
            repo
        }
    }

    fun providePlayerManager(context: Context): PlayerManager {
        return playerManager ?: synchronized(this) {
            val pm = PlayerManager(
                context = context.applicationContext,
                repository = provideRepository(context)
            )
            playerManager = pm
            pm
        }
    }
}
