package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converter.Converters
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.SongDao
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.SongEntity

@Database(entities = [SongEntity::class, PlaylistEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_player_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
