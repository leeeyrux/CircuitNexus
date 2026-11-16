package com.circuitnexus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("circuitnexus_prefs")

/**
 * Preferencias de la app (clave de IA, vista de la mesa, temas...).
 */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val SHOW_GRID = booleanPreferencesKey("show_grid")
        val SNAP_TO_GRID = booleanPreferencesKey("snap_to_grid")
        val GRID_SIZE = intPreferencesKey("grid_size")
        val AUTO_SIMULATE = booleanPreferencesKey("auto_simulate")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val CANVAS_SKIN = stringPreferencesKey("canvas_skin")
        val FIRST_RUN = booleanPreferencesKey("first_run")
    }

    val aiApiKey: Flow<String?> =
        context.dataStore.data.map { it[Keys.AI_API_KEY] }.distinctUntilChanged()

    val aiProvider: Flow<String> =
        context.dataStore.data.map { it[Keys.AI_PROVIDER] ?: "groq" }.distinctUntilChanged()

    val aiModel: Flow<String> =
        context.dataStore.data.map { it[Keys.AI_MODEL] ?: "llama-3.3-70b-versatile" }
            .distinctUntilChanged()

    val showGrid: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SHOW_GRID] ?: true }.distinctUntilChanged()

    val snapToGrid: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SNAP_TO_GRID] ?: true }.distinctUntilChanged()

    val gridSize: Flow<Int> =
        context.dataStore.data.map { it[Keys.GRID_SIZE] ?: 20 }.distinctUntilChanged()

    val autoSimulate: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_SIMULATE] ?: true }.distinctUntilChanged()

    val hapticFeedback: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAPTIC_FEEDBACK] ?: true }.distinctUntilChanged()

    val canvasSkin: Flow<String> =
        context.dataStore.data.map { it[Keys.CANVAS_SKIN] ?: "hypernexus" }.distinctUntilChanged()

    val isFirstRun: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.FIRST_RUN] ?: true }.distinctUntilChanged()

    suspend fun setAiApiKey(key: String?) {
        context.dataStore.edit { prefs ->
            if (key.isNullOrBlank()) prefs.remove(Keys.AI_API_KEY) else prefs[Keys.AI_API_KEY] = key
        }
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { it[Keys.AI_PROVIDER] = provider }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[Keys.AI_MODEL] = model }
    }

    suspend fun setShowGrid(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GRID] = show }
    }

    suspend fun setSnapToGrid(snap: Boolean) {
        context.dataStore.edit { it[Keys.SNAP_TO_GRID] = snap }
    }

    suspend fun setGridSize(size: Int) {
        context.dataStore.edit { it[Keys.GRID_SIZE] = size }
    }

    suspend fun setAutoSimulate(auto: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SIMULATE] = auto }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setCanvasSkin(skin: String) {
        context.dataStore.edit { it[Keys.CANVAS_SKIN] = skin }
    }

    suspend fun setFirstRunComplete() {
        context.dataStore.edit { it[Keys.FIRST_RUN] = false }
    }
}
