package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.dao.PlaybackDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.SongDao
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val playbackDao: PlaybackDao
) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val allAlbums: Flow<List<AlbumEntity>> = songDao.getAllAlbums()
    val allArtists: Flow<List<ArtistEntity>> = songDao.getAllArtists()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val favorites: Flow<List<SongEntity>> = playbackDao.getFavorites()
    val recentlyPlayed: Flow<List<SongEntity>> = playbackDao.getRecentlyPlayedLimit(20)
    val folderPaths: Flow<List<String>> = songDao.getUniqueFolderPaths()

    fun isFavorite(songId: String): Flow<Boolean> = playbackDao.isFavorite(songId)

    suspend fun toggleFavorite(songId: String) = withContext(Dispatchers.IO) {
        if (playbackDao.isFavoriteOneShot(songId)) {
            playbackDao.deleteFavorite(songId)
        } else {
            playbackDao.insertFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun setSongLyric(songId: String, text: String?) = withContext(Dispatchers.IO) {
        val song = songDao.getSongById(songId)
        if (song != null) {
            songDao.updateSong(song.copy(lyricText = text))
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        val currentSongs = playlistDao.getSongsInPlaylist(playlistId).first()
        val position = currentSongs.size
        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(playlistId = playlistId, songId = songId, position = position)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.deleteSongFromPlaylist(playlistId, songId)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(playlistName = name))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
        playlistDao.clearPlaylistSongs(playlistId)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.getSongsInPlaylist(playlistId)

    suspend fun addToRecentlyPlayed(songId: String) = withContext(Dispatchers.IO) {
        playbackDao.insertRecentlyPlayed(RecentlyPlayedEntity(songId = songId, playedAt = System.currentTimeMillis()))
    }

    suspend fun scanDeviceFiles(forceDemo: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val songsList = mutableListOf<SongEntity>()
            val albumsMap = mutableMapOf<String, AlbumEntity>()
            val artistsMap = mutableMapOf<String, ArtistEntity>()

            // If we are forced to load demo songs, or if we have no permissions
            if (forceDemo) {
                loadDemoSongs(songsList, albumsMap, artistsMap)
            } else {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }

                if (!hasPermission) {
                    // Fall back to demo songs so the app is immediately fully usable and looks spectacular
                    loadDemoSongs(songsList, albumsMap, artistsMap)
                } else {
                    scanMediaStore(songsList, albumsMap, artistsMap)
                    if (songsList.isEmpty()) {
                        // Sideload some beautiful default tracks if media store is completely empty
                        loadDemoSongs(songsList, albumsMap, artistsMap)
                    }
                }
            }

            // Save to Database
            if (songsList.isNotEmpty()) {
                songDao.clearAllSongs()
                songDao.clearAllAlbums()
                songDao.clearAllArtists()

                songDao.insertSongs(songsList)
                songDao.insertAlbums(albumsMap.values.toList())
                songDao.insertArtists(artistsMap.values.toList())
                Log.d("MusicRepository", "Scanned and saved ${songsList.size} songs, ${albumsMap.size} albums, ${artistsMap.size} artists.")
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to scan device files", e)
        }
    }

    private fun scanMediaStore(
        songsList: MutableList<SongEntity>,
        albumsMap: MutableMap<String, AlbumEntity>,
        artistsMap: MutableMap<String, ArtistEntity>
    ) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn).toString()
                val title = cursor.getString(titleColumn) ?: "Unknown Track"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val trackNumber = cursor.getInt(trackColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val song = SongEntity(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    filePath = filePath,
                    albumArtUri = albumArtUri,
                    dateAdded = dateAdded,
                    trackNumber = trackNumber,
                    lyricText = null
                )
                songsList.add(song)

                // Accumulate unique Albums
                val albumsKey = album.lowercase() + "_" + artist.lowercase()
                val currentAlbum = albumsMap[albumsKey]
                if (currentAlbum == null) {
                    albumsMap[albumsKey] = AlbumEntity(
                        id = albumId.toString(),
                        albumName = album,
                        artist = artist,
                        albumArtUri = albumArtUri,
                        songCount = 1
                    )
                } else {
                    albumsMap[albumsKey] = currentAlbum.copy(songCount = currentAlbum.songCount + 1)
                }

                // Accumulate unique Artists
                val artistKey = artist.lowercase()
                val currentArtist = artistsMap[artistKey]
                if (currentArtist == null) {
                    artistsMap[artistKey] = ArtistEntity(
                        id = artistKey,
                        artistName = artist,
                        albumCount = 1,
                        songCount = 1
                    )
                } else {
                    artistsMap[artistKey] = currentArtist.copy(
                        songCount = currentArtist.songCount + 1
                    )
                }
            }
        }
    }

    private fun loadDemoSongs(
        songsList: MutableList<SongEntity>,
        albumsMap: MutableMap<String, AlbumEntity>,
        artistsMap: MutableMap<String, ArtistEntity>
    ) {
        val demos = listOf(
            Triple(
                "demo_1",
                "Creative Minds",
                "https://www.bensound.com/bensound-music/bensound-creativeminds.mp3"
            ),
            Triple(
                "demo_2",
                "Acoustic Breeze",
                "https://www.bensound.com/bensound-music/bensound-acousticbreeze.mp3"
            ),
            Triple(
                "demo_3",
                "Sunny Days",
                "https://www.bensound.com/bensound-music/bensound-sunny.mp3"
            ),
            Triple(
                "demo_4",
                "Energy Booster",
                "https://www.bensound.com/bensound-music/bensound-energy.mp3"
            ),
            Triple(
                "demo_5",
                "Sweet Lullaby",
                "https://www.bensound.com/bensound-music/bensound-sweet.mp3"
            )
        )

        // Prepopulate lyric metadata
        val demoLyrics = mapOf(
            "demo_1" to "[00:05.00] This is Creative Minds\n[00:10.00] A beautiful ambient tune\n[00:15.00] Crafted perfectly with local metadata\n[00:20.00] Enjoy the high fidelity crossfades",
            "demo_2" to "[00:03.00] Feel the Acoustic Breeze\n[00:07.00] Sweeping across your beautiful device\n[00:12.00] Styled after Pixel Player interface\n[00:18.00] SKT Music Player offline",
            "demo_3" to "[00:04.00] Good morning! It's a Sunny day!\n[00:09.00] Let's listen to standard metadata lyrics\n[00:14.00] Live lyrics synching in progress\n[00:19.00] Have a wonderful time!"
        )

        demos.forEachIndexed { index, triple ->
            val (id, title, url) = triple
            val artist = "BenSound royalty-free"
            val album = "Chill Horizon"
            val duration = 180000L + index * 15300L // mock around 3 mins
            val artUri = "https://picsum.photos/seed/$id/400/400"
            val lyric = demoLyrics[id]

            songsList.add(
                SongEntity(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    filePath = url,
                    albumArtUri = artUri,
                    dateAdded = System.currentTimeMillis() - index * 86400000L,
                    trackNumber = index + 1,
                    lyricText = lyric
                )
            )

            val albumKey = album.lowercase() + "_" + artist.lowercase()
            albumsMap[albumKey] = AlbumEntity(
                id = "album_chill",
                albumName = album,
                artist = artist,
                albumArtUri = artUri,
                songCount = demos.size
            )

            val artistKey = artist.lowercase()
            artistsMap[artistKey] = ArtistEntity(
                id = artistKey,
                artistName = artist,
                albumCount = 1,
                songCount = demos.size
            )
        }
    }
}
