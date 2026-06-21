package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "skt_music_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_EQUALIZER_PRESET = stringPreferencesKey("equalizer_preset")
        val KEY_LYRICS_ENABLED = booleanPreferencesKey("lyrics_enabled")
        val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    }

    // Default values
    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "system"
    }

    val accentColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCENT_COLOR] ?: "Default"
    }

    val equalizerPreset: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_EQUALIZER_PRESET] ?: "Normal"
    }

    val lyricsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LYRICS_ENABLED] ?: true
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_PLAYBACK_SPEED] ?: 1.0f
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setAccentColor(colorName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCENT_COLOR] = colorName
        }
    }

    suspend fun setEqualizerPreset(preset: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EQUALIZER_PRESET] = preset
        }
    }

    suspend fun setLyricsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LYRICS_ENABLED] = enabled
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAYBACK_SPEED] = speed
        }
    }
}
