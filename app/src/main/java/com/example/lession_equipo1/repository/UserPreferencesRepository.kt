package com.example.lession_equipo1.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extensión para instanciar DataStore de preferencias
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_ENGLISH = booleanPreferencesKey("is_english")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val EMG_REFERENCE_REST_MV = floatPreferencesKey("emg_reference_rest_mv")
        val EMG_REFERENCE_MAX_MV = floatPreferencesKey("emg_reference_max_mv")
    }

    val emgReferenceRestMvFlow: Flow<Float?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[PreferencesKeys.EMG_REFERENCE_REST_MV] }

    val emgReferenceMaxMvFlow: Flow<Float?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[PreferencesKeys.EMG_REFERENCE_MAX_MV] }

    // Flujo para obtener el idioma (false = Español por defecto, true = Inglés)
    val isEnglishFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_ENGLISH] ?: false
        }

    // Flujo para obtener el tema (true = Modo Oscuro por defecto, false = Modo Claro)
    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: true
        }

    // Guardar preferencia de idioma
    suspend fun setEnglish(isEnglish: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENGLISH] = isEnglish
        }
    }

    // Guardar preferencia de tema
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun setEmgCalibration(restMv: Float, maximumMv: Float) {
        require(restMv.isFinite() && restMv >= 0f)
        require(maximumMv.isFinite() && maximumMv > restMv)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMG_REFERENCE_REST_MV] = restMv
            preferences[PreferencesKeys.EMG_REFERENCE_MAX_MV] = maximumMv
        }
    }
}
