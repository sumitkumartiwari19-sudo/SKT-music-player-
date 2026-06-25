package com.example.util

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

data class SongMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val genre: String
)

object TagEditorHelper {
    fun readTagsFromFile(filePath: String): SongMetadata? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return SongMetadata("", "", "", "", "")
            }
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            if (tag != null) {
                SongMetadata(
                    title = tag.getFirst(FieldKey.TITLE) ?: "",
                    artist = tag.getFirst(FieldKey.ARTIST) ?: "",
                    album = tag.getFirst(FieldKey.ALBUM) ?: "",
                    year = tag.getFirst(FieldKey.YEAR) ?: "",
                    genre = tag.getFirst(FieldKey.GENRE) ?: ""
                )
            } else {
                SongMetadata("", "", "", "", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SongMetadata("", "", "", "", "")
        }
    }

    fun writeTagsToFile(
        filePath: String,
        title: String,
        artist: String,
        album: String,
        year: Int,
        genre: String
    ): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: audioFile.createDefaultTag()
            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.YEAR, year.toString())
            tag.setField(FieldKey.GENRE, genre)
            audioFile.tag = tag
            AudioFileIO.write(audioFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
