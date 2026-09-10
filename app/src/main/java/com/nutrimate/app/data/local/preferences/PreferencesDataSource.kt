package com.nutrimate.app.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide key-value preferences (DataStore). UI prefs + daily generation quota.
 * All values are non-sensitive; secrets live in KeyStore (see `security`).
 */
@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME = stringPreferencesKey("theme") // "system" | "light" | "dark"
        val UNIT = stringPreferencesKey("unit")   // "metric" (v0.1 only)
        val GENERATION_DATE = stringPreferencesKey("generation_date")
        val GENERATION_COUNT = intPreferencesKey("generation_count")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY_BLOB = stringPreferencesKey("api_key_blob")
    }

    // --- Onboarding ---
    fun observeOnboardingDone(): Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    // --- Theme / unit (UI prefs) ---
    fun observeTheme(): Flow<String> = dataStore.data.map { it[Keys.THEME] ?: "system" }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[Keys.THEME] = theme }
    }

    fun observeUnit(): Flow<String> = dataStore.data.map { it[Keys.UNIT] ?: "metric" }

    suspend fun setUnit(unit: String) {
        dataStore.edit { it[Keys.UNIT] = unit }
    }

    // --- AI transport config (key itself stays in KeyStore) ---
    fun observeAiTransport(): Flow<Triple<String, String, String?>> =
        dataStore.data.map {
            Triple(
                it[Keys.AI_PROVIDER] ?: "",
                it[Keys.AI_MODEL] ?: "",
                it[Keys.AI_BASE_URL]
            )
        }

    suspend fun saveAiTransport(provider: String, model: String, baseUrl: String?) {
        dataStore.edit {
            it[Keys.AI_PROVIDER] = provider
            it[Keys.AI_MODEL] = model
            if (baseUrl.isNullOrBlank()) it.remove(Keys.AI_BASE_URL) else it[Keys.AI_BASE_URL] = baseUrl
        }
    }

    /** Encrypted API key blob (Base64 iv+ciphertext), non-readable without KeyStore. */
    fun observeHasApiKey(): Flow<Boolean> =
        dataStore.data.map { !it[Keys.API_KEY_BLOB].isNullOrBlank() }

    suspend fun readEncryptedApiKey(): String? =
        dataStore.data.first()[Keys.API_KEY_BLOB]

    suspend fun saveEncryptedApiKey(blob: String) {
        dataStore.edit { it[Keys.API_KEY_BLOB] = blob }
    }

    // --- Daily generation quota ---
    suspend fun usedToday(todayKey: String): Int {
        val prefs = dataStore.data.first()
        return if (prefs[Keys.GENERATION_DATE] == todayKey) prefs[Keys.GENERATION_COUNT] ?: 0
        else 0
    }

    suspend fun recordGeneration(todayKey: String, count: Int) {
        dataStore.edit {
            it[Keys.GENERATION_DATE] = todayKey
            it[Keys.GENERATION_COUNT] = count
        }
    }
}