package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: String?,
    val dateAdded: Long,
    val filePath: String,
    val year: Int = 0,
    val genre: String = "",
    val playCount: Int = 0,
    val isFavorite: Boolean = false
)

data class AlbumEntity(
    val id: Long,
    val albumName: String,
    val artist: String,
    val albumArtUri: String?
)

data class ArtistEntity(
    val id: Long,
    val artistName: String,
    val songCount: Int
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val playlistId: Long,
    val playlistName: String,
    val songIds: List<String> = emptyList(),
    val order: Int = 0
)
