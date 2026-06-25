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
        val KEY_SKIP_FOLDERS = stringPreferencesKey("skip_folders")
        val KEY_FORCE_INCLUDE_FOLDERS = stringPreferencesKey("force_include_folders")
        val KEY_MIN_DURATION = intPreferencesKey("min_duration")
        val KEY_MAX_DURATION = intPreferencesKey("max_duration")
        val KEY_APP_FONT = stringPreferencesKey("app_font")
        val KEY_MINI_PLAYER_SHAPE = stringPreferencesKey("mini_player_shape")
        val KEY_MINI_PLAYER_RADIUS = intPreferencesKey("mini_player_radius")
        val KEY_BOTTOM_BAR_SHAPE = stringPreferencesKey("bottom_bar_shape")
        val KEY_BOTTOM_BAR_RADIUS = intPreferencesKey("bottom_bar_radius")
        val KEY_CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val KEY_PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
        val KEY_REPLAY_GAIN_ENABLED = booleanPreferencesKey("replay_gain_enabled")
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

    val skipFolders: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SKIP_FOLDERS] ?: ""
    }

    val forceIncludeFolders: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_FORCE_INCLUDE_FOLDERS] ?: ""
    }

    val minDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MIN_DURATION] ?: 0
    }

    val maxDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MAX_DURATION] ?: 1800
    }

    val appFont: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_FONT] ?: "Default"
    }

    val miniPlayerShape: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_MINI_PLAYER_SHAPE] ?: "Pill"
    }

    val miniPlayerRadius: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MINI_PLAYER_RADIUS] ?: 20
    }

    val bottomBarShape: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOTTOM_BAR_SHAPE] ?: "Box"
    }

    val bottomBarRadius: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOTTOM_BAR_RADIUS] ?: 0
    }

    val crossfadeDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_CROSSFADE_DURATION] ?: 0
    }

    val gaplessPlayback: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_GAPLESS_PLAYBACK] ?: true
    }

    val playbackPitch: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_PLAYBACK_PITCH] ?: 1.0f
    }

    val replayGainEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REPLAY_GAIN_ENABLED] ?: false
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

    suspend fun setSkipFolders(folders: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SKIP_FOLDERS] = folders
        }
    }

    suspend fun setForceIncludeFolders(folders: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FORCE_INCLUDE_FOLDERS] = folders
        }
    }

    suspend fun setMinDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MIN_DURATION] = seconds
        }
    }

    suspend fun setMaxDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAX_DURATION] = seconds
        }
    }

    suspend fun setAppFont(font: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_FONT] = font
        }
    }

    suspend fun setMiniPlayerShape(shape: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MINI_PLAYER_SHAPE] = shape
        }
    }

    suspend fun setMiniPlayerRadius(radius: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MINI_PLAYER_RADIUS] = radius
        }
    }

    suspend fun setBottomBarShape(shape: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BOTTOM_BAR_SHAPE] = shape
        }
    }

    suspend fun setBottomBarRadius(radius: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BOTTOM_BAR_RADIUS] = radius
        }
    }

    suspend fun setCrossfadeDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CROSSFADE_DURATION] = seconds
        }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAPLESS_PLAYBACK] = enabled
        }
    }

    suspend fun setPlaybackPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAYBACK_PITCH] = pitch
        }
    }

    suspend fun setReplayGainEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REPLAY_GAIN_ENABLED] = enabled
        }
    }
}
